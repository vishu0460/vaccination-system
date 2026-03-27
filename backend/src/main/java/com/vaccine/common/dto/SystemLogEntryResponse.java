package com.vaccine.common.dto;

public record SystemLogEntryResponse(
    String timestamp,
    String level,
    String message,
    String service,
    String requestId,
    String userId,
    String userEmail,
    String requestPath,
    String httpMethod,
    String logger,
    String stackTrace,
    String raw
) {}
