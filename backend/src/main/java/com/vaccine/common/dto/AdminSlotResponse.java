package com.vaccine.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AdminSlotResponse(
    Long id,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    Integer capacity,
    Integer bookedCount,
    Integer remaining,
    Boolean available,
    Boolean bookable,
    Long centerId,
    String centerName,
    String centerCity,
    Long driveId,
    String driveName,
    Boolean almostFull,
    String demandLevel,
    String slotStatus,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    Integer totalCapacity,
    Integer availableSlots,
    String status
) {
}
