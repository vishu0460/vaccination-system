package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record NewsRequest(
    String title,
    String content,
    boolean published,
    LocalDateTime publishedAt,
    LocalDateTime expiresAt
) {}
