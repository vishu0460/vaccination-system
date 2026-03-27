package com.vaccine.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vaccine.domain.Status;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DriveRequest(
    @Size(max = 255, message = "Title must be 255 characters or fewer")
    String title,

    @Size(max = 5000, message = "Description must be 5000 characters or fewer")
    String description,

    @Size(max = 255, message = "Vaccine type must be 255 characters or fewer")
    String vaccineType,

    @Min(value = 1, message = "Center ID must be positive")
    Long centerId,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate driveDate,

    @Min(value = 0, message = "Minimum age cannot be negative")
    Integer minAge,

    @Min(value = 0, message = "Maximum age cannot be negative")
    Integer maxAge,

    @Min(value = 1, message = "Total slots must be at least 1")
    Integer totalSlots,
    Boolean secondDoseRequired,

    @Min(value = 1, message = "Second dose gap must be at least 1 day")
    Integer secondDoseGapDays,
    Status status,
    Boolean active
) {
    @AssertTrue(message = "Maximum age must be greater than or equal to minimum age")
    public boolean isAgeRangeValid() {
        return minAge == null || maxAge == null || maxAge >= minAge;
    }
}
