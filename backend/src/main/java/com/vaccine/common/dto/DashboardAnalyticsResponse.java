package com.vaccine.common.dto;

public record DashboardAnalyticsResponse(
    Long totalUsers,
    Long totalBookings,
    Long activeDrives,
    Long availableSlots,
    String mostSearchedCity,
    String mostBookedVaccine
) {
}
