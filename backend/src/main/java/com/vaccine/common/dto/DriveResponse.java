package com.vaccine.common.dto;

import com.vaccine.domain.Status;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.util.DriveStatusResolver;
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
    Status status,
    String realtimeStatus,
    Boolean bookable
) {
    public static DriveResponse from(VaccinationDrive drive) {
        String realtimeStatus = DriveStatusResolver.resolve(drive);
        return new DriveResponse(
            drive.getId(),
            drive.getTitle(),
            drive.getVaccineType(),
            drive.getDriveDate(),
            drive.getCenter().getName(),
            DriveStatusResolver.resolveStart(drive),
            DriveStatusResolver.resolveEnd(drive),
            drive.getTotalSlots(),
            drive.getStatus(),
            realtimeStatus,
            !"EXPIRED".equals(realtimeStatus)
        );
    }
    
    public static class SlotResponse {
        private Long id;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int capacity;
        private int bookedCount;
        private int availableSlots;
        private String status;
        
        public SlotResponse(Long id, LocalDateTime startTime, LocalDateTime endTime, int capacity, int bookedCount, int availableSlots, String status) {
            this.id = id;
            this.startTime = startTime;
            this.endTime = endTime;
            this.capacity = capacity;
            this.bookedCount = bookedCount;
            this.availableSlots = availableSlots;
            this.status = status;
        }
        
        // getters
        public Long getId() { return id; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getCapacity() { return capacity; }
        public int getBookedCount() { return bookedCount; }
        public int getAvailableSlots() { return availableSlots; }
        public String getStatus() { return status; }
    }
}
