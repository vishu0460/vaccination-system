package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record NewsRequest(
    String title,
    String content,
    String summary,
    String imageUrl,
    Integer priority,
    Boolean active,
    String category,
    boolean published,
    LocalDateTime publishedAt,
    LocalDateTime expiresAt
) {}
