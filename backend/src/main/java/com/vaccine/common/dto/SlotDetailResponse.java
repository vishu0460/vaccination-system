package com.vaccine.common.dto;

import com.vaccine.domain.Slot;
import com.vaccine.util.SlotStatusResolver;

import java.time.LocalDateTime;

public record SlotDetailResponse(
    Long id,
    Long driveId,
    String driveName,
    Long centerId,
    String centerName,
    String centerCity,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    Integer capacity,
    Integer bookedCount,
    Integer remaining,
    Long waitlistCount,
    Boolean almostFull,
    String demandLevel,
    Boolean available,
    Boolean bookable,
    String availability,
    String slotStatus
) {
    public static SlotDetailResponse from(Slot slot) {
        LocalDateTime startDate = slot.getStartDateTime();
        LocalDateTime endDate = SlotStatusResolver.resolveEnd(slot);
        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
        int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
        int remaining = Math.max(0, capacity - bookedCount);
        boolean available = remaining > 0;
        boolean bookable = available && SlotStatusResolver.resolve(slot) != com.vaccine.domain.SlotStatus.EXPIRED;
        double fillRate = capacity <= 0 ? 0d : (double) bookedCount / capacity;
        boolean almostFull = capacity > 0 && remaining <= Math.max(1, Math.ceil(capacity * 0.2));
        String demandLevel = fillRate >= 0.85 ? "HIGH_DEMAND" : almostFull ? "ALMOST_FULL" : "NORMAL";

        return new SlotDetailResponse(
            slot.getId(),
            slot.getDrive() != null ? slot.getDrive().getId() : null,
            slot.getDrive() != null ? slot.getDrive().getTitle() : null,
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getId() : null,
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getName() : null,
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getCity() : null,
            startDate,
            endDate,
            startDate,
            endDate,
            capacity,
            bookedCount,
            remaining,
            0L,
            almostFull,
            demandLevel,
            available,
            bookable,
            available ? "AVAILABLE" : "FULL",
            SlotStatusResolver.resolve(slot).name()
        );
    }
}
