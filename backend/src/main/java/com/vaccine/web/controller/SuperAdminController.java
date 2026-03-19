package com.vaccine.web.controller;

import com.vaccine.common.dto.AdminCreateRequest;
import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.CenterRequest;
import com.vaccine.domain.RoleName;
import com.vaccine.domain.User;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.core.service.AdminService;
import com.vaccine.core.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@Validated
@RequestMapping("/api/v1/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {
    private final AdminService adminService;
    private final AuditService auditService;

    public SuperAdminController(AdminService adminService, AuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    @PostMapping("/admins")
    public ResponseEntity<ApiResponse<Void>> createAdmin(@Valid @RequestBody AdminCreateRequest req, HttpServletRequest request) {
        log.info("Create admin for email={}", req.email());
        adminService.createAdmin(req, request);
        auditService.log(req.email(), "CREATE_ADMIN", "USER", "Admin created by super admin", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "Admin created successfully"));
    }

    @GetMapping("/admins")
    public ResponseEntity<ApiResponse<List<User>>> allAdmins() {
        log.info("Get all admins");
        List<User> admins = adminService.getAllAdmins();
        return ResponseEntity.ok(ApiResponse.success(admins));
    }

    @DeleteMapping("/admins/{adminId}")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(@PathVariable Long adminId, HttpServletRequest request) {
        log.info("Delete admin ID={}", adminId);
        adminService.deleteAdmin(adminId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Admin deleted successfully"));
    }

    @GetMapping("/centers")
    public ResponseEntity<ApiResponse<List<VaccinationCenter>>> allCenters() {
        log.info("Get all centers");
        List<VaccinationCenter> centers = adminService.getAllCenters();
        return ResponseEntity.ok(ApiResponse.success(centers));
    }

    @PostMapping("/centers")
    public ResponseEntity<ApiResponse<VaccinationCenter>> createCenter(@Valid @RequestBody CenterRequest req) {
        log.info("Create center={}", req.name());
        VaccinationCenter center = adminService.createCenter(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(center, "Center created"));
    }

    @DeleteMapping("/centers/{centerId}")
    public ResponseEntity<ApiResponse<Void>> deleteCenter(@PathVariable Long centerId, HttpServletRequest request) {
        log.info("Delete center ID={}", centerId);
        adminService.deleteCenter(centerId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Center deleted successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> allUsers() {
        log.info("Get all users");
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(@PathVariable Long userId, 
                                                      @RequestParam RoleName role,
                                                      HttpServletRequest request) {
        log.info("Update user ID={} role={}", userId, role);
        adminService.updateUserRole(userId, role, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Role updated successfully"));
    }
}
