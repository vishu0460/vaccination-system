package com.vaccine.web.controller;

import com.vaccine.common.dto.SummaryResponse;
import com.vaccine.domain.Slot;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.core.service.PublicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {
    
    private final PublicService publicService;

    @GetMapping("/centers")
    public ResponseEntity<Map<String, Object>> getCenters(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(publicService.getCenters(city, page, size));
    }

    @GetMapping("/drives")
    public ResponseEntity<Map<String, Object>> getDrives(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) java.time.LocalDate fromDate,
            @RequestParam(required = false) Integer age,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(publicService.getDrives(city, fromDate, age, page, size));
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary() {
        return ResponseEntity.ok(publicService.getSummary());
    }

    @GetMapping("/centers/{id}")
    public ResponseEntity<VaccinationCenter> getCenterDetail(@PathVariable Long id) {
        Optional<VaccinationCenter> center = publicService.getCenterDetail(id);
        return center.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/drives/{driveId}/slots")
    public ResponseEntity<List<Slot>> getDriveSlots(@PathVariable Long driveId) {
        return ResponseEntity.ok(publicService.getDriveSlots(driveId));
    }
}
