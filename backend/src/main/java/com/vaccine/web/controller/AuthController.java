package com.vaccine.web.controller;

import com.vaccine.common.dto.*;
import com.vaccine.common.exception.AppException;
import com.vaccine.common.dto.ApiMessage;
import com.vaccine.core.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest request) {
        log.info("Registration request received for email: {}", req.email());
        return ResponseEntity.ok(authService.register(req, request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        log.info("Login request received for email: {}", req.email());
        return ResponseEntity.ok(authService.login(req, request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiMessage> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiMessage> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        return ResponseEntity.ok(authService.resendEmailVerification(req));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessage> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.forgotPassword(req));
    }

@PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(new ApiMessage("Password reset successful"));
    }

    // 2FA endpoint - verify 2FA code
    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verify2FA(@Valid @RequestBody Verify2FARequest req, HttpServletRequest request) {
        return ResponseEntity.ok(authService.verify2FA(req, request));
    }

    // Phone verification endpoints
    @PostMapping("/phone/send-otp")
    public ResponseEntity<ApiMessage> sendPhoneOTP(@RequestParam String email) {
        return ResponseEntity.ok(authService.sendPhoneVerificationOTP(email));
    }

    @PostMapping("/phone/verify")
    public ResponseEntity<ApiMessage> verifyPhone(@Valid @RequestBody VerifyPhoneRequest req) {
        return ResponseEntity.ok(authService.verifyPhone(req));
    }
}
