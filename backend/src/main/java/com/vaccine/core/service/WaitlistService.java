package com.vaccine.core.service;

import com.vaccine.common.dto.WaitlistEntryResponse;
import com.vaccine.common.exception.AppException;
import com.vaccine.domain.Booking;
import com.vaccine.domain.BookingStatus;
import com.vaccine.domain.NotificationType;
import com.vaccine.domain.Slot;
import com.vaccine.domain.SlotStatus;
import com.vaccine.domain.User;
import com.vaccine.domain.WaitlistEntry;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.WaitlistEntryRepository;
import com.vaccine.util.SlotStatusResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WaitlistService {
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final UserRepository userRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final INotificationService notificationService;

    public WaitlistEntryResponse joinWaitlist(String email, Long slotId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException("User not found"));
        Slot slot = slotRepository.findById(slotId).orElseThrow(() -> new AppException("Slot not found"));

        if (SlotStatusResolver.resolve(slot) == SlotStatus.EXPIRED) {
            throw new AppException("Expired slots cannot accept waitlist entries");
        }
        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
        int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
        if (bookedCount < capacity) {
            throw new AppException("This slot still has available capacity");
        }
        if (bookingRepository.existsByUserIdAndSlotIdAndStatusIn(user.getId(), slotId, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))) {
            throw new AppException("You already have a booking for this slot");
        }
        if (waitlistEntryRepository.existsBySlotIdAndUserIdAndStatus(slotId, user.getId(), "WAITING")) {
            throw new AppException("You are already on the waitlist for this slot");
        }

        WaitlistEntry entry = waitlistEntryRepository.save(WaitlistEntry.builder()
            .slot(slot)
            .user(user)
            .status("WAITING")
            .build());
        return WaitlistEntryResponse.from(entry);
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> getUserWaitlist(String email) {
        return waitlistEntryRepository.findByUserEmailOrderByCreatedAtDesc(email).stream()
            .map(WaitlistEntryResponse::from)
            .toList();
    }

    public Booking promoteNextUserIfAvailable(Slot slot) {
        if (slot == null || slot.getId() == null) {
            return null;
        }
        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
        int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
        if (bookedCount >= capacity) {
            return null;
        }

        WaitlistEntry entry = waitlistEntryRepository.findFirstBySlotIdAndStatusOrderByCreatedAtAsc(slot.getId(), "WAITING")
            .orElse(null);
        if (entry == null || entry.getUser() == null) {
            return null;
        }

        Booking booking = Booking.builder()
            .user(entry.getUser())
            .slot(slot)
            .adminId(slot.getAdminId() != null ? slot.getAdminId() : (slot.getDrive() != null ? slot.getDrive().getAdminId() : null))
            .status(BookingStatus.CONFIRMED)
            .assignedTime(slot.getStartDateTime())
            .notes("Auto-promoted from waitlist")
            .build();
        Booking savedBooking = bookingRepository.save(booking);

        slot.setBookedCount(bookedCount + 1);
        slotRepository.save(slot);

        entry.setStatus("PROMOTED");
        entry.setPromotedAt(LocalDateTime.now());
        waitlistEntryRepository.save(entry);

        notificationService.queueNotification(
            entry.getUser(),
            NotificationType.WAITLIST_PROMOTED,
            "Waitlist promotion",
            "A seat opened up and your booking has been auto-confirmed for slot #" + slot.getId() + ".",
            LocalDateTime.now(),
            savedBooking.getId(),
            NotificationType.WAITLIST_PROMOTED.name() + ":" + savedBooking.getId()
        );
        notificationService.queueBookingConfirmedNotification(savedBooking);
        notificationService.sendEmail(entry.getUser(), "Waitlist Promotion Confirmed",
            "Your waitlist request has been promoted and booking #" + savedBooking.getId() + " is now confirmed.");
        return savedBooking;
    }
}
