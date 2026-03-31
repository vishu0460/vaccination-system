package com.vaccine.util;

import com.vaccine.domain.Slot;
import com.vaccine.domain.SlotDisplayStatus;
import com.vaccine.domain.SlotStatus;

import java.time.LocalDateTime;

public final class SlotStatusResolver {

    private SlotStatusResolver() {
    }

    public static SlotStatus resolve(Slot slot) {
        return resolve(slot, LocalDateTime.now());
    }

    public static SlotStatus resolve(Slot slot, LocalDateTime now) {
        SlotDisplayStatus displayStatus = resolveDisplayStatus(slot, now);
        if (displayStatus == SlotDisplayStatus.EXPIRED) {
            return SlotStatus.EXPIRED;
        }
        if (displayStatus == SlotDisplayStatus.UPCOMING) {
            return SlotStatus.UPCOMING;
        }
        return SlotStatus.LIVE;
    }

    public static SlotDisplayStatus resolveDisplayStatus(Slot slot) {
        return resolveDisplayStatus(slot, LocalDateTime.now());
    }

    public static SlotDisplayStatus resolveDisplayStatus(Slot slot, LocalDateTime now) {
        LocalDateTime start = slot != null ? slot.getStartDateTime() : null;
        LocalDateTime end = resolveEnd(slot);

        if (start == null) {
            return SlotDisplayStatus.EXPIRED;
        }

        if (end != null && now.isAfter(end)) {
            return SlotDisplayStatus.EXPIRED;
        }

        int availableSlots = slot == null || slot.getAvailableSlots() == null ? 0 : slot.getAvailableSlots();
        if (availableSlots <= 0) {
            return SlotDisplayStatus.FULL;
        }

        if ((now.isEqual(start) || now.isAfter(start)) && (end == null || now.isBefore(end) || now.isEqual(end))) {
            return SlotDisplayStatus.ACTIVE;
        }

        return SlotDisplayStatus.UPCOMING;
    }

    public static LocalDateTime resolveEnd(Slot slot) {
        return slot != null ? slot.getEndDateTime() : null;
    }
}
