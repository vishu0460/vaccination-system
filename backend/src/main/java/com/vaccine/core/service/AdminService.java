package com.vaccine.core.service;

import com.vaccine.common.dto.AdminDashboardStatsResponse;
import com.vaccine.common.dto.CenterRequest;
import com.vaccine.common.dto.DriveRequest;
import com.vaccine.common.dto.SlotRequest;
import com.vaccine.domain.Booking;
import com.vaccine.domain.BookingStatus;
import com.vaccine.domain.Slot;
import com.vaccine.domain.User;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.NewsRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationCenterRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationDriveRepository driveRepository;
    private final SlotRepository slotRepository;
    private final NewsRepository newsRepository;

    public AdminService(UserRepository userRepository,
                        BookingRepository bookingRepository,
                        VaccinationCenterRepository centerRepository,
                        VaccinationDriveRepository driveRepository,
                        SlotRepository slotRepository,
                        NewsRepository newsRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.centerRepository = centerRepository;
        this.driveRepository = driveRepository;
        this.slotRepository = slotRepository;
        this.newsRepository = newsRepository;
    }

    public AdminDashboardStatsResponse dashboardStats() {
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long approvedBookings = bookingRepository.countByStatus(BookingStatus.APPROVED);
        long rejectedBookings = bookingRepository.countByStatus(BookingStatus.REJECTED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long totalCenters = centerRepository.count();
        long totalDrives = driveRepository.count();
        long activeDrives = driveRepository.countByActiveTrue();
        long totalSlots = slotRepository.count();
        long availableSlots = slotRepository.countAvailableSlots();
        long totalNews = newsRepository.countByActiveTrue();
        
        // Dynamic monthly and daily counts
        LocalDateTime monthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        long newUsersThisMonth = userRepository.countUsersSince(monthStart);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long bookingsToday = bookingRepository.countBookingsSince(todayStart);
        long completedVaccinations = bookingRepository.countByStatus(BookingStatus.COMPLETED);

        return new AdminDashboardStatsResponse(
            totalUsers, totalBookings, pendingBookings, approvedBookings, rejectedBookings, cancelledBookings,
            totalCenters, totalDrives, activeDrives, totalSlots, newUsersThisMonth, bookingsToday, completedVaccinations,
            totalCenters, availableSlots, totalNews
        );
    }

    public List<Booking> allBookings() {
        return bookingRepository.findAll();
    }

    public List<VaccinationCenter> allCenters() {
        return centerRepository.findAll();
    }

    public List<VaccinationDrive> allDrives() {
        return driveRepository.findAll();
    }

    public VaccinationCenter createCenter(CenterRequest request) {
        VaccinationCenter center = VaccinationCenter.builder()
                .name(request.name())
                .address(request.address())
                .city(request.city())
                .state(request.state())
                .pincode(request.pincode())
                .phone(request.phone())
                .email(request.email())
                .workingHours(request.workingHours())
                .dailyCapacity(request.dailyCapacity())
                .build();
        return centerRepository.save(center);
    }

    public VaccinationDrive createDrive(DriveRequest request) {
        if (request.centerId() == null || request.minAge() > request.maxAge()) {
            throw new AppException("Invalid center ID or age range");
        }
        
        VaccinationCenter center = centerRepository.findById(request.centerId())
                .orElseThrow(() -> new AppException("Center not found"));
        
        VaccinationDrive drive = VaccinationDrive.builder()
                .title(request.title())
                .description(request.description())
                .center(center)
                .driveDate(request.driveDate())
                .minAge(request.minAge())
                .maxAge(request.maxAge())
                .active(request.active())
                .build();
        return driveRepository.save(drive);
    }

    public Slot createSlot(SlotRequest request) {
        if (request.driveId() == null) {
            throw new AppException("Drive ID is required");
        }
        
        VaccinationDrive drive = driveRepository.findById(request.driveId())
                .orElseThrow(() -> new AppException("Drive not found"));
        
        if (request.startTime().isAfter(request.endTime())) {
            throw new AppException("Start time must be before end time");
        }
        
        Slot slot = Slot.builder()
                .drive(drive)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .capacity(request.capacity())
                .build();
        return slotRepository.save(slot);
    }

    public List<Slot> slotsByDrive(Long driveId) {
return slotRepository.findByDriveIdOrderByStartTimeAsc(driveId);
    }

    public Booking updateBookingStatus(Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException("Booking not found"));
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    public void deleteCenter(Long centerId) {
        if (!centerRepository.existsById(centerId)) {
            throw new AppException("Center not found");
        }
        centerRepository.deleteById(centerId);
    }

    public void deleteDrive(Long driveId) {
        if (!driveRepository.existsById(driveId)) {
            throw new AppException("Drive not found");
        }
        driveRepository.deleteById(driveId);
    }
}
