package com.vaccine.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalBookings;
    private Long pendingBookings;
    private Long approvedBookings;
    private Long rejectedBookings;
    private Long cancelledBookings;
    private Long completedVaccinations;
    private Long totalCenters;
    private Long totalDrives;
    private Long activeDrives;
    private Long totalSlots;
    private Long availableSlots;
    private Long newUsersThisMonth;
    private Long bookingsToday;
    private Long totalNews;
}
