package com.vaccine.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DriveResponse(
    Long id,
    String title,
    String description,
    Long centerId,
    String centerName,
    LocalDate driveDate,
    Integer minAge,
    Integer maxAge,
    boolean active
) {
    public static DriveResponse from(com.vaccine.domain.VaccinationDrive drive) {
        return new DriveResponse(
            drive.getId(),
            drive.getTitle(),
            drive.getDescription(),
            drive.getCenter().getId(),
            drive.getCenter().getName(),
            drive.getDriveDate(),
            drive.getMinAge(),
            drive.getMaxAge(),
            drive.getActive()
        );
    }
}
