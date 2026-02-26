package com.vaccine.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "slots")
public class Slot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drive_id", nullable = false)
    private VaccinationDrive drive;
    
    @Column(name = "slot_date")
    private LocalDate slotDate;
    
    @Column(name = "slot_time")
    private LocalTime slotTime;
    
    @Column(name = "total_capacity")
    private Integer totalCapacity;
    
    @Column(name = "available_capacity")
    private Integer availableCapacity;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public Slot() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public VaccinationDrive getDrive() { return drive; }
    public void setDrive(VaccinationDrive drive) { this.drive = drive; }
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getSlotTime() { return slotTime; }
    public void setSlotTime(LocalTime slotTime) { this.slotTime = slotTime; }
    public Integer getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(Integer totalCapacity) { this.totalCapacity = totalCapacity; }
    public Integer getAvailableCapacity() { return availableCapacity; }
    public void setAvailableCapacity(Integer availableCapacity) { this.availableCapacity = availableCapacity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
