package com.vaccine.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SlotResponse(
    Long id,
    LocalDateTime startTime,
    LocalDateTime endTime,
    int capacity,
    int bookedCount,
    int remaining,
    LocalDate date,
    LocalTime slotStartTime,
    LocalTime slotEndTime,
    int totalCapacity,
    int availableSlots,
    String status
) {
}
