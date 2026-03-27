package com.vaccine.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
  @NotBlank(message = "Subject is required")
  @Size(max = 255, message = "Subject must be 255 characters or fewer")
  String subject,

  @NotBlank(message = "Message is required")
  @Size(max = 5000, message = "Message must be 5000 characters or fewer")
  String message, 

  @Min(value = 1, message = "Rating must be between 1 and 5")
  @Max(value = 5, message = "Rating must be between 1 and 5")
  Integer rating
) {}
