package com.vaccine.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CertificateResponse(
    Long id,
    Long bookingId,
    String certificateNumber,
    String vaccineName,
    Integer doseNumber,
    String doseLabel,
    LocalDateTime vaccinationDate,
    LocalDateTime nextDoseDate,
    String qrCode,
    String qrCodeData,
    String verificationUrl,
    LocalDateTime issuedAt,
    Long userId,
    String uniqueId,
    String userFullName,
    String userName,
    String gender,
    LocalDate dateOfBirth,
    String userEmail,
    String centerName,
    String centerAddress,
    String location,
    String driveTitle,
    LocalDateTime slotDateTime,
    String slotTime,
    String digitalVerificationCode,
    String verifiedBy
) {}
