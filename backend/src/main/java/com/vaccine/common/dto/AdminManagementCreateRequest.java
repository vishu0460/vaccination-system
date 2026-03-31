package com.vaccine.common.dto;

import com.vaccine.domain.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminManagementCreateRequest(
    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must be 255 characters or fewer")
    String fullName,

    @Email(message = "Enter a valid email address")
    @NotBlank(message = "Email is required")
    String email,

    @Size(max = 20, message = "Phone number must be 20 characters or fewer")
    String phoneNumber,

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
        message = "Password must include uppercase, lowercase, number, and special character"
    )
    String password,

    @NotNull(message = "Role is required")
    RoleName role,

    @NotNull(message = "Status is required")
    Boolean enabled,

    @Size(max = 500, message = "Address must be 500 characters or fewer")
    String address,

    String profileImage
) {}
