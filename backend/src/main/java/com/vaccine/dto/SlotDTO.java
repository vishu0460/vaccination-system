package com.vaccine.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class SlotDTO {
    
    private Long id;
    private Long driveId;
    private String driveName;
    private String vaccineName;
    private LocalDate slotDate;
    private LocalTime slotTime;
    private Integer totalCapacity;
    private Integer availableCapacity;
    
    public SlotDTO() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDriveId() { return driveId; }
    public void setDriveId(Long driveId) { this.driveId = driveId; }
    public String getDriveName() { return driveName; }
    public void setDriveName(String driveName) { this.driveName = driveName; }
    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getSlotTime() { return slotTime; }
    public void setSlotTime(LocalTime slotTime) { this.slotTime = slotTime; }
    public Integer getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(Integer totalCapacity) { this.totalCapacity = totalCapacity; }
    public Integer getAvailableCapacity() { return availableCapacity; }
    public void setAvailableCapacity(Integer availableCapacity) { this.availableCapacity = availableCapacity; }
}
