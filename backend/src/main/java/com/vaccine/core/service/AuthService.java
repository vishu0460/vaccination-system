package com.vaccine.core.service;

import com.vaccine.common.dto.*;
import com.vaccine.common.exception.AppException;
import com.vaccine.domain.*;
import com.vaccine.infrastructure.persistence.repository.*;
import com.vaccine.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PhoneVerificationService phoneVerificationService;

    private final Random random = new Random();

    @Value("${security.brute-force.max-attempts}")
    private int maxAttempts;

    @Value("${security.brute-force.lock-minutes}")
    private int lockMinutes;

    @Value("${app.dev.auto-verify-email:false}")
    private boolean autoVerifyEmail;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            EmailVerificationRepository emailVerificationRepository,
            PasswordResetRepository passwordResetRepository,
            PhoneVerificationRepository phoneVerificationRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NotificationService notificationService,
            AuditService auditService,
            PhoneVerificationService phoneVerificationService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.phoneVerificationService = phoneVerificationService;
    }

    public ApiMessage register(RegisterRequest req, HttpServletRequest request) {
        if (userRepository.existsByEmail(req.email().toLowerCase())) {
            throw new AppException("Email already registered");
        }

        Role role = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.USER).build()));

        boolean shouldAutoVerify = autoVerifyEmail;

        User user = User.builder()
                .email(req.email().toLowerCase())
                .fullName(req.fullName())
                .password(passwordEncoder.encode(req.password()))
                .phoneNumber(req.phoneNumber())
                .age(req.age())
                .enabled(true)
                .emailVerified(shouldAutoVerify)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .roles(Set.of(role))
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);

        if (!shouldAutoVerify) {
            String token = UUID.randomUUID().toString();
            emailVerificationRepository.save(
                    EmailVerification.builder()
                            .userEmail(user.getEmail())
                            .token(token)
                            .expiresAt(LocalDateTime.now().plusHours(24))
                            .verified(false)
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        }

        auditService.log(user.getEmail(),"REGISTER","AUTH","User registered",request);

        return new ApiMessage("Registration successful! " + (shouldAutoVerify ? "You can now login." : "Please check your email to verify your account."));
    }

    public AuthResponse login(LoginRequest req, HttpServletRequest request) {
        log.info("Login attempt for email: {}", req.email());

        User user = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> {
                    log.warn("User not found: {}", req.email());
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!user.getEnabled()) {
            log.warn("User account disabled: {}", req.email());
            throw new AppException("Account is disabled. Please contact administrator.");
        }

        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            log.warn("User account locked: {}", req.email());
            throw new AppException("Account locked. Try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.email().toLowerCase(),
                            req.password()
                    )
            );
            log.info("Authentication successful for email: {}", req.email());
        } catch (Exception e) {
            log.warn("Authentication failed for email: {} - {}", req.email(), e.getMessage());

            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if (user.getFailedLoginAttempts() >= maxAttempts) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                log.warn("User account locked due to too many failed attempts: {}", req.email());
            }

            userRepository.save(user);

            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.getEmailVerified()) {
            log.warn("Email not verified for: {}", req.email());
            throw new AppException("Email not verified. Please verify your email first.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);

        userRepository.save(user);

        String role = getUserRole(user);

        String accessToken = jwtService.createAccessToken(user.getEmail(), Map.of("role", role));
        String refreshToken = jwtService.createRefreshToken(user.getEmail());

        auditService.log(user.getEmail(),"LOGIN_SUCCESS","AUTH","User logged in",request);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.accessExpirySeconds(),
                user.getEmail(),
                role
        );
    }

    public AuthResponse refresh(RefreshTokenRequest req) {
        Claims claims = jwtService.parse(req.refreshToken());

        if (!"refresh".equals(String.valueOf(claims.get("type")))) {
            throw new AppException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(claims.getSubject())
                .orElseThrow(() -> new AppException("User not found"));

        String role = getUserRole(user);

        String access = jwtService.createAccessToken(user.getEmail(), Map.of("role", role));
        String refresh = jwtService.createRefreshToken(user.getEmail());

        return new AuthResponse(
                access,
                refresh,
                "Bearer",
                jwtService.accessExpirySeconds(),
                user.getEmail(),
                role
        );
    }

    public ApiMessage verifyEmail(String token) {
        EmailVerification verification =
                emailVerificationRepository.findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                        .filter(v -> !v.isVerified())
                        .orElseThrow(() -> new AppException("Invalid or expired verification token"));

        User user = userRepository.findByEmail(verification.getUserEmail())
                .orElseThrow(() -> new AppException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        verification.setVerified(true);
        emailVerificationRepository.save(verification);

        return new ApiMessage("Email verified successfully");
    }

    public ApiMessage resendEmailVerification(ResendVerificationRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new AppException("User not found"));

        if (user.getEmailVerified()) {
            return new ApiMessage("Email already verified");
        }

        String token = UUID.randomUUID().toString();

        emailVerificationRepository.save(
                EmailVerification.builder()
                        .userEmail(user.getEmail())
                        .token(token)
                        .expiresAt(LocalDateTime.now().plusHours(24))
                        .verified(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return new ApiMessage("Verification token generated (email send mock for dev)");
    }

    public ApiMessage sendPhoneVerificationOTP(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new AppException("User not found"));

        if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
            throw new AppException("No phone number on file. Please add a phone number first.");
        }

        boolean sent = phoneVerificationService.sendOTP(user);

        if (!sent) {
            throw new AppException("Failed to send OTP. Please try again later.");
        }

        return new ApiMessage("OTP sent to your phone");
    }

    public ApiMessage verifyPhone(VerifyPhoneRequest req) {
        boolean valid = phoneVerificationService.verifyOTP(req.phoneNumber(), req.otpCode());

        if (!valid) {
            throw new AppException("Invalid or expired OTP");
        }

        User user = userRepository.findByPhoneNumber(req.phoneNumber())
                .orElseThrow(() -> new AppException("User not found"));

        user.setPhoneVerified(true);
        userRepository.save(user);

        return new ApiMessage("Phone verified successfully");
    }

    public AuthResponse verify2FA(Verify2FARequest req, HttpServletRequest request) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new AppException("User not found"));

        if (!"123456".equals(req.twoFactorCode())) {
            throw new AppException("Invalid 2FA code");
        }

        String role = getUserRole(user);

        String accessToken = jwtService.createAccessToken(user.getEmail(), Map.of("role", role));
        String refreshToken = jwtService.createRefreshToken(user.getEmail());

        auditService.log(user.getEmail(), "2FA_LOGIN", "AUTH", "2FA verified", request);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.accessExpirySeconds(),
                user.getEmail(),
                role
        );
    }

    public ApiMessage forgotPassword(ForgotPasswordRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new AppException("User not found"));

        String resetToken = UUID.randomUUID().toString();
        passwordResetRepository.save(PasswordReset.builder()
                .userEmail(user.getEmail())
                .token(resetToken)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build());

        return new ApiMessage("Password reset token generated (email send mock for dev)");
    }

    public void resetPassword(ResetPasswordRequest req) {
        PasswordReset reset = passwordResetRepository.findByTokenAndExpiresAtAfter(req.token(), LocalDateTime.now())
                .filter(r -> !r.isUsed())
                .orElseThrow(() -> new AppException("Invalid or expired reset token"));

        User user = userRepository.findByEmail(reset.getUserEmail())
                .orElseThrow(() -> new AppException("User not found"));

        user.setPassword(passwordEncoder.encode(req.password()));
        userRepository.save(user);

        reset.setUsed(true);
        passwordResetRepository.save(reset);
    }

    private String getUserRole(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .map(RoleName::name)
                .findFirst()
                .orElse("USER");
    }
}

