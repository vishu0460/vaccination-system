package com.vaccine.common.dto;

public record FeedbackRequest(
    Integer rating,
    String subject,
    String message
) {}
