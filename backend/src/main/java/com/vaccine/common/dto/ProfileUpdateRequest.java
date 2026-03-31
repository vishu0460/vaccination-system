package com.vaccine.common.dto;

import java.time.LocalDate;

public record ProfileUpdateRequest(
    String fullName,
    LocalDate dob,
    Integer age,
    String phone,
    String phoneNumber,
    String address,
    String city,
    String state,
    String pincode,
    String profileImage,
    String password
) {}
