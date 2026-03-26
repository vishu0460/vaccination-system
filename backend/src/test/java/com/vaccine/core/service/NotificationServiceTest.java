package com.vaccine.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.vaccine.domain.Booking;
import com.vaccine.domain.BookingStatus;
import com.vaccine.domain.Notification;
import com.vaccine.domain.NotificationDeliveryStatus;
import com.vaccine.domain.NotificationType;
import com.vaccine.domain.Slot;
import com.vaccine.domain.User;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.NotificationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private NotificationRealtimeService notificationRealtimeService;
    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationRepository,
            bookingRepository,
            notificationRealtimeService,
            mailSenderProvider
        );
        ReflectionTestUtils.setField(notificationService, "maxRetries", 3);
        ReflectionTestUtils.setField(notificationService, "dispatchBatchSize", 100);
        ReflectionTestUtils.setField(notificationService, "reconcileWindowDays", 45);

        lenient().when(notificationRepository.findByDedupeKey(anyString())).thenReturn(Optional.empty());
        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) {
                notification.setId(Math.abs(notification.getDedupeKey().hashCode()) + 1L);
            }
            return notification;
        });
    }

    @Test
    void queueBookingConfirmedNotification_shouldCreateImmediateAndReminderNotifications() {
        Booking booking = buildBooking(BookingStatus.CONFIRMED, LocalDateTime.now().plusDays(3));
        List<Notification> savedNotifications = new ArrayList<>();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) {
                notification.setId((long) (savedNotifications.size() + 1));
            }
            savedNotifications.add(notification);
            return notification;
        });

        notificationService.queueBookingConfirmedNotification(booking);

        assertTrue(savedNotifications.stream().anyMatch(notification -> notification.getType() == NotificationType.BOOKING_CONFIRMED));
        assertTrue(savedNotifications.stream().anyMatch(notification -> notification.getType() == NotificationType.APPOINTMENT_REMINDER_48H));
        assertTrue(savedNotifications.stream().anyMatch(notification -> notification.getType() == NotificationType.APPOINTMENT_REMINDER_24H));
    }

    @Test
    void queueVaccinationCompletedNotification_shouldCreateSecondDoseReminders() {
        Booking booking = buildBooking(BookingStatus.COMPLETED, LocalDateTime.now().minusDays(1));
        booking.setSecondDoseRequired(true);
        booking.setNextDoseDueDate(LocalDateTime.now().plusDays(10));

        List<Notification> savedNotifications = new ArrayList<>();
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) {
                notification.setId((long) (savedNotifications.size() + 1));
            }
            savedNotifications.add(notification);
            return notification;
        });

        notificationService.queueVaccinationCompletedNotification(booking);

        assertTrue(savedNotifications.stream().anyMatch(notification -> notification.getType() == NotificationType.VACCINATION_COMPLETED));
        assertTrue(savedNotifications.stream().anyMatch(notification -> notification.getType() == NotificationType.SECOND_DOSE_REMINDER_48H));
        assertTrue(savedNotifications.stream().anyMatch(notification -> notification.getType() == NotificationType.SECOND_DOSE_REMINDER_24H));
    }

    @Test
    void dispatchDueNotifications_shouldMarkPendingNotificationsAsSent() {
        Notification notification = Notification.builder()
            .id(99L)
            .user(buildUser())
            .title("Reminder")
            .message("Test message")
            .type(NotificationType.APPOINTMENT_REMINDER_24H)
            .deliveryStatus(NotificationDeliveryStatus.PENDING)
            .scheduledTime(LocalDateTime.now().minusMinutes(5))
            .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
            .dedupeKey("APPOINTMENT_REMINDER_24H:99:24h")
            .build();

        when(notificationRepository.findDueForDispatch(anyList(), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(notification));

        notificationService.dispatchDueNotifications();

        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveryStatus());
        assertTrue(notification.getSentAt() != null);
    }

    private Booking buildBooking(BookingStatus status, LocalDateTime assignedTime) {
        VaccinationCenter center = VaccinationCenter.builder()
            .id(7L)
            .name("Central Health Hub")
            .build();

        VaccinationDrive drive = VaccinationDrive.builder()
            .id(4L)
            .title("City Immunization Drive")
            .vaccineType("VaxShield")
            .driveDate(LocalDate.now().plusDays(assignedTime.isAfter(LocalDateTime.now()) ? 3 : 0))
            .minAge(18)
            .maxAge(60)
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(17, 0))
            .totalSlots(100)
            .center(center)
            .build();

        Slot slot = Slot.builder()
            .id(3L)
            .drive(drive)
            .dateTime(assignedTime)
            .capacity(20)
            .bookedCount(5)
            .startTime(assignedTime.toLocalTime())
            .endTime(assignedTime.toLocalTime().plusMinutes(30))
            .build();

        return Booking.builder()
            .id(10L)
            .user(buildUser())
            .slot(slot)
            .status(status)
            .assignedTime(assignedTime)
            .bookedAt(LocalDateTime.now().minusDays(1))
            .build();
    }

    private User buildUser() {
        return User.builder()
            .id(11L)
            .email("user@example.com")
            .fullName("Test User")
            .phoneNumber("+1234567890")
            .age(29)
            .build();
    }
}
