package com.vaccine.common.dto;

import com.vaccine.domain.Booking;
import com.vaccine.util.SlotStatusResolver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BookingResponse(
    Long id,
    String status,
    LocalDateTime bookedAt,
    LocalDateTime assignedTime,
    String notes,
    Long slotId,
    LocalDateTime slotTime,
    String userName,
    String userEmail,
    String centerName,
    String driveName,
    LocalDate slotDate,
    LocalDateTime slotEndTime,
    LocalTime startTime,
    LocalTime endTime
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
            booking.getId(),
            booking.getStatus() != null ? booking.getStatus().name() : null,
            booking.getBookedAt(),
            booking.getAssignedTime(),
            booking.getNotes(),
            booking.getSlot() != null ? booking.getSlot().getId() : null,
            booking.getSlot() != null ? booking.getSlot().getDateTime() : null,
            booking.getUser() != null ? booking.getUser().getFullName() : null,
            booking.getUser() != null ? booking.getUser().getEmail() : null,
            booking.getSlot() != null && booking.getSlot().getDrive() != null && booking.getSlot().getDrive().getCenter() != null
                ? booking.getSlot().getDrive().getCenter().getName() : null,
            booking.getSlot() != null && booking.getSlot().getDrive() != null
                ? booking.getSlot().getDrive().getTitle() : null,
            booking.getSlot() != null ? booking.getSlot().getSlotDate() : null,
            booking.getSlot() != null ? SlotStatusResolver.resolveEnd(booking.getSlot()) : null,
            booking.getSlot() != null ? booking.getSlot().getStartTime() : null,
            booking.getSlot() != null ? booking.getSlot().getEndTime() : null
        );
    }
}
