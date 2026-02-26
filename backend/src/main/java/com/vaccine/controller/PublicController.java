package com.vaccine.controller;

import com.vaccine.dto.CenterDTO;
import com.vaccine.dto.DriveDTO;
import com.vaccine.dto.SlotDTO;
import com.vaccine.service.CenterService;
import com.vaccine.service.DriveService;
import com.vaccine.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    
    @Autowired
    private DriveService driveService;
    
    @Autowired
    private CenterService centerService;
    
    @Autowired
    private SlotService slotService;
    
    // Drive endpoints
    @GetMapping("/drives")
    public ResponseEntity<List<DriveDTO>> getAllActiveDrives() {
        return ResponseEntity.ok(driveService.getAllActiveDrives());
    }
    
    @GetMapping("/drives/{id}")
    public ResponseEntity<DriveDTO> getDriveById(@PathVariable Long id) {
        return ResponseEntity.ok(driveService.getDriveById(id));
    }
    
    @GetMapping("/drives/{id}/slots")
    public ResponseEntity<List<SlotDTO>> getSlotsByDrive(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date != null) {
            return ResponseEntity.ok(slotService.getAvailableSlotsByDriveAndDate(id, date));
        }
        return ResponseEntity.ok(slotService.getSlotsByDrive(id));
    }
    
    @GetMapping("/drives/date-range")
    public ResponseEntity<List<DriveDTO>> getDrivesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(driveService.getDrivesByDateRange(startDate, endDate));
    }
    
    // Center endpoints
    @GetMapping("/centers")
    public ResponseEntity<List<CenterDTO>> getAllCenters() {
        return ResponseEntity.ok(centerService.getAllCenters());
    }
    
    @GetMapping("/centers/{id}")
    public ResponseEntity<CenterDTO> getCenterById(@PathVariable Long id) {
        return ResponseEntity.ok(centerService.getCenterById(id));
    }
    
    @GetMapping("/centers/city/{city}")
    public ResponseEntity<List<CenterDTO>> getCentersByCity(@PathVariable String city) {
        return ResponseEntity.ok(centerService.getCentersByCity(city));
    }
}
