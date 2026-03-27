package com.vaccine.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CenterRequest(
    @NotBlank(message = "Center name is required")
    @Size(max = 255, message = "Center name must be 255 characters or fewer")
    String name,

    @NotBlank(message = "Address is required")
    @Size(max = 2000, message = "Address must be 2000 characters or fewer")
    String address,

    @Size(max = 100, message = "City must be 100 characters or fewer")
    String city,

    @Size(max = 100, message = "State must be 100 characters or fewer")
    String state,

    @Size(max = 20, message = "Pincode must be 20 characters or fewer")
    String pincode,

    @Size(max = 20, message = "Phone must be 20 characters or fewer")
    String phone,

    @Email(message = "Enter a valid email address")
    @Size(max = 255, message = "Email must be 255 characters or fewer")
    String email,

    @Size(max = 255, message = "Working hours must be 255 characters or fewer")
    String workingHours,

    @Min(value = 1, message = "Daily capacity must be at least 1")
    Integer dailyCapacity
) {}
