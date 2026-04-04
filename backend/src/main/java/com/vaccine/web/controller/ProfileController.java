package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiMessage;
import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.ChangePasswordRequest;
import com.vaccine.common.dto.ProfileUpdateRequest;
import com.vaccine.common.exception.AppException;
import com.vaccine.domain.User;
import com.vaccine.core.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping({"/v1/profile", "/profile", "/api/v1/profile", "/api/profile", "/users", "/api/users"})
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Value("${app.dev.disable-auth-for-profile:false}")
    private boolean disableAuthForProfile;

    @GetMapping({"", "/me", "/profile"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication auth, HttpServletRequest request) {
        String principal = auth != null ? auth.getName() : "anonymous";
        log.info("Profile request path={} principal={} authPresent={} debugBypass={}",
            request.getRequestURI(), principal, auth != null, disableAuthForProfile);

        User user;
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
            user = profileService.getProfile(auth.getName());
        } else if (disableAuthForProfile) {
            user = profileService.getFirstEnabledUserForDebug();
        } else {
            throw new AppException("Authentication required for profile access");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("role", user.getEffectiveRole());
        response.put("dob", user.getDob() != null ? user.getDob().toString() : "");
        response.put("age", user.getAge());
        response.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        response.put("address", user.getAddress() != null ? user.getAddress() : "");
        response.put("profileImage", user.getProfileImage() != null ? user.getProfileImage() : "");
        response.put("city", user.getUserCity() != null ? user.getUserCity() : "");
        response.put("state", user.getUserState() != null ? user.getUserState() : "");
        response.put("pincode", user.getUserPincode() != null ? user.getUserPincode() : "");
        response.put("emailVerified", user.getEmailVerified());
        response.put("phoneVerified", user.getPhoneVerified() != null ? user.getPhoneVerified() : false);
        response.put("enabled", user.getEnabled());
        response.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        log.info("Profile response resolved userId={} email={}", user.getId(), user.getEmail());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping({"", "/update-profile"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication auth) {
        User user = profileService.updateProfile(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "message", "Profile updated successfully",
            "fullName", user.getFullName(),
            "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
            "address", user.getAddress() != null ? user.getAddress() : "",
            "dob", user.getDob() != null ? user.getDob().toString() : "",
            "age", user.getAge(),
            "profileImage", user.getProfileImage() != null ? user.getProfileImage() : ""
        )));
    }

    @PostMapping("/change-password/request-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiMessage> requestPasswordChangeOtp(Authentication auth) {
        profileService.sendPasswordChangeOtp(auth.getName());
        return ResponseEntity.ok(new ApiMessage("OTP sent to your email for verification."));
    }

    @RequestMapping(value = "/change-password", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth) {
        profileService.changePassword(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @PostMapping("/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(Authentication auth) {
        profileService.deactivateAccount(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Account deactivated successfully"));
    }
}
