package com.vaccine.web.controller;

import com.vaccine.common.dto.AdminCreateRequest;
import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.CenterRequest;
import com.vaccine.common.dto.DriveRequest;
import com.vaccine.common.dto.SlotRequest;
import com.vaccine.common.dto.UserUpdateRequest;
import com.vaccine.domain.RoleName;
import com.vaccine.domain.Slot;
import com.vaccine.domain.User;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
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
import java.util.Map;


@Slf4j
@RestController
@Validated
@RequestMapping({"/super-admin", "/api/super-admin"})
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
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

    @PutMapping({"/users/{userId}", "/user/{userId}"})
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateUser(userId, req), "User updated successfully"));
    }

    @DeleteMapping({"/users/{userId}", "/user/{userId}"})
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId, HttpServletRequest request) {
        adminService.deleteUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @PutMapping({"/centers/{centerId}", "/center/{centerId}"})
    public ResponseEntity<ApiResponse<VaccinationCenter>> updateCenter(@PathVariable Long centerId, @Valid @RequestBody CenterRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateCenter(centerId, req), "Center updated successfully"));
    }

    @DeleteMapping({"/centers/{centerId}", "/center/{centerId}"})
    public ResponseEntity<ApiResponse<Void>> deleteCenterAny(@PathVariable Long centerId, HttpServletRequest request) {
        adminService.deleteCenter(centerId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Center deleted successfully"));
    }

    @PutMapping({"/drives/{driveId}", "/drive/{driveId}"})
    public ResponseEntity<ApiResponse<VaccinationDrive>> updateDrive(@PathVariable Long driveId, @Valid @RequestBody DriveRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateDrive(driveId, req), "Drive updated successfully"));
    }

    @DeleteMapping({"/drives/{driveId}", "/drive/{driveId}"})
    public ResponseEntity<ApiResponse<Void>> deleteDrive(@PathVariable Long driveId, HttpServletRequest request) {
        adminService.deleteDrive(driveId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Drive deleted successfully"));
    }

    @PutMapping({"/slots/{slotId}", "/slot/{slotId}"})
    public ResponseEntity<ApiResponse<Slot>> updateSlot(@PathVariable Long slotId, @Valid @RequestBody SlotRequest req) {
        log.info("Super admin update slot id={} driveId={} startDate={} endDate={}", slotId, req.getDriveId(), req.getStartDate(), req.getEndDate());
        return ResponseEntity.ok(ApiResponse.success(adminService.updateSlot(slotId, req), "Slot updated successfully"));
    }

    @GetMapping("/drives/{driveId}/slots")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriveSlots(@PathVariable Long driveId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDriveSlots(driveId)));
    }

    @DeleteMapping({"/slots/{slotId}", "/slot/{slotId}"})
    public ResponseEntity<ApiResponse<Void>> deleteSlot(@PathVariable Long slotId, HttpServletRequest request) {
        adminService.deleteSlot(slotId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Slot deleted successfully"));
    }
}
