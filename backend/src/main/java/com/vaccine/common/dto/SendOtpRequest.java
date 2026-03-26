package com.vaccine.common.dto;

import com.vaccine.domain.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendOtpRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    String email,

    @NotNull(message = "OTP purpose is required")
    OtpPurpose purpose
) {}
