package com.vaccine.common.dto;

public record VerifyCertificateResponse(
    boolean valid,
    String status,
    CertificateResponse certificate
) {}
