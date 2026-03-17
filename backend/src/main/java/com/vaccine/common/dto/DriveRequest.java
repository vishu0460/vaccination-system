package com.vaccine.common.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DriveRequest(
    String title,
    String description,
    Long centerId,
    LocalDate driveDate,
    Integer minAge,
    Integer maxAge,
    Boolean active
) {}
