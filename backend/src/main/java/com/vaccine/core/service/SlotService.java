package com.vaccine.core.service;

import com.vaccine.dto.DriveResponse;
import com.vaccine.core.model.Slot;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SlotService {
    private final SlotRepository slotRepository;

    public SlotService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public List<Slot> getAllActiveSlots() {
        return slotRepository.findByDrive_ActiveTrue();
    }

    public long countSlots() {
        return slotRepository.count();
    }

    public List<DriveResponse.SlotResponse> getSlotsForDrive(Long driveId) {
        return slotRepository.findByDriveId(driveId)
                .stream()
                .map(DriveResponse.SlotResponse::from)
                .collect(Collectors.toList());
    }
}
