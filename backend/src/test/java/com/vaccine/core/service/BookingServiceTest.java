package com.vaccine.core.service;

import com.vaccine.common.dto.BookingRequest;
import com.vaccine.common.exception.AppException;
import com.vaccine.core.service.INotificationService;
import com.vaccine.domain.*;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
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

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VaccinationDriveRepository driveRepository;

    @Mock
    private INotificationService notificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private WaitlistService waitlistService;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Slot testSlot;
    private VaccinationDrive testDrive;
    private VaccinationCenter testCenter;

    @BeforeEach
    void setUp() {
        testCenter = VaccinationCenter.builder()
            .id(1L)
            .name("Test Center")
            .city("Test City")
            .build();

        testDrive = VaccinationDrive.builder()
            .id(1L)
            .title("COVID-19 Drive")
            .minAge(18)
            .maxAge(60)
            .status(Status.LIVE)
            .active(true)
            .driveDate(LocalDate.now().plusDays(1))
            .startTime(java.time.LocalTime.of(9, 0))
            .endTime(java.time.LocalTime.of(17, 0))
            .center(testCenter)
            .build();

        LocalDateTime slotDateTime = LocalDateTime.now().plusDays(1);
        testSlot = Slot.builder()
            .id(1L)
            .drive(testDrive)
            .dateTime(slotDateTime)
            .capacity(10)
            .bookedCount(0)
            .startTime(slotDateTime.toLocalTime())
            .endTime(slotDateTime.plusHours(1).toLocalTime())
            .build();

        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .fullName("Test User")
            .age(25)
            .build();
    }

    @Test
    void book_Success() {
        BookingRequest request = new BookingRequest(1L, 1L, 1L, null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
        when(driveRepository.findById(1L)).thenReturn(Optional.of(testDrive));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        Booking result = bookingService.book("test@example.com", request);

        assertNotNull(result);
        assertEquals(BookingStatus.PENDING, result.getStatus());
        verify(slotRepository).save(testSlot);
    }

    @Test
    void book_AgeNotEligible() {
        testUser.setAge(15);
        BookingRequest request = new BookingRequest(1L, 1L, 1L, null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
        when(driveRepository.findById(1L)).thenReturn(Optional.of(testDrive));

        assertThrows(AppException.class, () -> 
            bookingService.book("test@example.com", request));
    }

    @Test
    void book_SlotFull() {
        testSlot.setBookedCount(10);
        BookingRequest request = new BookingRequest(1L, 1L, 1L, null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
        when(driveRepository.findById(1L)).thenReturn(Optional.of(testDrive));

        assertThrows(AppException.class, () -> 
            bookingService.book("test@example.com", request));
    }

    @Test
    void book_ExpiredSlotRejected() {
        LocalDateTime slotDateTime = LocalDateTime.now().minusHours(2);
        testSlot.setDateTime(slotDateTime);
        testSlot.setStartTime(slotDateTime.toLocalTime());
        testSlot.setEndTime(slotDateTime.plusHours(1).toLocalTime());
        BookingRequest request = new BookingRequest(1L, 1L, 1L, null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
        when(driveRepository.findById(1L)).thenReturn(Optional.of(testDrive));

        assertThrows(AppException.class, () ->
            bookingService.book("test@example.com", request));
    }

    @Test
    void reschedule_ExpiredSlotRejected() {
        Booking booking = Booking.builder()
            .id(1L)
            .user(testUser)
            .slot(testSlot)
            .status(BookingStatus.PENDING)
            .build();

        LocalDateTime slotDateTime = LocalDateTime.now().minusHours(2);
        Slot expiredSlot = Slot.builder()
            .id(2L)
            .drive(testDrive)
            .dateTime(slotDateTime)
            .capacity(10)
            .bookedCount(0)
            .startTime(slotDateTime.toLocalTime())
            .endTime(slotDateTime.plusHours(1).toLocalTime())
            .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(slotRepository.findById(2L)).thenReturn(Optional.of(expiredSlot));

        assertThrows(AppException.class, () ->
            bookingService.reschedule("test@example.com", 1L, new BookingRequest(1L, 1L, 2L, null)));
    }

    @Test
    void cancel_Success() {
        Booking booking = Booking.builder()
            .id(1L)
            .user(testUser)
            .slot(testSlot)
            .status(BookingStatus.PENDING)
            .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        Booking result = bookingService.cancel("test@example.com", 1L);

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(slotRepository).save(testSlot);
    }

    @Test
    void cancel_NotOwnBooking() {
        User otherUser = User.builder().id(2L).email("other@example.com").build();
        Booking booking = Booking.builder()
            .id(1L)
            .user(otherUser)
            .slot(testSlot)
            .status(BookingStatus.PENDING)
            .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(AppException.class, () -> 
            bookingService.cancel("test@example.com", 1L));
    }

    @Test
    void cancel_AlreadyCancelled() {
        Booking booking = Booking.builder()
            .id(1L)
            .user(testUser)
            .slot(testSlot)
            .status(BookingStatus.CANCELLED)
            .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(AppException.class, () -> 
            bookingService.cancel("test@example.com", 1L));
    }
}
