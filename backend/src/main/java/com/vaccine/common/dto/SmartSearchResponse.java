package com.vaccine.common.dto;

import java.util.List;

public record SmartSearchResponse(
    String query,
    String normalizedQuery,
    String didYouMean,
    String cityFilter,
    String detectedCity,
    List<SearchCityResult> cities,
    List<SearchCenterResult> centers,
    List<SearchDriveResult> drives,
    int totalResults
) {
}
