package com.vaccine.controller;

import com.vaccine.dto.DriveDTO;
import com.vaccine.service.DriveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/drives")
public class DriveController {
    
    @Autowired
    private DriveService driveService;
    
    @GetMapping
    public ResponseEntity<List<DriveDTO>> getAllDrives() {
        return ResponseEntity.ok(driveService.getAllDrives());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DriveDTO> getDriveById(@PathVariable Long id) {
        return ResponseEntity.ok(driveService.getDriveById(id));
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<DriveDTO>> getActiveDrives() {
        return ResponseEntity.ok(driveService.getActiveDrives());
    }
    
    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<DriveDTO>> getDrivesByCenter(@PathVariable Long centerId) {
        return ResponseEntity.ok(driveService.getDrivesByCenter(centerId));
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<DriveDTO>> getDrivesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(driveService.getDrivesByDateRange(startDate, endDate));
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriveDTO> createDrive(@RequestBody DriveDTO driveDTO) {
        return ResponseEntity.ok(driveService.createDrive(driveDTO));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriveDTO> updateDrive(@PathVariable Long id, @RequestBody DriveDTO driveDTO) {
        return ResponseEntity.ok(driveService.updateDrive(id, driveDTO));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDrive(@PathVariable Long id) {
        driveService.deleteDrive(id);
        return ResponseEntity.ok().build();
    }
}
