package com.vaccine.common.dto;

import java.util.List;

public record SystemLogPageResponse(
    List<SystemLogEntryResponse> content,
    long totalElements,
    int totalPages,
    int page,
    int size,
    boolean last
) {}
