package com.vaccine.common.dto;

import jakarta.validation.constraints.*;

public record ForgotPasswordRequest(
    @NotBlank @Email String email
) {}
