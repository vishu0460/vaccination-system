package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record CertificateResponse(
    Long id,
    String certificateNumber,
    String vaccineType,
    LocalDateTime issued
