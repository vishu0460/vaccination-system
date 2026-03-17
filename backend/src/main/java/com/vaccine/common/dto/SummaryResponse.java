package com.vaccine.common.dto;

public record SummaryResponse(
    Long totalCenters,
    Long totalDrives,
    Long availableSlots,
    Long totalUsers,
    Double averageRating
) {}
