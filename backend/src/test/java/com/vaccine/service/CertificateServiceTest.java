package com.vaccine.service;

import com.vaccine.domain.*;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.CertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.vaccine.core.service.CertificateService;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private CertificateService certificateService;

    private Booking testBooking;
    private User testUser;
    private Slot testSlot;
    private VaccinationDrive testDrive;
    private VaccinationCenter testCenter;

    @BeforeEach
    void setUp() {
        testCenter = new VaccinationCenter();
        testCenter.setId(1L);
        testCenter.setName("Test Center");
        testCenter.setCity("Test City");

        testDrive = new VaccinationDrive();
        testDrive.setId(1L);
        testDrive.setTitle("COVID-19 Drive");
        testDrive.setMinAge(18);
        testDrive.setMaxAge(60);
        testDrive.setCenter(testCenter);

        testSlot = new Slot();
        testSlot.setId(1L);
        testSlot.setDrive(testDrive);
        testSlot.setStartTime(LocalDateTime.now().plusDays(1).toLocalTime());
        testSlot.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1).toLocalTime());
        testSlot.setCapacity(10);
        testSlot.setBookedCount(0);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setAge(25);

        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setUser(testUser);
        testBooking.setSlot(testSlot);
        testBooking.setStatus(BookingStatus.COMPLETED);
    }

    // ==================== POSITIVE TESTS ====================

    @Test
    void generateCertificate_Success_WithCompletedBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(certificateRepository.existsByBookingId(1L)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            return cert;
        });

        Certificate result = certificateService.generateCertificate(1L, "Covishield", 1);

        assertNotNull(result);
        assertNotNull(result.getCertificateNumber());
        assertTrue(result.getCertificateNumber().startsWith("VAX-"));
        assertEquals("Covishield", result.getVaccineName());
        assertEquals(1, result.getDoseNumber());
        assertNotNull(result.getQrCode());
        assertNotNull(result.getDigitalVerificationCode());
    }

    @Test
    void generateCertificate_Success_WithApprovedBooking() {
        testBooking.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(certificateRepository.existsByBookingId(1L)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            return cert;
        });

        Certificate result = certificateService.generateCertificate(1L, "Covaxin", 2);

        assertNotNull(result);
        assertEquals("Covaxin", result.getVaccineName());
        assertEquals(2, result.getDoseNumber());
    }

@Test
    void generateCertificate_FirstDose_HasNextDoseDate() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(certificateRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            cert.setNextDoseDate(LocalDate.now().plusDays(84).atStartOfDay());
            return cert;
        });

        Certificate result = certificateService.generateCertificate(1L, "Covishield", 1);

        assertNotNull(result.getNextDoseDate());
        assertEquals(84, java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), result.getNextDoseDate().toLocalDate()));
    }

    @Test
    void generateCertificate_SecondDose_HasNoNextDoseDate() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(certificateRepository.existsByBookingId(1L)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            return cert;
        });

        Certificate result = certificateService.generateCertificate(1L, "Covishield", 2);

        assertNull(result.getNextDoseDate());
    }

    @Test
    void getCertificateById_Success() {
        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setCertificateNumber("VAX-TEST-123");
        when(certificateRepository.findById(1L)).thenReturn(Optional.of(certificate));

        Certificate result = certificateService.getCertificateById(1L);

        assertEquals("VAX-TEST-123", result.getCertificateNumber());
    }

