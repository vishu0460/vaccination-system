package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.ProfileUpdateRequest;
import com.vaccine.domain.User;
import com.vaccine.core.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@Validated
@RequestMapping({"/v1/profile", "/profile"})
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication auth) {
        log.info("Get profile for user: {}", auth.getName());
        User user = profileService.getProfile(auth.getName());
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("age", user.getAge());
        response.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        response.put("address", user.getAddress() != null ? user.getAddress() : "");
        response.put("city", user.getUserCity() != null ? user.getUserCity() : "");
        response.put("state", user.getUserState() != null ? user.getUserState() : "");
        response.put("pincode", user.getUserPincode() != null ? user.getUserPincode() : "");
        response.put("emailVerified", user.getEmailVerified());
        response.put("phoneVerified", user.getPhoneVerified() != null ? user.getPhoneVerified() : false);
        response.put("enabled", user.getEnabled());
        response.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication auth) {
        log.info("Update profile for user: {}", auth.getName());
        User user = profileService.updateProfile(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "message", "Profile updated successfully",
            "fullName", user.getFullName(),
            "age", user.getAge()
        )));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody Map<String, String> passwords,
            Authentication auth) {
        log.info("Change password for user: {}", auth.getName());
        if (!passwords.containsKey("currentPassword") || !passwords.containsKey("newPassword")) {
            throw new IllegalArgumentException("currentPassword and newPassword are required");
        }
        profileService.changePassword(
            auth.getName(),
            passwords.get("currentPassword"),
            passwords.get("newPassword")
        );
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(Authentication auth) {
        log.warn("Account deactivation requested for user: {}", auth.getName());
        profileService.deactivateAccount(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Account deactivated successfully"));
    }
}
