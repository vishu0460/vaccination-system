package com.vaccine.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record UserUpdateRequest(
    @Email String email,
    String fullName,
    LocalDate dob,
    @Min(0) Integer age,
    String phoneNumber,
    Boolean enabled
) {}
