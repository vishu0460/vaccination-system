package com.vaccine.common.dto;

public record CenterRequest(
    String name,
    String address,
    String city,
    String state,
    String pincode,
    String phone,
    String email,
    String workingHours,
    Integer dailyCapacity
) {}
