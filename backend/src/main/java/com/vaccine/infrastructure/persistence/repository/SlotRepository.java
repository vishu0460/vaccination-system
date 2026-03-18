package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByDriveId(Long driveId);

    @Query("SELECT s FROM Slot s WHERE s.drive.id = :driveId ORDER BY s.startTime ASC")
    List<Slot> findByDriveIdOrderByStartTimeAsc(Long driveId);

    @Query("SELECT s FROM Slot s WHERE s.dateTime >= :now AND s.bookedCount < s.capacity ORDER BY s.dateTime")
    List<Slot> findAvailableSlots(LocalDateTime now);

    long countAvailableSlots();
}
