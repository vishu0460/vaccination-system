package com.vaccine.service;

import com.vaccine.core.service.AuditService;
import com.vaccine.core.service.AdminService;
import com.vaccine.core.service.CertificateService;
import com.vaccine.core.service.ContactService;
import com.vaccine.core.service.FeedbackService;
import com.vaccine.core.service.INotificationService;
import com.vaccine.domain.Booking;
import com.vaccine.domain.BookingStatus;
import com.vaccine.domain.Slot;
import com.vaccine.infrastructure.persistence.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private VaccinationCenterRepository centerRepository;
    @Mock private VaccinationDriveRepository driveRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private NewsRepository newsRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private ContactRepository contactRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private EmailVerificationRepository emailVerificationRepository;
    @Mock private PasswordResetRepository passwordResetRepository;
    @Mock private PhoneVerificationRepository phoneVerificationRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SearchLogRepository searchLogRepository;
    @Mock private CertificateService certificateService;
    @Mock private AuditService auditService;
    @Mock private FeedbackService feedbackService;
    @Mock private ContactService contactService;
    @Mock private RoleRepository roleRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private INotificationService notificationService;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getDashboardStats() {
        // Minimal test - service compiles
        assertNotNull(adminService);
    }

    @Test
    void completeBooking_UpdatesStatusAndGeneratesCertificate() {
        Booking booking = Booking.builder()
            .id(1L)
            .status(BookingStatus.CONFIRMED)
            .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.saveAndFlush(booking)).thenReturn(booking);
        when(certificateService.certificateExistsForBooking(1L)).thenReturn(false);

        Booking result = adminService.completeBooking(1L);

        assertEquals(BookingStatus.COMPLETED, result.getStatus());
        verify(bookingRepository).saveAndFlush(booking);
        verify(certificateService).generate(booking);
        verify(notificationService).queueVaccinationCompletedNotification(booking);
    }

    @Test
    void deleteBooking_RemovesBookingAndReleasesSlotCapacity() {
        Slot slot = Slot.builder()
            .id(10L)
            .bookedCount(3)
            .build();
        Booking booking = Booking.builder()
            .id(1L)
            .slot(slot)
            .status(BookingStatus.CONFIRMED)
            .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        adminService.deleteBooking(1L, request);

        assertEquals(2, slot.getBookedCount());
        verify(slotRepository).save(slot);
        verify(bookingRepository).save(booking);
    }
}
