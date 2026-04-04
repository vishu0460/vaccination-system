package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record DownloadHistoryResponse(
    Long id,
    Long certificateId,
    String certificateNumber,
    String downloadType,
    LocalDateTime timestamp
) {}
