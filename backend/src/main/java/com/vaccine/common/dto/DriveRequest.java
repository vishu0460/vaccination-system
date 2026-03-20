package com.vaccine.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

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
    Boolean active
) {}
