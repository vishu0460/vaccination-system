package com.vaccine.common.dto;

import java.util.List;

public record NearbyCentersResponse(
    String detectedCity,
    boolean reverseGeocoded,
    List<SearchCenterResult> centers
) {
}
