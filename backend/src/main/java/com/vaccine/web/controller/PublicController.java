package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.SummaryResponse;
import com.vaccine.domain.Slot;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.core.service.PublicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping({"/api/v1/public", "/api/public"})
public class PublicController {
    
    private final PublicService publicService;

    @GetMapping("/centers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCenters(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Public centers query: city={}, page={}, size={}", city, page, size);
        Map<String, Object> centersData = publicService.getCenters(city, page, size);
        return ResponseEntity.ok(ApiResponse.success(centersData));
    }

    @GetMapping("/drives")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDrives(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) Integer age,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Public drives query: city={}, fromDate={}, age={}, page={}, size={}", city, fromDate, age, page, size);
        Map<String, Object> drivesData = publicService.getDrives(city, fromDate, age, page, size);
        return ResponseEntity.ok(ApiResponse.success(drivesData));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary() {
        log.info("Public summary requested");
        SummaryResponse summary = publicService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/centers/{id}")
    public ResponseEntity<ApiResponse<VaccinationCenter>> getCenterDetail(@PathVariable Long id) {
        log.info("Public center detail ID={}", id);
        Optional<VaccinationCenter> centerOpt = publicService.getCenterDetail(id);
        if (centerOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(centerOpt.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/drives/{driveId}/slots")
    public ResponseEntity<ApiResponse<List<Slot>>> getDriveSlots(@PathVariable Long driveId) {
        log.info("Public drive slots ID={}", driveId);
        List<Slot> slots = publicService.getDriveSlots(driveId);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }
}
