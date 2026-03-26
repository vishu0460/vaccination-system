package com.vaccine.common.dto;

import com.vaccine.domain.WaitlistEntry;

import java.time.LocalDateTime;

public record WaitlistEntryResponse(
    Long id,
    Long slotId,
    Long driveId,
    String driveName,
    String centerName,
    String centerCity,
    String status,
    LocalDateTime createdAt,
    LocalDateTime promotedAt
) {
    public static WaitlistEntryResponse from(WaitlistEntry entry) {
        return new WaitlistEntryResponse(
            entry.getId(),
            entry.getSlot() != null ? entry.getSlot().getId() : null,
            entry.getSlot() != null && entry.getSlot().getDrive() != null ? entry.getSlot().getDrive().getId() : null,
            entry.getSlot() != null && entry.getSlot().getDrive() != null ? entry.getSlot().getDrive().getTitle() : null,
            entry.getSlot() != null && entry.getSlot().getDrive() != null && entry.getSlot().getDrive().getCenter() != null
                ? entry.getSlot().getDrive().getCenter().getName() : null,
            entry.getSlot() != null && entry.getSlot().getDrive() != null && entry.getSlot().getDrive().getCenter() != null
                ? entry.getSlot().getDrive().getCenter().getCity() : null,
            entry.getStatus(),
            entry.getCreatedAt(),
            entry.getPromotedAt()
        );
    }
}
