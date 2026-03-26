package com.vaccine.common.dto;

import java.util.List;

public record DashboardAnalyticsResponse(
    Long totalUsers,
    Long totalBookings,
    Long activeDrives,
    Long availableSlots,
    Double slotFillRate,
    String mostSearchedCity,
    String mostBookedVaccine,
    List<PopularSlotInsightResponse> mostPopularSlots,
    List<BookingTrendPointResponse> dailyBookings,
    List<BookingTrendPointResponse> slotUsage
) {
}
