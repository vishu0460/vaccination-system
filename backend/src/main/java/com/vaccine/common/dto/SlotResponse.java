package com.vaccine.common.dto;

import java.time.LocalDateTime;

public record SlotResponse(Long id, LocalDateTime startTime, LocalDateTime endTime, int capacity, int bookedCount, int remaining) {
}

