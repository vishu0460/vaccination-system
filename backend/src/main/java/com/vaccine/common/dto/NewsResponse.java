package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record NewsResponse(
    Long id,
    String title,
    String content,
    boolean published,
    LocalDateTime publishedAt,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {}
