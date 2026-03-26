package com.vaccine.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    String email,

    @JsonAlias("name")
    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 80, message = "Full name must be between 3 and 80 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]{2,79}$", message = "Full name contains invalid characters")
    String fullName,

    @JsonAlias("phone")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Enter a valid phone number")
    String phoneNumber,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
        message = "Password must include uppercase, lowercase, number, and special character"
    )
    String password,

    @Min(value = 1, message = "Age must be at least 1")
    Integer age
) {}
