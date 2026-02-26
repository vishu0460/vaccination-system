package com.vaccine.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class BookingRequest {
    
    @NotNull(message = "Drive ID is required")
    private Long driveId;
    
    @NotNull(message = "Slot ID is required")
    private Long slotId;
    
    @NotNull(message = "Appointment date is required")
    private LocalDate appointmentDate;
    
    private String appointmentTime;
    
    public BookingRequest() {}

    public Long getDriveId() { return driveId; }
    public void setDriveId(Long driveId) { this.driveId = driveId; }
    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
}
