package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record AdminSlotResponse(
    Long id,
    LocalDateTime time,
    LocalDateTime endTime,
    Integer capacity,
    Integer bookedCount,
    Integer remaining,
    Long centerId,
    String centerName,
    Long driveId,
    String driveName,
    String slotStatus
) {
}
