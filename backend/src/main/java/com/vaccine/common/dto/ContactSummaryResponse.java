package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record ContactSummaryResponse(
    Long id,
    String userName,
    String userEmail,
    String subject,
    String message,
    String status,
    LocalDateTime createdAt
) {
}
