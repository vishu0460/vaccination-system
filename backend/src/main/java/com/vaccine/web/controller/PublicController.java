package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.NearbyCentersResponse;
import com.vaccine.common.dto.SlotDetailResponse;
import com.vaccine.common.dto.SmartSearchResponse;
import com.vaccine.common.dto.SummaryResponse;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.core.service.PublicService;
import com.vaccine.core.service.SearchService;
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
@RequestMapping({"/v1/public", "/public"})
public class PublicController {
    
    private final PublicService publicService;
    private final SearchService searchService;

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
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) String vaccineType,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String slot,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Public drives query: city={}, date={}, age={}, vaccineType={}, available={}, slot={}, page={}, size={}",
            city, date, age, vaccineType, available, slot, page, size);
        Map<String, Object> drivesData = publicService.getDrives(city, date, age, vaccineType, available, slot, page, size);
        return ResponseEntity.ok(ApiResponse.success(drivesData));
    }

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<String>>> getCitySuggestions(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "8") int limit) {
        log.info("Public city suggestions query: query={}, limit={}", query, limit);
        return ResponseEntity.ok(ApiResponse.success(searchService.getCitySuggestions(query, Math.max(1, Math.min(limit, 20)))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SmartSearchResponse>> smartSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "6") int limit) {
        log.info("Public smart search query: query={}, city={}, limit={}", query, city, limit);
        return ResponseEntity.ok(ApiResponse.success(searchService.search(query, city, Math.max(1, Math.min(limit, 10)))));
    }

    @GetMapping("/nearby-centers")
    public ResponseEntity<ApiResponse<NearbyCentersResponse>> nearbyCenters(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "6") int limit) {
        log.info("Public nearby centers query: lat={}, lng={}, limit={}", lat, lng, limit);
        return ResponseEntity.ok(ApiResponse.success(searchService.findNearbyCenters(lat, lng, Math.max(1, Math.min(limit, 10)))));
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
    public ResponseEntity<ApiResponse<List<SlotDetailResponse>>> getDriveSlots(@PathVariable Long driveId) {
        log.info("Public drive slots ID={}", driveId);
        List<SlotDetailResponse> slots = publicService.getDriveSlots(driveId);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }
}
