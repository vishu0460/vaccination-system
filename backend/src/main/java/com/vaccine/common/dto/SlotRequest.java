package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record SlotRequest(
    Long driveId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer capacity
) {}

