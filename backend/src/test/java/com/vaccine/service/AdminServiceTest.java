package com.vaccine.core.service;

import com.vaccine.domain.AdminServiceTest; // Wait, no.

import com.vaccine.common.dto.AdminDashboardStatsResponse;
import com.vaccine.common.dto.CenterRequest;
import com.vaccine.common.dto.DriveRequest;
import com.vaccine.common.dto.SlotRequest;
import com.vaccine.domain.*;
import com.vaccine.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*; 

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private VaccinationCenterRepository centerRepository;
    @Mock
    private VaccinationDriveRepository driveRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private NewsRepository newsRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
            userRepository,
            bookingRepository,
            centerRepository,
            driveRepository,
            slotRepository,
            newsRepository
        );
    }

    @Test
    void createCenter_WithValidRequest_ShouldCreateCenter() {
CenterRequest req = new CenterRequest("Test Center", "Test Address", 12345, "Test City", "Test State", 12.34, 56.78);
        
        when(centerRepository.save(any(VaccinationCenter.class))).thenAnswer(inv -> {
            VaccinationCenter center = inv.getArgument(0);
            center.setId(1L);
            return center;
        });

        VaccinationCenter result = adminService.createCenter(req);

        assertNotNull(result);
        assertEquals("Test Center", result.getName());
        assertEquals("Test City", result.getCity());
        verify(centerRepository).save(any(VaccinationCenter.class));
    }

    @Test
    void createDrive_WithValidRequest_ShouldCreateDrive() {
        VaccinationCenter center = VaccinationCenter.builder().id(1L).name("Center").city("City").build();
DriveRequest req = new DriveRequest("Drive Title", "Description", 1L, LocalDate.now().plusDays(7), 18, 60);

        when(centerRepository.findById(1L)).thenReturn(Optional.of(center));
        when(driveRepository.save(any(VaccinationDrive.class))).thenAnswer(inv -> inv.getArgument(0));

        VaccinationDrive result = adminService.createDrive(req);

        assertNotNull(result);
        assertEquals("Drive Title", result.getTitle());
        verify(driveRepository).save(any(VaccinationDrive.class));
    }

    @Test
    void createDrive_WithInvalidAgeRange_ShouldThrowException() {
DriveRequest req = new DriveRequest("Drive", "Desc", 1L, LocalDate.now().plusDays(7), 60, 18);

        assertThrows(AppException.class, () -> adminService.createDrive(req));
    }

    @Test
    void createSlot_WithValidRequest_ShouldCreateSlot() {
        VaccinationDrive drive = VaccinationDrive.builder().id(1L).title("Drive").build();
        LocalDateTime startTime = LocalDateTime.now().plusDays(7).withHour(9).withMinute(0);
        LocalDateTime endTime = LocalDateTime.now().plusDays(7).withHour(12).withMinute(0);
        SlotRequest req = new SlotRequest(1L, startTime, endTime, 50);

        when(driveRepository.findById(1L)).thenReturn(Optional.of(drive));
        when(slotRepository.save(any(Slot.class))).thenAnswer(inv -> inv.getArgument(0));

        Slot result = adminService.createSlot(req);

        assertNotNull(result);
        assertEquals(50, result.getCapacity());
        verify(slotRepository).save(any(Slot.class));
    }

    @Test
    void createSlot_WithInvalidTime_ShouldThrowException() {
        VaccinationDrive drive = VaccinationDrive.builder().id(1L).build();
        LocalDateTime startTime = LocalDateTime.now().plusDays(7).withHour(12);
        LocalDateTime endTime = LocalDateTime.now().plusDays(7).withHour(9);
        SlotRequest req = new SlotRequest(1L, startTime, endTime, 50);

        when(driveRepository.findById(1L)).thenReturn(Optional.of(drive));

        assertThrows(AppException.class, () -> adminService.createSlot(req));
    }

    @Test
    void updateBookingStatus_WithValidId_ShouldUpdateStatus() {
        Booking booking = Booking.builder().id(1L).status(BookingStatus.PENDING).build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = adminService.updateBookingStatus(1L, BookingStatus.APPROVED);

        assertEquals(BookingStatus.APPROVED, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void dashboardStats_ShouldReturnCorrectStats() {
        when(userRepository.count()).thenReturn(100L);
        when(bookingRepository.count()).thenReturn(5L);
        when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(1L);
        when(bookingRepository.countByStatus(BookingStatus.APPROVED)).thenReturn(1L);
        when(bookingRepository.countByStatus(BookingStatus.REJECTED)).thenReturn(1L);
        when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(1L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(1L);
        when(centerRepository.count()).thenReturn(5L);
        when(driveRepository.count()).thenReturn(10L);
        when(driveRepository.countByActiveTrue()).thenReturn(7L);
        when(slotRepository.count()).thenReturn(20L);
        when(slotRepository.countAvailableSlots()).thenReturn(12L);
        when(newsRepository.countByActiveTrue()).thenReturn(4L);
        when(userRepository.countUsersSince(any())).thenReturn(10L);
        when(bookingRepository.countBookingsSince(any())).thenReturn(3L);

        AdminDashboardStatsResponse result = adminService.dashboardStats();

        assertEquals(100, result.totalUsers());
        assertEquals(5, result.totalBookings());
        assertEquals(5, result.totalCenters());
        assertEquals(10, result.totalDrives());
        assertEquals(7, result.activeDrives());
        assertEquals(20, result.totalSlots());
        assertEquals(10, result.newUsersThisMonth());
        assertEquals(3, result.bookingsToday());
        assertEquals(5, result.centers());
        assertEquals(12, result.availableSlots());
        assertEquals(4, result.news());
    }

    @Test
    void allBookings_ShouldReturnAllBookings() {
        List<Booking> bookings = List.of(
            Booking.builder().id(1L).build(),
            Booking.builder().id(2L).build()
        );
        when(bookingRepository.findAll()).thenReturn(bookings);

        List<Booking> result = adminService.allBookings();

        assertEquals(2, result.size());
    }

    @Test
    void allCenters_ShouldReturnAllCenters() {
        List<VaccinationCenter> centers = List.of(
            VaccinationCenter.builder().id(1L).name("Center 1").build(),
            VaccinationCenter.builder().id(2L).name("Center 2").build()
        );
        when(centerRepository.findAll()).thenReturn(centers);

        List<VaccinationCenter> result = adminService.allCenters();

        assertEquals(2, result.size());
    }

    @Test
    void slotsByDrive_ShouldReturnSlotsForDrive() {
        List<Slot> slots = List.of(
            Slot.builder().id(1L).build(),
            Slot.builder().id(2L).build()
        );
        when(slotRepository.findByDriveIdOrderByStartTimeAsc(1L)).thenReturn(slots);

        List<Slot> result = adminService.slotsByDrive(1L);

        assertEquals(2, result.size());
    }
}
