package com.vaccine.service;

import com.vaccine.common.dto.BookingRequest;\nimport com.vaccine.common.exception.AppException;\nimport com.vaccine.infrastructure.persistence.repository.UserRepository;\nimport com.vaccine.domain.User;\nimport com.vaccine.domain.Slot;\nimport com.vaccine.domain.Booking;\nimport com.vaccine.domain.VaccinationDrive;\nimport com.vaccine.domain.VaccinationCenter;\nimport com.vaccine.domain.BookingStatus;
import com.vaccine.domain.*;
import com.vaccine.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    private INotificationService notificationService;

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
            .center(testCenter)
            .build();

        testSlot = Slot.builder()
            .id(1L)
            .drive(testDrive)
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
            .capacity(10)
            .bookedCount(0)
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
        BookingRequest request = new BookingRequest(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
        when(bookingRepository.existsByUserIdAndSlotStartTimeBetweenAndStatusIn(
            any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        Booking result = bookingService.book("test@example.com", request);

        assertNotNull(result);
        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertEquals(1, testSlot.getBookedCount());
        verify(slotRepository).save(testSlot);
    }

    @Test
    void book_AgeNotEligible() {
        testUser.setAge(15);
        BookingRequest request = new BookingRequest(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(testSlot));

        assertThrows(AppException.class, () -> 
            bookingService.book("test@example.com", request));
    }

    @Test
    void book_SlotFull() {
        testSlot.setBookedCount(10);
        BookingRequest request = new BookingRequest(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(testSlot));

        assertThrows(AppException.class, () -> 
            bookingService.book("test@example.com", request));
    }

    @Test
    void book_ConflictExists() {
        BookingRequest request = new BookingRequest(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(testSlot));
        when(bookingRepository.existsByUserIdAndSlotStartTimeBetweenAndStatusIn(
            any(), any(), any(), any())).thenReturn(true);

        assertThrows(AppException.class, () -> 
            bookingService.book("test@example.com", request));
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
        assertEquals(0, testSlot.getBookedCount());
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
