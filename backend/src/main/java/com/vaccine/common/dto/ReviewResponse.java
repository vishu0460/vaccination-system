package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record ReviewResponse(Long id, Integer rating, String comment, LocalDateTime createdAt, String userFullName, Long centerId, String centerName) {
}

