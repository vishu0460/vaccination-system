package com.vaccine.core.service;

import com.vaccine.common.dto.DriveResponse;
import com.vaccine.domain.Slot;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SlotService {
    private final SlotRepository slotRepository;
    private final VaccinationDriveRepository driveRepository;

    public List<Slot> getAllActiveSlots() {
        return slotRepository.findByDriveIdOrderByStartTimeAsc(1L);  // Temp fix; add proper query if needed
    }

    public long countSlots() {
        return slotRepository.count();
    }

    public List<DriveResponse.SlotResponse> getSlotsForDrive(Long driveId) {
        var drive = driveRepository.findById(driveId).orElseThrow();
        LocalDate driveDate = drive.getDriveDate();
        return slotRepository.findByDriveIdOrderByStartTimeAsc(driveId)
                .stream()
                .map(slot -> new DriveResponse.SlotResponse(
                    slot.getId(), 
                    slot.getStartTime().atDate(driveDate), 
                    slot.getEndTime().atDate(driveDate), 
                    slot.getCapacity(), 
                    slot.getBookedCount()))
                .collect(Collectors.toList());
    }
}

