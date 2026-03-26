package com.vaccine.core.service;

import com.vaccine.domain.Booking;
import com.vaccine.domain.BookingStatus;
import com.vaccine.domain.DriveSubscription;
import com.vaccine.domain.NotificationType;
import com.vaccine.domain.Slot;
import com.vaccine.domain.Status;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.DriveSubscriptionRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import com.vaccine.util.DriveStatusResolver;
import com.vaccine.util.SlotStatusResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotAutomationService {
    private final SlotRepository slotRepository;
    private final VaccinationDriveRepository driveRepository;
    private final DriveSubscriptionRepository driveSubscriptionRepository;
    private final BookingRepository bookingRepository;
    private final INotificationService notificationService;

    @Scheduled(fixedDelayString = "${app.slots.automation-delay-ms:60000}", initialDelayString = "${app.slots.automation-initial-delay-ms:30000}")
    @Transactional
    public void automateSlots() {
        syncDriveStatuses();
        notifyTimeSensitiveSlotEvents();
    }

    private void syncDriveStatuses() {
        List<VaccinationDrive> drives = driveRepository.findAll();
        for (VaccinationDrive drive : drives) {
            String realtimeStatus = DriveStatusResolver.resolve(drive);
            Status nextStatus = Status.valueOf(realtimeStatus);
            if (drive.getStatus() != nextStatus) {
                drive.setStatus(nextStatus);
            }
        }
        driveRepository.saveAll(drives);
    }

    private void notifyTimeSensitiveSlotEvents() {
        LocalDateTime now = LocalDateTime.now();
        for (Slot slot : slotRepository.findAll()) {
            LocalDateTime start = slot.getStartDateTime();
            LocalDateTime end = SlotStatusResolver.resolveEnd(slot);
            if (start == null || end == null) {
                continue;
            }

            long minutesUntilStart = Duration.between(now, start).toMinutes();
            if (minutesUntilStart >= 0 && minutesUntilStart <= 15) {
                notifySlotAudience(slot, NotificationType.SLOT_STARTING_SOON,
                    "Slot starting soon",
                    "Slot #" + slot.getId() + " starts at " + start + ".");
            }

            if (!now.isBefore(start) && !now.isAfter(end)) {
                notifySlotAudience(slot, NotificationType.SLOT_LIVE,
                    "Slot is now live",
                    "Slot #" + slot.getId() + " is now live for booking.");
            }
        }
    }

    private void notifySlotAudience(Slot slot, NotificationType type, String title, String message) {
        Set<Long> recipientIds = new HashSet<>();
        if (slot.getDrive() != null && slot.getDrive().getId() != null) {
            for (DriveSubscription subscription : driveSubscriptionRepository.findByDriveId(slot.getDrive().getId())) {
                if (subscription.getUser() != null && subscription.getUser().getId() != null && recipientIds.add(subscription.getUser().getId())) {
                    notificationService.queueNotification(
                        subscription.getUser(),
                        type,
                        title,
                        message,
                        LocalDateTime.now(),
                        slot.getId(),
                        type.name() + ":" + slot.getId() + ":" + subscription.getUser().getId()
                    );
                }
            }
        }

        for (Booking booking : bookingRepository.findBySlotId(slot.getId())) {
            if (booking.getUser() == null || booking.getUser().getId() == null) {
                continue;
            }
            if (booking.getStatus() == BookingStatus.CANCELLED || !recipientIds.add(booking.getUser().getId())) {
                continue;
            }
            notificationService.queueNotification(
                booking.getUser(),
                type,
                title,
                message,
                LocalDateTime.now(),
                slot.getId(),
                type.name() + ":" + slot.getId() + ":" + booking.getUser().getId()
            );
        }
    }
}
