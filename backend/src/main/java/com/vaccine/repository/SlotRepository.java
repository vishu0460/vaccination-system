package com.vaccine.repository;

import com.vaccine.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    
    List<Slot> findByDriveId(Long driveId);
    
    @Query("SELECT s FROM Slot s WHERE s.drive.id = :driveId AND s.slotDate = :date AND s.availableCapacity > 0")
    List<Slot> findAvailableSlotsByDriveAndDate(Long driveId, LocalDate date);
    
    @Query("SELECT s FROM Slot s WHERE s.slotDate >= :startDate AND s.slotDate <= :endDate AND s.availableCapacity > 0")
    List<Slot> findAvailableSlotsBetweenDates(LocalDate startDate, LocalDate endDate);
    
    Optional<Slot> findByDriveIdAndSlotDateAndSlotTime(Long driveId, LocalDate date, LocalTime slotTime);
}
