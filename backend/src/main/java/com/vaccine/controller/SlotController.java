package com.vaccine.controller;

import com.vaccine.dto.SlotDTO;
import com.vaccine.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotController {
    
    @Autowired
    private SlotService slotService;
    
    @GetMapping("/drive/{driveId}")
    public ResponseEntity<List<SlotDTO>> getSlotsByDrive(@PathVariable Long driveId) {
        return ResponseEntity.ok(slotService.getSlotsByDrive(driveId));
    }
    
    @GetMapping("/drive/{driveId}/date/{date}")
    public ResponseEntity<List<SlotDTO>> getAvailableSlotsByDriveAndDate(
            @PathVariable Long driveId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(slotService.getAvailableSlotsByDriveAndDate(driveId, date));
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<SlotDTO>> getAvailableSlotsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(slotService.getAvailableSlotsBetweenDates(startDate, endDate));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SlotDTO> getSlotById(@PathVariable Long id) {
        return ResponseEntity.ok(slotService.getSlotById(id));
    }
}
