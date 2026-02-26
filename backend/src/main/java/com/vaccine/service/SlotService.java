package com.vaccine.service;

import com.vaccine.dto.SlotDTO;
import com.vaccine.entity.Slot;
import com.vaccine.repository.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SlotService {
    
    @Autowired
    private SlotRepository slotRepository;
    
    public List<SlotDTO> getSlotsByDrive(Long driveId) {
        return slotRepository.findByDriveId(driveId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<SlotDTO> getAvailableSlotsByDriveAndDate(Long driveId, LocalDate date) {
        return slotRepository.findAvailableSlotsByDriveAndDate(driveId, date).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<SlotDTO> getAvailableSlotsBetweenDates(LocalDate startDate, LocalDate endDate) {
        return slotRepository.findAvailableSlotsBetweenDates(startDate, endDate).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public SlotDTO getSlotById(Long id) {
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        return mapToDTO(slot);
    }
    
    private SlotDTO mapToDTO(Slot slot) {
        SlotDTO dto = new SlotDTO();
        dto.setId(slot.getId());
        dto.setDriveId(slot.getDrive().getId());
        dto.setDriveName(slot.getDrive().getName());
        dto.setVaccineName(slot.getDrive().getVaccineName());
        dto.setSlotDate(slot.getSlotDate());
        dto.setSlotTime(slot.getSlotTime());
        dto.setTotalCapacity(slot.getTotalCapacity());
        dto.setAvailableCapacity(slot.getAvailableCapacity());
        return dto;
    }
}
