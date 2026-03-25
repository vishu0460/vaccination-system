package com.vaccine.common.dto;
import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    String title,
    String type,
    String subject,
    String message,
    String reply,
    String status,
    String deliveryStatus,
    LocalDateTime scheduledTime,
    LocalDateTime createdAt,
    boolean read
) {
}
