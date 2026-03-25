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

    @Query("SELECT b FROM Booking b WHERE lower(b.user.email) = lower(:email) ORDER BY b.bookedAt DESC")
    List<Booking> findByUserEmailOrderByBookedAtDesc(String email);

    List<Booking> findBySlotId(Long slotId);
    List<Booking> findByAdminId(Long adminId);

    boolean existsByUserIdAndSlotIdAndStatusIn(Long userId, Long slotId, List<BookingStatus> statuses);

    long countByStatus(BookingStatus status);

@Query("SELECT COUNT(b) FROM Booking b WHERE b.bookedAt >= :since")
    long countBookingsSince(LocalDateTime since);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
           "WHERE b.user.id = :userId " +
           "AND b.slot.dateTime BETWEEN :start AND :end " +
           "AND b.status IN :statuses")
    boolean existsByUserIdAndSlotDateTimeBetweenAndStatusIn(Long userId, LocalDateTime start, LocalDateTime end, List<BookingStatus> statuses);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = :status
          AND b.assignedTime IS NOT NULL
          AND b.assignedTime BETWEEN :start AND :end
    """)
    List<Booking> findScheduledBookingsForWindow(BookingStatus status, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = :status
          AND b.secondDoseRequired = true
          AND b.nextDoseDueDate IS NOT NULL
          AND b.nextDoseDueDate BETWEEN :start AND :end
    """)
    List<Booking> findSecondDoseBookingsForWindow(BookingStatus status, LocalDateTime start, LocalDateTime end);

    default boolean existsByUserIdAndSlotStartTimeBetweenAndStatusIn(Long userId, LocalDateTime start, LocalDateTime end, List<BookingStatus> statuses) {
        return existsByUserIdAndSlotDateTimeBetweenAndStatusIn(userId, start, end, statuses);
    }
}
