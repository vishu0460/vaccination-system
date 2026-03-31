package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.LogFeedResponse;
import com.vaccine.common.dto.SystemLogEntryResponse;
import com.vaccine.common.dto.SystemLogPageResponse;
import com.vaccine.core.service.LogService;
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
    public ResponseEntity<ApiResponse<?>> getLogs(
        @RequestParam(required = false) String level,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(defaultValue = "20") int limit
    ) {
        if (page != null || size != null) {
            int resolvedPage = page == null ? 0 : page;
            int resolvedSize = size == null ? limit : size;
            SystemLogPageResponse pagedLogs = logService.getRecentLogsPage(level, search, resolvedPage, resolvedSize);
            return ResponseEntity.ok(ApiResponse.success(pagedLogs));
        }

        return ResponseEntity.ok(ApiResponse.success(logService.getRecentLogs(level, search, limit)));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<LogFeedResponse>> getActivityLogs(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String user,
        @RequestParam(required = false) String actionType,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(logService.getActivityLogs(search, user, actionType, startDate, endDate, page, size)));
    }

    @GetMapping("/security")
    public ResponseEntity<ApiResponse<LogFeedResponse>> getSecurityLogs(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String user,
        @RequestParam(required = false) String actionType,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(logService.getSecurityLogs(search, user, actionType, startDate, endDate, page, size)));
    }
}
