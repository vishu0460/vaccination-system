package com.vaccine.web.controller;

import com.vaccine.common.dto.ProfileUpdateRequest;
import com.vaccine.domain.User;
import com.vaccine.core.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        User user = profileService.getProfile(auth.getName());
        Map<String, Object> response = new java.util.HashMap<>();
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
        response.put("createdAt", user.getCreatedAt().toString());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication auth) {
        User user = profileService.updateProfile(auth.getName(), request);
        return ResponseEntity.ok(Map.of(
            "message", "Profile updated successfully",
            "fullName", user.getFullName(),
            "age", user.getAge()
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> passwords,
            Authentication auth) {
        profileService.changePassword(
            auth.getName(),
            passwords.get("currentPassword"),
            passwords.get("newPassword")
        );
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivateAccount(Authentication auth) {
        profileService.deactivateAccount(auth.getName());
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully"));
    }
}
