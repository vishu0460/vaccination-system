package com.vaccine.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be 255 characters or fewer")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 255, message = "Email must be 255 characters or fewer")
    String email,

    @Size(max = 20, message = "Phone must be 20 characters or fewer")
    String phone,

    @Size(max = 255, message = "Subject must be 255 characters or fewer")
    String subject,

    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message must be 5000 characters or fewer")
    String message
) {}
