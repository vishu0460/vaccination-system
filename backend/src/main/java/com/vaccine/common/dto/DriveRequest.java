package com.vaccine.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vaccine.domain.Status;
import java.time.LocalDate;

public record DriveRequest(
    String title,
    String description,
    String vaccineType,
    Long centerId,
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate driveDate,
    Integer minAge,
    Integer maxAge,
    Integer totalSlots,
    Boolean secondDoseRequired,
    Integer secondDoseGapDays,
    Status status,
    Boolean active
) {}
