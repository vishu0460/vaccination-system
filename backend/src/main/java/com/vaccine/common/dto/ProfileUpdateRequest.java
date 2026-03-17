package com.vaccine.common.dto;

public record ProfileUpdateRequest(
    String fullName,
    String phoneNumber,
    String address,
    String city,
    String state,
    String pincode,
    String password
) {}
