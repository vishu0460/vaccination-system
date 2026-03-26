package com.vaccine.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    String newPassword,

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{7}$", message = "OTP must be 7 digits")
    String otp
) {}
