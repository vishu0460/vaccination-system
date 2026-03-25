package com.vaccine.common.dto;

public record SearchCityResult(
    String name,
    String state,
    double score,
    String matchType
) {
}
