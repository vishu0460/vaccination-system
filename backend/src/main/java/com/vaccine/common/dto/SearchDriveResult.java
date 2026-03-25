package com.vaccine.common.dto;

import java.time.LocalDate;

public record SearchDriveResult(
    Long id,
    String title,
    String city,
    String centerName,
    LocalDate driveDate,
    String vaccineType,
    Long availableSlots,
    double score,
    String matchType
) {
}
