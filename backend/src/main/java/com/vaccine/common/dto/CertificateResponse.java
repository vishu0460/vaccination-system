package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record CertificateResponse(
    Long id,
    String certificateNumber,
    String vaccineName,
    Integer doseNumber,
    LocalDateTime nextDoseDate,
    String qrCode,
    LocalDateTime issuedAt,
    String userName,
    String userEmail,
    String centerName,
    String driveTitle,
    String slotTime,
    String digitalVerificationCode
) {}
