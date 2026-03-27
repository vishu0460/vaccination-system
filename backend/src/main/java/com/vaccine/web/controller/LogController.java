package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.SystemLogEntryResponse;
import com.vaccine.core.service.LogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/logs", "/api/logs", "/admin/logs", "/api/admin/logs"})
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class LogController {
    private final LogService logService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemLogEntryResponse>>> getLogs(
        @RequestParam(required = false) String level,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "200") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(logService.getRecentLogs(level, search, limit)));
    }
}
