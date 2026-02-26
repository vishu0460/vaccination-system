package com.vaccine.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class DriveDTO {
    
    private Long id;
    private String name;
    private String description;
    private String vaccineName;
    private String vaccineManufacturer;
    private Integer minAge;
    private Integer maxAge;
    private Integer dosesRequired;
    private Integer doseGapDays;
    private Long centerId;
    private String centerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalSlots;
    private Integer availableSlots;
    private Boolean isActive;
    
    public DriveDTO() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
    public String getVaccineManufacturer() { return vaccineManufacturer; }
    public void setVaccineManufacturer(String vaccineManufacturer) { this.vaccineManufacturer = vaccineManufacturer; }
    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }
    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }
    public Integer getDosesRequired() { return dosesRequired; }
    public void setDosesRequired(Integer dosesRequired) { this.dosesRequired = dosesRequired; }
    public Integer getDoseGapDays() { return doseGapDays; }
    public void setDoseGapDays(Integer doseGapDays) { this.doseGapDays = doseGapDays; }
    public Long getCenterId() { return centerId; }
    public void setCenterId(Long centerId) { this.centerId = centerId; }
    public String getCenterName() { return centerName; }
    public void setCenterName(String centerName) { this.centerName = centerName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getTotalSlots() { return totalSlots; }
    public void setTotalSlots(Integer totalSlots) { this.totalSlots = totalSlots; }
    public Integer getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(Integer availableSlots) { this.availableSlots = availableSlots; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
