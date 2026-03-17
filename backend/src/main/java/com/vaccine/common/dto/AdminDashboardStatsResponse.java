package com.vaccine.common.dto;

public record AdminDashboardStatsResponse(
    Long totalUsers,
    Long totalBookings,
    Long pendingBookings,
    Long approvedBookings,
    Long rejectedBookings,
    Long cancelledBookings,
    Long completedVaccinations,
    Long totalCenters,
    Long totalDrives,
    Long activeDrives,
    Long totalSlots,
    Long availableSlots,
    Long newUsersThisMonth,
    Long bookingsToday,
    Long totalNews
) {}

