package com.vaccine.common.dto;

import java.time.LocalDate;

public record SearchTrendPointResponse(
    LocalDate day,
    long count
) {
}
