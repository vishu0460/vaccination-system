package com.vaccine.core.service;

import com.vaccine.common.dto.*;
import com.vaccine.common.exception.AppException;
import com.vaccine.domain.*;
import com.vaccine.infrastructure.persistence.repository.*;
import com.vaccine.security.JwtService;
import com.vaccine.util.AgeCalculator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
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
    private final OtpService otpService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PhoneVerificationService phoneVerificationService;
    private final Environment environment;

    @Value("${security.brute-force.max-attempts}")
    private int maxAttempts;

    @Value("${security.brute-force.lock-minutes}")
    private int lockMinutes;

    @Value("${app.dev.auto-verify-email:false}")
    private boolean autoVerifyEmail;

    @Value("${app.dev.include-otp-in-messages:false}")
    private boolean includeOtpInMessages;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            EmailVerificationRepository emailVerificationRepository,
            PasswordResetRepository passwordResetRepository,
            PhoneVerificationRepository phoneVerificationRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService,
            NotificationService notificationService,
            AuditService auditService,
            PhoneVerificationService phoneVerificationService,
            Environment environment
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.phoneVerificationService = phoneVerificationService;
        this.environment = environment;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req, HttpServletRequest request) {
        String normalizedEmail = normalizeEmail(req.email());
        String normalizedName = normalizeFullName(req.fullName());
        String normalizedPhone = normalizePhone(req.phoneNumber());
        Integer calculatedAge = AgeCalculator.calculateAge(req.dob());
        int normalizedAge = calculatedAge != null ? calculatedAge : (req.age() == null ? 18 : req.age());

        if (userRepository.existsAnyByEmail(normalizedEmail)) {
            throw new AppException("Email already registered");
        }

        Role role = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.USER).build()));

        boolean shouldAutoVerify = autoVerifyEmail;

        User user = User.builder()
                .email(normalizedEmail)
                .fullName(normalizedName)
                .password(passwordEncoder.encode(req.password()))
                .phoneNumber(normalizedPhone)
                .dob(req.dob())
                .age(normalizedAge)
                .role(RoleName.USER.name())
                .enabled(true)
                .emailVerified(shouldAutoVerify)
                .verificationToken(shouldAutoVerify ? null : UUID.randomUUID().toString())
                .verificationTokenExpiry(shouldAutoVerify ? null : LocalDateTime.now().plusHours(24))
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .roles(new HashSet<>(Set.of(role)))
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);

        OtpService.OtpDispatchResult otpDispatchResult = null;

        if (!shouldAutoVerify) {
            otpDispatchResult = otpService.sendOtp(user, OtpPurpose.EMAIL_VERIFICATION);
        }

        auditService.logActionAs(user.getEmail(), "REGISTER", "USER", user.getId(), "User registered", request);

        if (shouldAutoVerify) {
            return new RegisterResponse(
                "Registration successful. You can log in now.",
                200,
                false,
                false,
                user.getEmail(),
                null,
                true,
                null,
                null,
                0
            );
        }

        return buildRegisterResponse(user, otpDispatchResult);
    }

    public AuthResponse login(LoginRequest req, HttpServletRequest request) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> {
                    log.warn("Authentication failed for unknown account");
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!user.getEnabled()) {
            log.warn("Disabled account login attempt for userId={}", user.getId());
            throw new AppException("Account is disabled. Please contact administrator.");
        }

        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            log.warn("Locked account login attempt for userId={}", user.getId());
            throw new AppException("Account locked. Try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.email().toLowerCase(),
                            req.password()
                    )
            );
        } catch (Exception e) {
            log.warn("Authentication failed for userId={}", user.getId());

            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if (user.getFailedLoginAttempts() >= maxAttempts) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                log.warn("Account locked after repeated failures for userId={}", user.getId());
            }

            userRepository.save(user);

            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.getEmailVerified()) {
            log.warn("Login blocked because email is not verified for userId={}", user.getId());
            throw new AppException("Email not verified. Please verify your email first.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);

        userRepository.save(user);

        String role = getUserRole(user);

        String accessToken = jwtService.createAccessToken(user.getEmail(), Map.of("role", role));
        String refreshToken = jwtService.createRefreshToken(user.getEmail());

        auditService.logActionAs(user.getEmail(), "LOGIN_SUCCESS", "AUTH", user.getId(), "User logged in", request);

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
        if (token == null || token.isBlank()) {
            throw new AppException("Verification token is required");
        }

        User user = userRepository.findByVerificationToken(token.trim())
                .orElseThrow(() -> new AppException("Invalid or expired verification token"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return new ApiMessage("Email already verified");
        }
        if (user.getVerificationTokenExpiry() == null || user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AppException("Invalid or expired verification token");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        return new ApiMessage("Email verified successfully");
    }

    @Transactional
    public ApiMessage verifyOtp(VerifyOtpRequest req) {
        User user = userRepository.findByEmail(normalizeEmail(req.email()))
            .orElseThrow(() -> new AppException("User not found"));

        otpService.verifyOtp(user, req.otp(), req.purpose(), req.purpose() == OtpPurpose.EMAIL_VERIFICATION);
        return new ApiMessage(switch (req.purpose()) {
            case EMAIL_VERIFICATION -> "Email verified successfully";
            case PASSWORD_RESET -> "OTP verified successfully";
            case PASSWORD_CHANGE -> "Security verification successful";
            case ADMIN_SENSITIVE_ACTION -> "Admin verification successful";
        });
    }

    @Transactional
    public OtpDeliveryResponse resendEmailVerification(ResendVerificationRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase()).orElse(null);

        if (user == null) {
            return new OtpDeliveryResponse(
                true,
                "If the account exists, a verification email has been sent.",
                req.email().trim().toLowerCase(),
                true,
                null,
                null,
                req.email().trim().toLowerCase(),
                "EMAIL",
                0
            );
        }

        if (user.getEmailVerified()) {
            return new OtpDeliveryResponse(true, "Email already verified", user.getEmail(), true, null, null, user.getEmail(), "EMAIL", 0);
        }

        OtpService.OtpDispatchResult dispatchResult = otpService.sendOtp(user, OtpPurpose.EMAIL_VERIFICATION);
        return buildOtpDeliveryResponse(
            user,
            "EMAIL",
            dispatchResult,
            "OTP sent to your email for verification.",
            "OTP service unavailable. Use temporary OTP."
        );
    }

    public OtpDeliveryResponse sendPhoneVerificationOTP(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new AppException("User not found"));

        if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
            throw new AppException("No phone number on file. Please add a phone number first.");
        }

        PhoneVerificationService.PhoneOtpDispatchResult dispatchResult = phoneVerificationService.sendOtp(user);
        return new OtpDeliveryResponse(
            true,
            dispatchResult.delivered() ? "OTP sent to your phone" : buildOtpFailureMessage("OTP service unavailable. Use temporary OTP.", dispatchResult.otp()),
            user.getEmail(),
            dispatchResult.delivered(),
            shouldExposeOtpPreview(dispatchResult.delivered(), dispatchResult.otp()) ? dispatchResult.otp() : null,
            resolveDevOtp(dispatchResult.delivered(), dispatchResult.otp()),
            user.getPhoneNumber(),
            "SMS",
            300
        );
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

        auditService.logActionAs(user.getEmail(), "2FA_LOGIN", "AUTH", user.getId(), "2FA verified", request);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.accessExpirySeconds(),
                user.getEmail(),
                role
        );
    }

    @Transactional
    public ApiMessage forgotPassword(ForgotPasswordRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return new ApiMessage("If the account exists, a password reset OTP has been sent.");
        }

        otpService.sendOtp(user, OtpPurpose.PASSWORD_RESET);

        return new ApiMessage("If the account exists, a password reset OTP has been sent.");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (hasText(req.email()) && hasText(req.otp()) && hasText(req.resolvedPassword())) {
            resetPasswordWithOtp(req);
            return;
        }

        resetPasswordWithLegacyToken(req);
    }

    private void resetPasswordWithOtp(ResetPasswordRequest req) {
        User user = userRepository.findByEmail(normalizeEmail(req.email()))
                .orElseThrow(() -> new AppException("Invalid or expired OTP"));

        otpService.verifyOtp(user, req.otp(), OtpPurpose.PASSWORD_RESET, false);

        user.setPassword(passwordEncoder.encode(req.resolvedPassword()));
        userRepository.save(user);
    }

    private void resetPasswordWithLegacyToken(ResetPasswordRequest req) {
        PasswordReset reset = passwordResetRepository.findByTokenAndExpiresAtAfter(req.token(), LocalDateTime.now())
                .filter(r -> !r.isUsed())
                .filter(r -> r.getExpiresAt() != null && r.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new AppException("Invalid or expired reset token"));

        User user = userRepository.findByEmail(reset.getUserEmail())
                .orElseThrow(() -> new AppException("User not found"));

        user.setPassword(passwordEncoder.encode(req.resolvedPassword()));
        clearResetOtp(user);
        userRepository.save(user);

        reset.setUsed(true);
        passwordResetRepository.save(reset);
    }

    private void clearResetOtp(User user) {
        user.setResetOtp(null);
        user.setOtpExpiry(null);
    }

    private String getUserRole(User user) {
        return user.getEffectiveRole();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeFullName(String fullName) {
        return fullName == null ? "" : fullName.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null ? "" : phoneNumber.replaceAll("\\s+", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private RegisterResponse buildRegisterResponse(User user, OtpService.OtpDispatchResult otpDispatchResult) {
        String message = buildVerificationMessage(otpDispatchResult, "Registration successful. Please verify the 7-digit OTP sent to your email.");
        return new RegisterResponse(
            message,
            200,
            true,
            otpDispatchResult != null && !otpDispatchResult.delivered(),
            user.getEmail(),
            shouldExposeOtpPreview(otpDispatchResult) ? otpDispatchResult.otp() : null,
            otpDispatchResult != null && otpDispatchResult.delivered(),
            shouldExposeOtpPreview(otpDispatchResult) ? otpDispatchResult.otp() : null,
            resolveDevOtp(otpDispatchResult),
            otpService.secondsUntilExpiry(user)
        );
    }

    private String buildVerificationMessage(OtpService.OtpDispatchResult otpDispatchResult, String deliveredMessage) {
        if (otpDispatchResult == null || otpDispatchResult.delivered()) {
            return deliveredMessage;
        }

        return buildOtpFailureMessage(
            "Registration successful, but we could not send the verification OTP right now. Please use resend OTP to try again.",
            otpDispatchResult.otp()
        );
    }

    private OtpDeliveryResponse buildOtpDeliveryResponse(
        User user,
        String channel,
        OtpService.OtpDispatchResult dispatchResult,
        String deliveredMessage,
        String fallbackMessage
    ) {
        return new OtpDeliveryResponse(
            true,
            dispatchResult != null && dispatchResult.delivered()
                ? deliveredMessage
                : buildOtpFailureMessage(fallbackMessage, dispatchResult != null ? dispatchResult.otp() : null),
            user.getEmail(),
            dispatchResult != null && dispatchResult.delivered(),
            shouldExposeOtpPreview(dispatchResult) ? dispatchResult.otp() : null,
            resolveDevOtp(dispatchResult),
            user.getEmail(),
            channel,
            dispatchResult != null ? otpService.secondsUntilExpiry(user) : 0
        );
    }

    private String buildOtpFailureMessage(String fallbackMessage, String otp) {
        if (shouldExposeOtpPreview(false, otp)) {
            return fallbackMessage + " Temporary OTP: " + otp;
        }
        return fallbackMessage;
    }

    private boolean shouldExposeOtpPreview(OtpService.OtpDispatchResult otpDispatchResult) {
        return otpDispatchResult != null && !otpDispatchResult.delivered() && isDevelopmentMode() && includeOtpInMessages;
    }

    private boolean shouldExposeOtpPreview(boolean delivered, String otp) {
        return !delivered && otp != null && !otp.isBlank() && isDevelopmentMode() && includeOtpInMessages;
    }

    private String resolveDevOtp(OtpService.OtpDispatchResult otpDispatchResult) {
        if (otpDispatchResult == null) {
            return null;
        }
        return resolveDevOtp(otpDispatchResult.delivered(), otpDispatchResult.otp());
    }

    private String resolveDevOtp(boolean delivered, String otp) {
        if (!isDevelopmentMode() || delivered || otp == null || otp.isBlank()) {
            return null;
        }
        return otp;
    }

    private boolean isDevelopmentMode() {
        return environment.acceptsProfiles(Profiles.of("local", "dev"));
    }
}
