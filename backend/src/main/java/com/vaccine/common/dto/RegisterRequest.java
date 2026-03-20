package com.vaccine.common.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String fullName,
    @NotBlank @Size(min = 8) String password,
    @NotNull @Min(0) Integer age,
    String phoneNumber
) {}
