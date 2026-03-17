package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    String channel,
    String message,
    boolean delivered,
    LocalDateTime createdAt
) {}
