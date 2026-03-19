package com.vaccine.common.dto;

import com.vaccine.domain.VaccinationDrive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DriveResponse(
    Long id,
    String title,
    String vaccineType,
    LocalDate driveDate,
    String centerName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    int availableSlots,
    boolean active
) {
    public static DriveResponse from(VaccinationDrive drive) {
        return new DriveResponse(
            drive.getId(),
            drive.getTitle(),
            drive.getVaccineType(),
            drive.getDriveDate(),
            drive.getCenter().getName(),
            drive.getStartTime().atDate(drive.getDriveDate()),
            drive.getEndTime().atDate(drive.getDriveDate()),
            drive.getTotalSlots(),
            drive.getActive()
        );
    }
    
    public static class SlotResponse {
        private Long id;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int capacity;
        private int bookedCount;
        
        public SlotResponse(Long id, LocalDateTime startTime, LocalDateTime endTime, int capacity, int bookedCount) {
            this.id = id;
            this.startTime = startTime;
            this.endTime = endTime;
            this.capacity = capacity;
            this.bookedCount = bookedCount;
        }
        
        // getters
        public Long getId() { return id; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getCapacity() { return capacity; }
        public int getBookedCount() { return bookedCount; }
    }
}
