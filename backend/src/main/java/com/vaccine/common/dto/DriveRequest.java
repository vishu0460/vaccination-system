package com.vaccine.common.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DriveRequest(
    String title,
    String description,
    String vaccineType,
    Long centerId,
    LocalDate driveDate,
    Integer minAge,
    Integer maxAge,
    Integer totalSlots,
    Boolean active
) {}
