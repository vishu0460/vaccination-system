package com.vaccine.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;

public record UserUpdateRequest(
    @Email String email,
    String fullName,
    @Min(0) Integer age,
    String phoneNumber,
    Boolean enabled
) {}
