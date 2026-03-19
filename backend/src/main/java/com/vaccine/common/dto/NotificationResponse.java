package com.vaccine.common.dto;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, String title, String message, LocalDateTime createdAt, boolean read) {
}
