package com.vaccine.core.service;

import com.vaccine.common.dto.BookingRequest;
import com.vaccine.domain.*;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BookingService {
    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final INotificationService notificationService;

    public BookingService(BookingRepository bookingRepository, SlotRepository slotRepository,
                          UserRepository userRepository, INotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Booking book(String email, BookingRequest req) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException("User not found"));
        Slot slot = slotRepository.findById(req.slotId()).orElseThrow(() -> new AppException("Slot not found"));

        VaccinationDrive drive = slot.getDrive();
        if (user.getAge() < drive.getMinAge() || user.getAge() > drive.getMaxAge()) {
            throw new AppException("Age not eligible for this drive");
        }

        boolean hasConflict = bookingRepository.existsByUserIdAndSlotStartTimeBetweenAndStatusIn(
            user.getId(), slot.getStartTime().minusHours(1), slot.getEndTime().plusHours(1),
            List.of(BookingStatus.PENDING, BookingStatus.APPROVED));
        if (hasConflict) {
            throw new AppException("You already have a nearby slot booking");
        }

        if (slot.getBookedCount() >= slot.getCapacity()) {
            throw new AppException("Slot is full");
        }

        slot.setBookedCount(slot.getBookedCount() + 1);
        slotRepository.save(slot);

        Booking booking = Booking.builder()
            .user(user)
            .slot(slot)
            .status(BookingStatus.PENDING)
            .build();

        Booking saved = bookingRepository.save(booking);
        notificationService.sendEmail(user, "Booking Confirmation", 
            "Your booking has been created with status PENDING. Booking ID: " + saved.getId());
        notificationService.sendSms(user, "Booking request submitted. ID=" + saved.getId());
        return saved;
    }

    public Booking cancel(String email, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found"));
        if (!booking.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AppException("You can only cancel your own booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException("Already cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        Slot slot = booking.getSlot();
        slot.setBookedCount(Math.max(0, slot.getBookedCount() - 1));
        slotRepository.save(slot);
        notificationService.sendEmail(booking.getUser(), "Booking Cancelled", 
            "Your booking has been cancelled. Booking ID: " + booking.getId());
        return bookingRepository.save(booking);
    }

    // Additional methods...
}
