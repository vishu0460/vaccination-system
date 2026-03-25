package com.vaccine.common.dto;

public record SearchCenterResult(
    Long id,
    String name,
    String city,
    String state,
    String address,
    Double distanceKm,
    double score,
    String matchType
) {
}
