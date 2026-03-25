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
    LocalDateTime startDate,
    LocalDateTime endDate,
    Integer capacity,
    Integer bookedCount,
    Integer remaining,
    String availability,
    String slotStatus
) {
    public static SlotDetailResponse from(Slot slot) {
        LocalDateTime startDate = slot.getDateTime();
        LocalDateTime endDate = SlotStatusResolver.resolveEnd(slot);
        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
        int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();

        return new SlotDetailResponse(
            slot.getId(),
            slot.getDrive() != null ? slot.getDrive().getId() : null,
            slot.getDrive() != null ? slot.getDrive().getTitle() : null,
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getId() : null,
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getName() : null,
            startDate,
            endDate,
            capacity,
            bookedCount,
            Math.max(0, capacity - bookedCount),
            Math.max(0, capacity - bookedCount) > 0 ? "AVAILABLE" : "FULL",
            SlotStatusResolver.resolve(slot).name()
        );
    }
}
