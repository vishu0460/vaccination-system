package com.vaccine.web.controller;

import com.vaccine.common.dto.AdminManagementCreateRequest;
import com.vaccine.common.dto.AdminManagementUpdateRequest;
import com.vaccine.common.dto.ApiResponse;
import com.vaccine.core.service.AdminService;
import com.vaccine.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/admins", "/api/admins"})
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminManagementController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAdmins() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllAdmins()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createAdmin(
        @Valid @RequestBody AdminManagementCreateRequest req,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(adminService.createAdmin(req, request), "Admin created successfully"));
    }

    @PutMapping("/{adminId}")
    public ResponseEntity<ApiResponse<User>> updateAdmin(
        @PathVariable Long adminId,
        @Valid @RequestBody AdminManagementUpdateRequest req,
        HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateAdmin(adminId, req, request), "Admin updated successfully"));
    }

    @DeleteMapping("/{adminId}")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(@PathVariable Long adminId, HttpServletRequest request) {
        adminService.deleteAdmin(adminId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Admin deleted successfully"));
    }
}
