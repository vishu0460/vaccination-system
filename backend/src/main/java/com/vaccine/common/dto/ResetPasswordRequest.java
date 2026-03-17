package com.vaccine.common.dto;

public record ResetPasswordRequest(
    String token,
    String password
) {}
