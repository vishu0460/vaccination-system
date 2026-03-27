package com.vaccine.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record NewsRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must be 500 characters or fewer")
    String title,

    @NotBlank(message = "Content is required")
    @Size(max = 20000, message = "Content must be 20000 characters or fewer")
    String content,

    @Size(max = 1000, message = "Summary must be 1000 characters or fewer")
    String summary,

    @Size(max = 500, message = "Image URL must be 500 characters or fewer")
    String imageUrl,

    @Min(value = 0, message = "Priority cannot be negative")
    Integer priority,
    Boolean active,

    @Size(max = 50, message = "Category must be 50 characters or fewer")
    String category,
    boolean published,
    LocalDateTime publishedAt,
    LocalDateTime expiresAt
) {}
