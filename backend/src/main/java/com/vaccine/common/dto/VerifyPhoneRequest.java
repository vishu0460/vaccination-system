package com.vaccine.common.dto;

import jakarta.validation.constraints.*;

public record VerifyPhoneRequest(
    @NotBlank String phoneNumber,
    @NotBlank String otpCode
) {}
