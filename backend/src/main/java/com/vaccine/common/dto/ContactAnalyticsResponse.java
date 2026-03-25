package com.vaccine.common.dto;

import java.util.List;

public record ContactAnalyticsResponse(
    Long totalInquiries,
    Long todayInquiries,
    Long weeklyInquiries,
    List<AnalyticsPointResponse> inquiriesByDay,
    List<AnalyticsPointResponse> mostActiveUsers,
    List<ContactSummaryResponse> recentInquiries
) {
}
