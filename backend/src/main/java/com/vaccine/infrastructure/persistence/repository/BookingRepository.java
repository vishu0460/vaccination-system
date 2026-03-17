package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Booking;
import com.vaccine.domain.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatus(BookingStatus status);

    Optional<Booking> findByIdAndStatus(Long id, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId")
    List<Booking> findByUserId(Long userId);

    long countByStatus(BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookedAt >= :since")
    long countBookingsSince(LocalDateTime since);
}
