package com.vaccine.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record Verify2FARequest(
    @NotBlank @Email String email,
    @NotBlank String twoFactorCode
) {}
