package com.vaccine.common.dto;

import java.time.LocalDateTime;

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
    String slotStatus
) {
}
