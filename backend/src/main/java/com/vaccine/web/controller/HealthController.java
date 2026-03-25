package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/v1/health", "/health"})
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        log.debug("Health check requested");
        Map<String, Object> healthInfo = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().toString(),
            "service", "Vaccination System API",
            "version", "1.0.0"
        );
        return ResponseEntity.ok(ApiResponse.success(healthInfo));
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        log.trace("Ping endpoint called");
        return ResponseEntity.ok(ApiResponse.success("pong"));
    }
}