@Test
    void getCertificateByBookingId_Success() {
        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setBooking(testBooking);
        certificate.setCertificateNumber("VAX-TEST-123");
        when(certificateRepository.findByBooking_Id(1L)).thenReturn(Optional.of(certificate));

        Certificate result = certificateService.getCertificateByBookingId(1L);

        assertEquals("VAX-TEST-123", result.getCertificateNumber());
    }


    @Test
    void getCertificateByNumber_Success() {
        Certificate certificate = new Certificate();
        certificate.setId(1L);
        certificate.setCertificateNumber("VAX-20241201-ABC12345");
        when(certificateRepository.findByCertificateNumber("VAX-20241201-ABC12345")).thenReturn(Optional.of(certificate));

        Certificate result = certificateService.getCertificateByNumber("VAX-20241201-ABC12345");

        assertEquals("VAX-20241201-ABC12345", result.getCertificateNumber());
    }

    @Test
    void getUserCertificates_Success() {
        Certificate certificate1 = new Certificate();
        certificate1.setId(1L);
        certificate1.setCertificateNumber("VAX-001");
        Certificate certificate2 = new Certificate();
        certificate2.setId(2L);
        certificate2.setCertificateNumber("VAX-002");
        when(certificateRepository.findByBookingUserIdOrderByIssuedAtDesc(1L)).thenReturn(java.util.List.of(certificate1, certificate2));

        var result = certificateService.getUserCertificates(1L);

        assertEquals(2, result.size());
    }

    // ==================== NEGATIVE TESTS ====================

    @Test
    void generateCertificate_BookingNotFound_ThrowsException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            certificateService.generateCertificate(999L, "Covishield", 1));
    }

    @Test
    void generateCertificate_BookingNotApproved_ThrowsException() {
        testBooking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThrows(RuntimeException.class, () -> 
            certificateService.generateCertificate(1L, "Covishield", 1));
    }

    @Test
    void generateCertificate_BookingCancelled_ThrowsException() {
        testBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThrows(RuntimeException.class, () -> 
            certificateService.generateCertificate(1L, "Covishield", 1));
    }

    @Test
    void generateCertificate_CertificateAlreadyExists_ThrowsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
when(certificateRepository.findByBookingId(1L)).thenReturn(Optional.of(new Certificate()));

        assertThrows(AppException.class, () -> 
            certificateService.generateCertificate(1L, "Covishield", 1));
    }


    @Test
    void getCertificateById_NotFound_ThrowsException() {
        when(certificateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            certificateService.getCertificateById(999L));
    }

@Test
    void getCertificateByBookingId_NotFound_ThrowsException() {
        when(certificateRepository.findByBooking_Id(999L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> 
            certificateService.getCertificateByBookingId(999L));
    }

    @Test
    void getCertificateByNumber_NotFound_ThrowsException() {
        when(certificateRepository.findByCertificateNumber("INVALID")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            certificateService.getCertificateByNumber("INVALID"));
    }

    // ==================== BOUNDARY TESTS ====================

    @Test
    void generateCertificate_DoseNumberZero_Valid() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(certificateRepository.existsByBookingId(1L)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            return cert;
        });

        Certificate result = certificateService.generateCertificate(1L, "Covishield", 0);

        assertNotNull(result);
        assertEquals(0, result.getDoseNumber());
    }

    @Test
    void generateCertificate_NegativeDoseNumber_Valid() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(certificateRepository.existsByBookingId(1L)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            return cert;
        });

        Certificate result = certificateService.generateCertificate(1L, "Covishield", -1);

        assertNotNull(result);
        assertEquals(-1, result.getDoseNumber());
    }

    @Test
    void generateCertificate_NullVaccineName_Valid() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(certificateRepository.existsByBookingId(1L)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            return cert;
        });

        Certificate result = certificateService.generateCertificate(1L, null, 1);

        assertNotNull(result);
        assertNull(result.getVaccineName());
    }

    // ==================== EDGE CASES ====================

    @Test
    void generateCertificate_CertificateNumberFormat_Valid() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(certificateRepository.existsByBookingId(1L)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            return cert;
        });

        Certificate result = certificateService.generateCertificate(1L, "Covishield", 1);

        // Format: VAX-YYYYMMDD-XXXXXXXX
        String certNumber = result.getCertificateNumber();
        assertTrue(certNumber.matches("VAX-\\d{8}-[A-Z0-9]{8}"));
    }

    @Test
    void generateCertificate_DigitalVerificationCodeLength_Valid() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(certificateRepository.existsByBookingId(1L)).thenReturn(false);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate cert = invocation.getArgument(0);
            cert.setId(1L);
            return cert;
        });

        Certificate result = certificateService.generateCertificate(1L, "Covishield", 1);

        assertEquals(32, result.getDigitalVerificationCode().length());
    }
}

