package com.vaccine.repository;

import com.vaccine.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserId(Long userId);
    
    List<Booking> findByUserIdAndStatus(Long userId, Booking.BookingStatus status);
    
    List<Booking> findByStatus(Booking.BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' ORDER BY b.createdAt ASC")
    List<Booking> findPendingBookings();
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.drive.id = :driveId AND b.status IN ('PENDING', 'APPROVED')")
    List<Booking> findActiveBookingsByUserAndDrive(Long userId, Long driveId);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'PENDING'")
    long countPendingBookings();
    
    @Query("SELECT b FROM Booking b WHERE b.appointmentDate = :date")
    List<Booking> findByAppointmentDate(LocalDate date);
}
