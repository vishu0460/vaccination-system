package com.vaccine.common.dto;

import jakarta.validation.constraints.NotBlank;

public record CertificateDownloadRequest(
    @NotBlank String downloadType
) {}
