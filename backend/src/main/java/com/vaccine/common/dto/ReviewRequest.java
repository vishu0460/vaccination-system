package com.vaccine.common.dto;

public record ReviewRequest(
    Long centerId,
    Integer rating,
    String comment
) {}
