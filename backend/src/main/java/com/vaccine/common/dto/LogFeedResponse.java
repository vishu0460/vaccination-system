package com.vaccine.common.dto;

import java.util.List;

public record LogFeedResponse(
    List<LogFeedEntryResponse> content,
    long totalElements,
    int page,
    int size,
    boolean hasMore
) {}
