package com.vaccine.service;

import com.vaccine.dto.BookingDTO;
import com.vaccine.dto.BookingRequest;
import com.vaccine.entity.*;
import com.vaccine.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VaccinationDriveRepository driveRepository;
    
    @Autowired
    private SlotRepository slotRepository;
    
    public List<BookingDTO> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<BookingDTO> getUserBookingsByStatus(Long userId, Booking.BookingStatus status) {
        return bookingRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public BookingDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return mapToDTO(booking);
    }
    
    @Transactional
    public BookingDTO createBooking(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        VaccinationDrive drive = driveRepository.findById(request.getDriveId())
                .orElseThrow(() -> new RuntimeException("Drive not found"));
        
        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        
        List<Booking> activeBookings = bookingRepository.findActiveBookingsByUserAndDrive(userId, request.getDriveId());
        if (!activeBookings.isEmpty()) {
            throw new RuntimeException("You already have an active booking for this drive");
        }
        
        if (slot.getAvailableCapacity() <= 0) {
            throw new RuntimeException("No slots available");
        }
        
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setDrive(drive);
        booking.setSlot(slot);
        booking.setAppointmentDate(request.getAppointmentDate());
        if (request.getAppointmentTime() != null) {
            booking.setAppointmentTime(LocalTime.parse(request.getAppointmentTime()));
        }
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        
        slot.setAvailableCapacity(slot.getAvailableCapacity() - 1);
        slotRepository.save(slot);
        
        booking = bookingRepository.save(booking);
        return mapToDTO(booking);
    }
    
    @Transactional
    public BookingDTO updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        booking.setStatus(status);
        booking.setUpdatedAt(LocalDateTime.now());
        
        if (status == Booking.BookingStatus.CANCELLED) {
            Slot slot = booking.getSlot();
            slot.setAvailableCapacity(slot.getAvailableCapacity() + 1);
            slotRepository.save(slot);
        }
        
        booking = bookingRepository.save(booking);
        return mapToDTO(booking);
    }
    
    @Transactional
    public BookingDTO cancelBooking(Long id, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed booking");
        }
        
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());
        
        Slot slot = booking.getSlot();
        slot.setAvailableCapacity(slot.getAvailableCapacity() + 1);
        slotRepository.save(slot);
        
        booking = bookingRepository.save(booking);
        return mapToDTO(booking);
    }
    
    public List<BookingDTO> getPendingBookings() {
        return bookingRepository.findPendingBookings().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    private BookingDTO mapToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setUserName(booking.getUser().getFullName());
        dto.setUserEmail(booking.getUser().getEmail());
        dto.setDriveId(booking.getDrive().getId());
        dto.setDriveName(booking.getDrive().getName());
        dto.setVaccineName(booking.getDrive().getVaccineName());
        dto.setCenterId(booking.getDrive().getCenter().getId());
        dto.setCenterName(booking.getDrive().getCenter().getName());
        dto.setAppointmentDate(booking.getAppointmentDate());
        if (booking.getAppointmentTime() != null) {
            dto.setAppointmentTime(booking.getAppointmentTime().toString());
        }
        dto.setStatus(booking.getStatus().name());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        dto.setCancellationReason(booking.getCancellationReason());
        return dto;
    }
}
