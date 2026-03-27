package com.vaccine.common.dto;

public record RegisterResponse(
    String message,
    int status,
    boolean requiresVerification,
    boolean emailDeliveryFailed,
    String verificationEmail,
    String otpPreview,
    boolean otpSent,
    String fallbackOtp,
    String devOtp,
    long otpExpiresInSeconds
) {}
