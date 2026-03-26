package com.vaccine.common.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @Email(message = "Enter a valid email address")
    String email,

    @Pattern(regexp = "^\\d{7}$", message = "OTP must be 7 digits")
    String otp,

    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword,

    String token,

    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {
    @AssertTrue(message = "Provide either email, otp, and newPassword, or token and password")
    public boolean isValidPayload() {
        boolean emailOtpFlow = hasText(email) && hasText(otp) && hasText(newPassword);
        boolean legacyTokenFlow = hasText(token) && hasText(password);
        return emailOtpFlow || legacyTokenFlow;
    }

    public String resolvedPassword() {
        return hasText(newPassword) ? newPassword : password;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
