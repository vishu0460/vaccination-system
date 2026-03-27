package com.vaccine.common.dto;

public record OtpDeliveryResponse(
    boolean success,
    String message,
    String email,
    boolean otpSent,
    String fallbackOtp,
    String devOtp,
    String target,
    String channel,
    long otpExpiresInSeconds
) {}
