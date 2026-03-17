package com.vaccine.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPhoneRequest(
    @NotBlank String phoneNumber,
    @NotBlank @Pattern(regexp = "\\d{6}") String otpCode
) {}
