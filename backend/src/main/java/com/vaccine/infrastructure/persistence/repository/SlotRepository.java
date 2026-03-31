package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Slot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :slotId")
    java.util.Optional<Slot> findByIdForUpdate(Long slotId);

    @Query("SELECT s FROM Slot s WHERE s.drive.id = :driveId ORDER BY s.dateTime ASC")
    List<Slot> findByDrive_IdOrderByDateTimeAsc(Long driveId);

    @Query("SELECT s FROM Slot s WHERE s.drive.id = :driveId")
    List<Slot> findByDrive_Id(Long driveId);

    List<Slot> findByAdminId(Long adminId);

    @Query("SELECT s FROM Slot s WHERE s.dateTime >= :now AND s.bookedCount < s.capacity ORDER BY s.dateTime")
    List<Slot> findAvailableSlots(LocalDateTime now);

    @Query("SELECT COUNT(s) FROM Slot s WHERE s.bookedCount < s.capacity")
    long countAvailableSlots();

    @Query("SELECT COALESCE(SUM(s.capacity - s.bookedCount), 0) FROM Slot s WHERE s.bookedCount < s.capacity")
    long sumAvailableCapacity();

    @Query("SELECT COALESCE(SUM(s.capacity), 0) FROM Slot s WHERE s.drive.id = :driveId")
    long sumCapacityByDriveId(Long driveId);

    @Query("SELECT COALESCE(SUM(s.capacity - s.bookedCount), 0) FROM Slot s WHERE s.drive.id = :driveId")
    long sumAvailableCapacityByDriveId(Long driveId);
}
