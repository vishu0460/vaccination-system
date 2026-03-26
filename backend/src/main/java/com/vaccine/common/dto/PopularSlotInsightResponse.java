package com.vaccine.common.dto;

public record PopularSlotInsightResponse(
    Long slotId,
    String driveName,
    String centerName,
    Long bookingCount,
    Double fillRate
) {
}
