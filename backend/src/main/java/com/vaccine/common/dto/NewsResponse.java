package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record NewsResponse(
    Long id,
    String title,
    String content,
    String summary,
    String imageUrl,
    Integer priority,
    Boolean active,
    LocalDateTime publishedAt,
    LocalDateTime expiresAt,
    String category,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
