package com.vaccine.common.dto;

import java.util.List;

public record SearchAnalyticsResponse(
    long totalSearches,
    List<SearchMetricResponse> topCities,
    List<SearchMetricResponse> topKeywords,
    List<SearchTrendPointResponse> trends
) {
}
