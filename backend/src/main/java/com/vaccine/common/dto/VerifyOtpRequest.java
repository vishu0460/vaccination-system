package com.vaccine.common.dto;

import com.vaccine.domain.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    String email,

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{7}$", message = "OTP must be 7 digits")
    String otp,

    @NotNull(message = "OTP purpose is required")
    OtpPurpose purpose
) {}
