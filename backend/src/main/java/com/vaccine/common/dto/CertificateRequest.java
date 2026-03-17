package com.vaccine.common.dto;

public record CertificateRequest(
    Long bookingId,
    String vaccineName,
    Integer doseNumber
) {}
