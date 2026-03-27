package com.vaccine.core.service;

import com.vaccine.common.dto.BookingRequest;
import com.vaccine.common.dto.BookingResponse;
import com.vaccine.domain.*;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import com.vaccine.util.DriveStatusResolver;
import com.vaccine.util.SlotStatusResolver;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class BookingService {
    private static final List<Status> BOOKABLE_DRIVE_STATUSES = List.of(Status.LIVE, Status.UPCOMING);

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final VaccinationDriveRepository driveRepository;
    private final INotificationService notificationService;
    private final AuditService auditService;
    private final WaitlistService waitlistService;

    public BookingService(BookingRepository bookingRepository, SlotRepository slotRepository,
                          UserRepository userRepository, VaccinationDriveRepository driveRepository,
                          INotificationService notificationService, AuditService auditService,
                          WaitlistService waitlistService) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.driveRepository = driveRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.waitlistService = waitlistService;
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public Booking book(String email, BookingRequest req) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException("User not found"));
        log.info("Creating booking for userId={}, slotId={}, driveId={}", user.getId(), req.slotId(), req.driveId());
        if (req.userId() != null && !req.userId().equals(user.getId())) {
            throw new AppException("Booking request user does not match the logged-in user");
        }
        Slot slot = slotRepository.findById(req.slotId()).orElseThrow(() -> new AppException("Slot not found"));
        VaccinationDrive requestedDrive = req.driveId() == null
            ? null
            : driveRepository.findById(req.driveId()).orElseThrow(() -> new AppException("Drive not found"));

        VaccinationDrive drive = slot.getDrive();
        if (drive == null) {
            throw new AppException("Selected slot is not linked to a drive");
        }
        if (requestedDrive != null && !requestedDrive.getId().equals(drive.getId())) {
            throw new AppException("Selected slot does not belong to the requested drive");
        }
        if (!BOOKABLE_DRIVE_STATUSES.contains(drive.getStatus())) {
            throw new AppException("Booking is only allowed for drives marked LIVE or UPCOMING");
        }
        if ("EXPIRED".equals(DriveStatusResolver.resolve(drive))) {
            throw new AppException("Cannot book a slot for an expired drive");
        }
        if (SlotStatusResolver.resolve(slot) == SlotStatus.EXPIRED) {
            throw new AppException("Cannot book an expired slot");
        }
        Integer userAge = user.getAge();
        if (userAge == null) {
            throw new AppException("Please complete your profile date of birth before booking");
        }
        if (userAge < drive.getMinAge() || userAge > drive.getMaxAge()) {
            throw new AppException("Age not eligible for this drive");
        }

        LocalDateTime slotDateTime = slot.getStartDateTime();
        boolean hasConflict = slotDateTime != null && bookingRepository.existsByUserIdAndSlotDateTimeBetweenAndStatusIn(
            user.getId(),
            slotDateTime.minusHours(2),
            slotDateTime.plusHours(2),
            Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );
        if (hasConflict) {
            throw new AppException("You already have a nearby slot booking");
        }

        int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
        if (bookedCount >= capacity) {
            throw new AppException("Slot is full. You can join the waitlist.");
        }

        boolean alreadyBooked = bookingRepository.existsByUserIdAndSlotIdAndStatusIn(
            user.getId(),
            slot.getId(),
            Arrays.asList(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );
        if (alreadyBooked) {
            throw new AppException("You have already booked this slot");
        }

        LocalDateTime assignedTime = calculateAssignedTime(slot, bookedCount);

        Booking booking = Booking.builder()
            .user(user)
            .slot(slot)
            .adminId(resolveBookingAdminId(slot))
            .status(BookingStatus.PENDING)
            .assignedTime(assignedTime)
            .notes(req.notes())
            .build();

        slot.setBookedCount(bookedCount + 1);
        slotRepository.save(slot);

        Booking saved = bookingRepository.save(booking);
        auditService.logActionAs(user.getEmail(), "CREATE_BOOKING", "BOOKING", saved.getId(), "Booking created for slot " + slot.getId(), null);
        log.info("Booking created successfully for userId={}, bookingId={}, status={}, assignedTime={}", user.getId(), saved.getId(), saved.getStatus(), saved.getAssignedTime());
        return saved;
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public Booking cancel(String email, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found"));
        log.info("Cancelling bookingId={} for userId={}", bookingId, booking.getUser().getId());
        if (!booking.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AppException("You can only cancel your own booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException("Already cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        Slot slot = booking.getSlot();
        if (slot != null) {
            int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
            slot.setBookedCount(Math.max(0, bookedCount - 1));
            slotRepository.save(slot);
            waitlistService.promoteNextUserIfAvailable(slot);
        }
        notificationService.sendEmail(booking.getUser(), "Booking Cancelled", 
            buildCancellationMessage(booking));
        notificationService.sendSms(booking.getUser(), buildShortCancellationMessage(booking));
        log.info("Booking cancelled successfully for bookingId={}", bookingId);
        Booking savedBooking = bookingRepository.save(booking);
        auditService.logActionAs(booking.getUser().getEmail(), "CANCEL_BOOKING", "BOOKING", savedBooking.getId(), "Booking cancelled by user", null);
        return savedBooking;
    }

    public List<BookingResponse> getMyBookings(String email) {
        return bookingRepository.findByUserEmailOrderByBookedAtDesc(email).stream()
            .map(BookingResponse::from)
            .collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public Booking reschedule(String email, Long bookingId, BookingRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found"));
        log.info("Rescheduling bookingId={} for userId={} to slotId={}", bookingId, booking.getUser().getId(), req.slotId());
        if (!booking.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AppException("You can only reschedule your own booking");
        }
        Slot newSlot = slotRepository.findById(req.slotId()).orElseThrow(() -> new AppException("Slot not found"));
        if (req.userId() != null && !req.userId().equals(booking.getUser().getId())) {
            throw new AppException("Booking request user does not match the logged-in user");
        }
        if (req.driveId() != null && (newSlot.getDrive() == null || !req.driveId().equals(newSlot.getDrive().getId()))) {
            throw new AppException("Selected slot does not belong to the requested drive");
        }
        if (newSlot.getDrive() == null || !BOOKABLE_DRIVE_STATUSES.contains(newSlot.getDrive().getStatus())) {
            throw new AppException("Selected slot is not available for booking");
        }
        if (newSlot.getDrive() != null && "EXPIRED".equals(DriveStatusResolver.resolve(newSlot.getDrive()))) {
            throw new AppException("Cannot reschedule to a slot for an expired drive");
        }
        if (SlotStatusResolver.resolve(newSlot) == SlotStatus.EXPIRED) {
            throw new AppException("Cannot reschedule to an expired slot");
        }
        int newSlotBookedCount = newSlot.getBookedCount() == null ? 0 : newSlot.getBookedCount();
        int newSlotCapacity = newSlot.getCapacity() == null ? 0 : newSlot.getCapacity();
        if (newSlotBookedCount >= newSlotCapacity) {
            throw new AppException("Selected slot is full");
        }
        Slot previousSlot = booking.getSlot();
        if (previousSlot != null && !previousSlot.getId().equals(newSlot.getId())) {
            int previousBookedCount = previousSlot.getBookedCount() == null ? 0 : previousSlot.getBookedCount();
            previousSlot.setBookedCount(Math.max(0, previousBookedCount - 1));
            slotRepository.save(previousSlot);
            LocalDateTime assignedTime = calculateAssignedTime(newSlot, newSlotBookedCount);
            newSlot.setBookedCount(newSlotBookedCount + 1);
            slotRepository.save(newSlot);
            booking.setAssignedTime(assignedTime);
        }
        booking.setSlot(newSlot);
        booking.setAdminId(resolveBookingAdminId(newSlot));
        booking.setNotes(req.notes());
        log.info("Booking rescheduled successfully for bookingId={} to slotId={}", bookingId, newSlot.getId());
        Booking savedBooking = bookingRepository.save(booking);
        auditService.logActionAs(booking.getUser().getEmail(), "RESCHEDULE_BOOKING", "BOOKING", savedBooking.getId(), "Booking moved to slot " + newSlot.getId(), null);
        return savedBooking;
    }

    public List<Slot> recommendSlots(String email, String city, int limit) {
        // Simple recommendation logic
        User user = userRepository.findByEmail(email).orElseThrow();
        Integer userAge = user.getAge();
        if (userAge == null) {
            return List.of();
        }
        return slotRepository.findAll().stream()
            .filter(slot -> {
                int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
                int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
                return capacity > bookedCount;
            })
            .filter(slot -> SlotStatusResolver.resolve(slot) != SlotStatus.EXPIRED)
            .filter(slot -> slot.getDrive() != null && BOOKABLE_DRIVE_STATUSES.contains(slot.getDrive().getStatus()))
            .filter(s -> userAge >= s.getDrive().getMinAge() && userAge <= s.getDrive().getMaxAge())
            .limit(limit)
            .collect(Collectors.toList());
    }

    private String buildCancellationMessage(Booking booking) {
        String centerName = booking.getSlot() != null && booking.getSlot().getDrive() != null && booking.getSlot().getDrive().getCenter() != null
            ? booking.getSlot().getDrive().getCenter().getName()
            : "your selected center";
        String slotTime = booking.getAssignedTime() != null
            ? booking.getAssignedTime().toString()
            : "the selected time";
        return "Your vaccination booking at " + centerName + " for " + slotTime + " has been cancelled.";
    }

    private String buildShortCancellationMessage(Booking booking) {
        String centerName = booking.getSlot() != null && booking.getSlot().getDrive() != null && booking.getSlot().getDrive().getCenter() != null
            ? booking.getSlot().getDrive().getCenter().getName()
            : "the selected center";
        return "Booking cancelled for " + centerName + ". Booking ID: " + booking.getId();
    }

    private LocalDateTime calculateAssignedTime(Slot slot, int alreadyBookedCount) {
        LocalDateTime slotStart = slot.getStartDateTime();
        if (slotStart == null) {
            throw new AppException("Slot start time is not configured");
        }

        LocalDateTime slotEnd = SlotStatusResolver.resolveEnd(slot);
        if (slotEnd == null) {
            throw new AppException("Slot end time is not configured");
        }

        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
        if (capacity < 1) {
            throw new AppException("Slot capacity must be at least 1");
        }

        if (!slotEnd.isAfter(slotStart)) {
            throw new AppException("Slot end time must be after slot start time");
        }

        Duration duration = Duration.between(slotStart, slotEnd);
        long intervalMinutes = duration.toMinutes() / capacity;
        if (intervalMinutes > 0) {
            return slotStart.plusMinutes(intervalMinutes * alreadyBookedCount);
        }

        long intervalSeconds = Math.max(1, duration.getSeconds() / capacity);
        return slotStart.plusSeconds(intervalSeconds * alreadyBookedCount);
    }

    private Long resolveBookingAdminId(Slot slot) {
        if (slot == null) {
            return null;
        }
        if (slot.getAdminId() != null) {
            return slot.getAdminId();
        }
        if (slot.getDrive() != null) {
            return slot.getDrive().getAdminId();
        }
        return null;
    }
}
