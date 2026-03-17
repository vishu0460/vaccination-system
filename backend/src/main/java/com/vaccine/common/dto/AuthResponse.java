package com.vaccine.common.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    String email,
    String role,
    Boolean requiresTwoFactor
) {
    public AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, String email, String role) {
        this(accessToken, refreshToken, tokenType, expiresIn, email, role, false);
    }
}
