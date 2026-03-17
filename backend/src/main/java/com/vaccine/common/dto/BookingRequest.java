package com.vaccine.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
    @NotNull @Min(1) Long driveId,
    @NotNull @Min(1) Long slotId,
    String notes
) {}
