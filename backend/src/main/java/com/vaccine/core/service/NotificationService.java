package com.vaccine.core.service;

import com.vaccine.common.dto.NotificationResponse;
import com.vaccine.domain.Booking;
import com.vaccine.domain.BookingStatus;
import com.vaccine.domain.Notification;
import com.vaccine.domain.NotificationDeliveryStatus;
import com.vaccine.domain.NotificationReadStatus;
import com.vaccine.domain.NotificationType;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.NotificationRepository;
import com.vaccine.common.exception.AppException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements INotificationService {
    private static final DateTimeFormatter NOTIFICATION_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' hh:mm a");

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRealtimeService notificationRealtimeService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:noreply@vaccination-system.com}")
    private String mailFrom;

    @Value("${app.notifications.max-retries:5}")
    private int maxRetries;

    @Value("${app.notifications.dispatch-batch-size:100}")
    private int dispatchBatchSize;

    @Value("${app.notifications.reconcile-window-days:45}")
    private int reconcileWindowDays;

    @Override
    @Transactional
    public void queueBookingConfirmedNotification(Booking booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }

        queueNotification(
            booking.getUser(),
            NotificationType.BOOKING_CONFIRMED,
            "Booking confirmed",
            buildBookingConfirmedMessage(booking),
            LocalDateTime.now(),
            booking.getId(),
            buildDedupeKey(NotificationType.BOOKING_CONFIRMED, booking.getId(), "immediate")
        );
        ensureBookingReminderNotifications(booking);
    }

    @Override
    @Transactional
    public void queueVaccinationCompletedNotification(Booking booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }

        queueNotification(
            booking.getUser(),
            NotificationType.VACCINATION_COMPLETED,
            "Vaccination completed",
            buildVaccinationCompletedMessage(booking),
            LocalDateTime.now(),
            booking.getId(),
            buildDedupeKey(NotificationType.VACCINATION_COMPLETED, booking.getId(), "immediate")
        );
        ensureSecondDoseReminderNotifications(booking);
    }

    @Override
    @Transactional
    public void ensureBookingReminderNotifications(Booking booking) {
        if (booking == null || booking.getId() == null || booking.getAssignedTime() == null) {
            return;
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return;
        }

        queueReminderNotification(
            booking,
            NotificationType.APPOINTMENT_REMINDER_48H,
            "Vaccination reminder",
            booking.getAssignedTime().minusHours(48),
            buildAppointmentReminderMessage(booking, "48 hours"),
            "48h"
        );
        queueReminderNotification(
            booking,
            NotificationType.APPOINTMENT_REMINDER_24H,
            "Vaccination reminder",
            booking.getAssignedTime().minusHours(24),
            buildAppointmentReminderMessage(booking, "24 hours"),
            "24h"
        );
    }

    @Override
    @Transactional
    public void ensureSecondDoseReminderNotifications(Booking booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        if (!Boolean.TRUE.equals(booking.getSecondDoseRequired()) || booking.getNextDoseDueDate() == null) {
            return;
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            return;
        }

        queueSecondDoseReminder(
            booking,
            NotificationType.SECOND_DOSE_REMINDER_48H,
            booking.getNextDoseDueDate().minusHours(48),
            "48h"
        );
        queueSecondDoseReminder(
            booking,
            NotificationType.SECOND_DOSE_REMINDER_24H,
            booking.getNextDoseDueDate().minusHours(24),
            "24h"
        );
    }

    @Override
    @Transactional
    public Notification queueNotification(User user, NotificationType type, String title, String message,
                                          LocalDateTime scheduledTime, Long referenceId, String dedupeKey) {
        return queueNotification(user, type, title, message, scheduledTime, referenceId, dedupeKey, null);
    }

    @Override
    @Transactional
    public Notification queueReplyNotification(User user, NotificationType type, String title, String sourceMessage,
                                               String replyMessage, Long referenceId, String dedupeKey) {
        return queueNotification(user, type, title, sourceMessage, LocalDateTime.now(), referenceId, dedupeKey, replyMessage);
    }

    @Override
    @Transactional
    public void dispatchDueNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> dueNotifications = notificationRepository.findDueForDispatch(
            List.of(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.FAILED),
            now,
            PageRequest.of(0, Math.max(1, dispatchBatchSize))
        );

        dueNotifications.forEach(this::attemptDelivery);
    }

    @Override
    @Transactional
    public void reconcileScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusDays(Math.max(1, reconcileWindowDays));

        bookingRepository.findScheduledBookingsForWindow(BookingStatus.CONFIRMED, now.minusHours(1), windowEnd)
            .forEach(this::ensureBookingReminderNotifications);
        bookingRepository.findSecondDoseBookingsForWindow(BookingStatus.COMPLETED, now.minusHours(1), windowEnd)
            .forEach(this::ensureSecondDoseReminderNotifications);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(String email) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(email).stream()
            .map(this::toNotificationResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        return notificationRepository.countByUserEmailAndIsReadFalse(email);
    }

    @Override
    @Transactional
    public void markNotificationsRead(String email) {
        List<Notification> unreadNotifications = notificationRepository.findByUserEmailAndIsReadFalseOrderByCreatedAtDesc(email);
        unreadNotifications.forEach(this::markAsRead);
        notificationRepository.saveAll(unreadNotifications);
        notificationRealtimeService.pushUnreadCount(email, 0);
    }

    @Override
    @Transactional
    public void markNotificationRead(String email, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserEmail(notificationId, email)
            .orElseThrow(() -> new AppException("Notification not found"));
        markAsRead(notification);
        notificationRepository.save(notification);
        notificationRealtimeService.pushUnreadCount(email, getUnreadCount(email));
    }

    @Override
    public void sendEmail(User user, String subject, String message) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        try {
            JavaMailSender javaMailSender = mailSenderProvider.getIfAvailable();
            if (javaMailSender == null) {
                log.info("Email delivery skipped because JavaMailSender is not configured");
                return;
            }
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject(subject);
            mail.setText(message);
            mail.setFrom(mailFrom);
            javaMailSender.send(mail);
            log.debug("Email delivered successfully for userId={} subject={}", user.getId(), subject);
        } catch (Exception ex) {
            log.warn("Email delivery skipped for userId={} subject={} reason={}", user.getId(), subject, ex.getMessage());
        }
    }

    @Override
    public void sendSms(User user, String message) {
        if (user == null || user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            return;
        }
        log.debug("SMS delivery prepared for userId={}", user.getId());
    }

    private Notification queueNotification(User user, NotificationType type, String title, String message,
                                           LocalDateTime scheduledTime, Long referenceId, String dedupeKey,
                                           String replyMessage) {
        if (user == null || dedupeKey == null || dedupeKey.isBlank()) {
            return null;
        }

        Notification existing = notificationRepository.findByDedupeKey(dedupeKey).orElse(null);
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveSchedule = scheduledTime == null ? now : scheduledTime;

        Notification notification = Notification.builder()
            .user(user)
            .title(title)
            .message(message)
            .type(type)
            .replyMessage(replyMessage)
            .status(NotificationReadStatus.UNREAD)
            .deliveryStatus(NotificationDeliveryStatus.PENDING)
            .referenceId(referenceId)
            .isRead(false)
            .scheduledTime(effectiveSchedule)
            .nextAttemptAt(effectiveSchedule.isBefore(now) ? now : effectiveSchedule)
            .retryCount(0)
            .dedupeKey(dedupeKey)
            .build();

        Notification saved = notificationRepository.save(notification);
        if (!saved.getNextAttemptAt().isAfter(now)) {
            attemptDelivery(saved);
        }
        return saved;
    }

    private void queueReminderNotification(Booking booking, NotificationType type, String title,
                                           LocalDateTime scheduledTime, String message, String suffix) {
        if (booking.getAssignedTime() == null || booking.getAssignedTime().isBefore(LocalDateTime.now())) {
            return;
        }

        LocalDateTime effectiveSchedule = scheduledTime.isAfter(LocalDateTime.now()) ? scheduledTime : LocalDateTime.now();
        queueNotification(
            booking.getUser(),
            type,
            title,
            message,
            effectiveSchedule,
            booking.getId(),
            buildDedupeKey(type, booking.getId(), suffix)
        );
    }

    private void queueSecondDoseReminder(Booking booking, NotificationType type, LocalDateTime scheduledTime, String suffix) {
        if (booking.getNextDoseDueDate() == null || booking.getNextDoseDueDate().isBefore(LocalDateTime.now())) {
            return;
        }

        LocalDateTime effectiveSchedule = scheduledTime.isAfter(LocalDateTime.now()) ? scheduledTime : LocalDateTime.now();
        queueNotification(
            booking.getUser(),
            type,
            "Second dose reminder",
            buildSecondDoseReminderMessage(booking, suffix.equals("48h") ? "48 hours" : "24 hours"),
            effectiveSchedule,
            booking.getId(),
            buildDedupeKey(type, booking.getId(), suffix)
        );
    }

    private void attemptDelivery(Notification notification) {
        LocalDateTime now = LocalDateTime.now();
        try {
            if (notification.getUser() == null || notification.getUser().getId() == null) {
                throw new AppException("Notification has no target user");
            }

            notification.setLastAttemptAt(now);
            notification.setDeliveryStatus(NotificationDeliveryStatus.SENT);
            notification.setSentAt(now);
            notification.setNextAttemptAt(now);
            notification.setLastError(null);
            notificationRepository.save(notification);
            long unreadCount = notification.getUser() != null && notification.getUser().getEmail() != null
                ? notificationRepository.countByUserEmailAndIsReadFalse(notification.getUser().getEmail())
                : 0L;
            notificationRealtimeService.pushNotification(
                notification.getUser() != null ? notification.getUser().getEmail() : null,
                toNotificationResponse(notification),
                unreadCount
            );
            log.info("Notification {} dispatched successfully with type={}", notification.getId(), notification.getType());
        } catch (Exception exception) {
            int retries = (notification.getRetryCount() == null ? 0 : notification.getRetryCount()) + 1;
            notification.setRetryCount(retries);
            notification.setLastAttemptAt(now);
            notification.setLastError(truncateError(exception.getMessage()));
            notification.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
            notification.setNextAttemptAt(
                retries >= maxRetries
                    ? now.plusYears(100)
                    : now.plusMinutes(Math.min(60, retries * 5L))
            );
            notificationRepository.save(notification);
            log.error("Notification {} failed on attempt {}", notification.getId(), retries, exception);
            if (retries >= maxRetries) {
                log.error("Notification {} exceeded retry budget", notification.getId());
            }
        }
    }

    private void markAsRead(Notification notification) {
        notification.setIsRead(true);
        notification.setStatus(NotificationReadStatus.READ);
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getTitle(),
            notification.getType() != null ? notification.getType().name() : null,
            notification.getTitle(),
            notification.getMessage(),
            notification.getReplyMessage(),
            notification.getStatus() != null ? notification.getStatus().name() : null,
            notification.getDeliveryStatus() != null ? notification.getDeliveryStatus().name() : null,
            notification.getScheduledTime(),
            notification.getCreatedAt(),
            Boolean.TRUE.equals(notification.getIsRead())
        );
    }

    private String buildBookingConfirmedMessage(Booking booking) {
        return "Your vaccination booking is confirmed for "
            + formatAppointmentTime(booking)
            + " at "
            + resolveCenterName(booking)
            + ".";
    }

    private String buildVaccinationCompletedMessage(Booking booking) {
        StringBuilder builder = new StringBuilder("Your vaccination was marked as completed for ")
            .append(formatAppointmentTime(booking))
            .append(" at ")
            .append(resolveCenterName(booking))
            .append(".");

        if (Boolean.TRUE.equals(booking.getSecondDoseRequired()) && booking.getNextDoseDueDate() != null) {
            builder.append(" Your next dose is due on ")
                .append(booking.getNextDoseDueDate().format(NOTIFICATION_TIME_FORMAT))
                .append(".");
        }

        return builder.toString();
    }

    private String buildAppointmentReminderMessage(Booking booking, String leadTime) {
        return "Reminder: your vaccination appointment is in " + leadTime
            + ". Please arrive at "
            + resolveCenterName(booking)
            + " on "
            + formatAppointmentTime(booking)
            + ".";
    }

    private String buildSecondDoseReminderMessage(Booking booking, String leadTime) {
        return "Reminder: your second dose is due in " + leadTime
            + ". Your next dose date is "
            + booking.getNextDoseDueDate().format(NOTIFICATION_TIME_FORMAT)
            + ".";
    }

    private String formatAppointmentTime(Booking booking) {
        LocalDateTime appointment = booking.getAssignedTime() != null
            ? booking.getAssignedTime()
            : booking.getSlot() != null ? booking.getSlot().getDateTime() : null;
        return appointment != null ? appointment.format(NOTIFICATION_TIME_FORMAT) : "your scheduled appointment time";
    }

    private String resolveCenterName(Booking booking) {
        if (booking.getSlot() != null && booking.getSlot().getDrive() != null && booking.getSlot().getDrive().getCenter() != null) {
            return booking.getSlot().getDrive().getCenter().getName();
        }
        return "your vaccination center";
    }

    private String buildDedupeKey(NotificationType type, Long referenceId, String suffix) {
        return type.name() + ":" + referenceId + ":" + suffix;
    }

    private String truncateError(String message) {
        if (message == null || message.isBlank()) {
            return "Notification dispatch failed";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
