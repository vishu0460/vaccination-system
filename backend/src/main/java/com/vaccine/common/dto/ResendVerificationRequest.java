package com.vaccine.common.dto;

import jakarta.validation.constraints.*;

public record ResendVerificationRequest(
    @NotBlank @Email String email
) {}
