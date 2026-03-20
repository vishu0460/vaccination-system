package com.vaccine.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record SlotRequest(
    Long driveId,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    LocalDateTime startTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    LocalDateTime endTime,
    Integer capacity
) {}
