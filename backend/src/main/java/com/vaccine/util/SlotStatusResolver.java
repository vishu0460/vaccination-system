package com.vaccine.util;

import com.vaccine.domain.Slot;
import com.vaccine.domain.SlotStatus;

import java.time.LocalDateTime;

public final class SlotStatusResolver {

    private SlotStatusResolver() {
    }

    public static SlotStatus resolve(Slot slot) {
        return resolve(slot, LocalDateTime.now());
    }

    public static SlotStatus resolve(Slot slot, LocalDateTime now) {
        LocalDateTime start = slot.getDateTime();
        LocalDateTime end = resolveEnd(slot);

        if (start == null) {
            return SlotStatus.EXPIRED;
        }

        if (end != null && now.isAfter(end)) {
            return SlotStatus.EXPIRED;
        }

        if ((now.isEqual(start) || now.isAfter(start)) && (end == null || now.isBefore(end) || now.isEqual(end))) {
            return SlotStatus.LIVE;
        }

        return SlotStatus.UPCOMING;
    }

    public static LocalDateTime resolveEnd(Slot slot) {
        if (slot == null || slot.getDateTime() == null) {
            return null;
        }

        if (slot.getEndTime() != null) {
            return slot.getDateTime().toLocalDate().atTime(slot.getEndTime());
        }

        return slot.getDateTime();
    }
}
