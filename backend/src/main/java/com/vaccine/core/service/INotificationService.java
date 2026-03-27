package com.vaccine.core.service;

import com.vaccine.common.dto.NotificationResponse;
import com.vaccine.domain.Booking;
import com.vaccine.domain.Notification;
import com.vaccine.domain.NotificationType;
import com.vaccine.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface INotificationService {
    void queueBookingConfirmedNotification(Booking booking);

    void queueVaccinationCompletedNotification(Booking booking);

    void ensureBookingReminderNotifications(Booking booking);

    void ensureSecondDoseReminderNotifications(Booking booking);

    void queueBirthdayNotification(User user, LocalDate birthdayDate);

    Notification queueNotification(User user, NotificationType type, String title, String message,
                                   LocalDateTime scheduledTime, Long referenceId, String dedupeKey);

    Notification queueReplyNotification(User user, NotificationType type, String title, String sourceMessage,
                                        String replyMessage, Long referenceId, String dedupeKey);

    void dispatchDueNotifications();

    void reconcileScheduledNotifications();

    List<Notification> getAllNotifications();

    List<NotificationResponse> getNotificationsForUser(String email);

    long getUnreadCount(String email);

    void markNotificationsRead(String email);

    void markNotificationRead(String email, Long notificationId);

    void sendEmail(User user, String subject, String message);

    void sendSms(User user, String message);
}
