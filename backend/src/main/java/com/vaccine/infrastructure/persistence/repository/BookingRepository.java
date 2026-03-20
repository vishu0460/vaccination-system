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

    List<Booking> findBySlotId(Long slotId);

    boolean existsByUserIdAndSlotIdAndStatusIn(Long userId, Long slotId, List<BookingStatus> statuses);

    long countByStatus(BookingStatus status);

@Query("SELECT COUNT(b) FROM Booking b WHERE b.bookedAt >= :since")
    long countBookingsSince(LocalDateTime since);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.user.id = :userId " +
           "AND b.slot.startTime BETWEEN :start AND :end " +
           "AND b.status IN :statuses")
    boolean existsByUserIdAndSlotStartTimeBetweenAndStatusIn(Long userId, LocalDateTime start, LocalDateTime end, List<BookingStatus> statuses);
}
