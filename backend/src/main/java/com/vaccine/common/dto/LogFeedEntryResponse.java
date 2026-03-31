package com.vaccine.common.dto;

public record LogFeedEntryResponse(
    String id,
    String timestamp,
    String source,
    String category,
    String actionType,
    String actorName,
    String actorRole,
    String readableMessage,
    String level,
    String requestPath,
    String httpMethod,
    String userEmail,
    String userId,
    String ipAddress,
    String resource,
    String resourceId,
    String logger,
    String stackTrace,
    String rawDetails
) {}
