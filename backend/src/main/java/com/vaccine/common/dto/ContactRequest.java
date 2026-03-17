package com.vaccine.common.dto;

public record ContactRequest(
    String name,
    String email,
    String phone,
    String subject,
    String message
) {}
