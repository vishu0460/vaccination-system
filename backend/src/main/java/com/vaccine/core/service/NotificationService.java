package com.vaccine.core.service;

import com.vaccine.domain.Booking;
import com.vaccine.domain.Notification;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.NotificationRepository;
import com.vaccine.core.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements INotificationService {
    private static final DateTimeFormatter NOTIFICATION_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' hh:mm a");
    private final NotificationRepository notificationRepository;
    
    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
    
    @Override
    public void sendReminderNotification(Booking booking) {
        log.info("Sending reminder for booking: {}", booking.getId());
        User user = booking.getUser();
        String message = buildMeaningfulBookingMessage(booking, "Reminder");
        persistInAppNotification(user, "BOOKING_REMINDER", "Vaccination Reminder", message, booking.getId());
        sendEmail(user, "Vaccination reminder", message);
        sendSms(user, message);
    }
    
    @Override
    public void sendBookingNotification(Booking booking) {
        log.info("Sending booking notification for booking: {}", booking.getId());
        User user = booking.getUser();
        String message = buildMeaningfulBookingMessage(booking, "Confirmation");
        persistInAppNotification(user, "BOOKING_UPDATE", "Booking request received", message, booking.getId());
        sendEmail(user, "Booking request received", message);
        sendSms(user, message);
    }
    
    public void sendEmail(User user, String subject, String message) {
        log.info("Email to {}: {} - {}", user.getEmail(), subject, message);
    }
    
    public void sendSms(User user, String message) {
        log.info("SMS to {}: {}", user.getPhoneNumber(), message);
    }

    private String buildMeaningfulBookingMessage(Booking booking, String prefix) {
        String fullName = booking.getUser() != null && booking.getUser().getFullName() != null
            ? booking.getUser().getFullName()
            : "User";
        String centerName = booking.getSlot() != null && booking.getSlot().getDrive() != null && booking.getSlot().getDrive().getCenter() != null
            ? booking.getSlot().getDrive().getCenter().getName()
            : "your vaccination center";
        String driveTitle = booking.getSlot() != null && booking.getSlot().getDrive() != null && booking.getSlot().getDrive().getTitle() != null
            ? booking.getSlot().getDrive().getTitle()
            : "your vaccination drive";
        String appointmentTime = booking.getAssignedTime() != null
            ? booking.getAssignedTime().format(NOTIFICATION_TIME_FORMAT)
            : booking.getSlot() != null && booking.getSlot().getDateTime() != null
                ? booking.getSlot().getDateTime().format(NOTIFICATION_TIME_FORMAT)
            : "the scheduled time";
        return prefix + " for " + fullName + ": your slot at " + centerName + " for " + driveTitle + " is scheduled on " + appointmentTime + ". Booking ID: " + booking.getId() + ".";
    }

    private void persistInAppNotification(User user, String type, String title, String message, Long referenceId) {
        if (user == null) {
            return;
        }

        Notification notification = Notification.builder()
            .user(user)
            .title(title)
            .type(type)
            .message(message)
            .status("UNREAD")
            .referenceId(referenceId)
            .isRead(false)
            .build();
        notificationRepository.save(notification);
    }
}
