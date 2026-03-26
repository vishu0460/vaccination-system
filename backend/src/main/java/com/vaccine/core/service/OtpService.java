package com.vaccine.core.service;

import com.vaccine.common.exception.AppException;
import com.vaccine.domain.OtpPurpose;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class OtpService {
    public record OtpDispatchResult(String otp, boolean delivered) {}


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${security.otp.max-attempts:5}")
    private int maxOtpAttempts;

    @Value("${security.otp.block-minutes:15}")
    private int otpBlockMinutes;

    @Value("${security.otp.max-requests-per-minute:3}")
    private int maxOtpRequestsPerMinute;

    public OtpService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public OtpDispatchResult sendOtp(User user, OtpPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        guardRequestRate(user, now, purpose);

        String otp = generateOtp();
        user.setOtpHash(passwordEncoder.encode(otp));
        user.setOtpExpiry(now.plusMinutes(otpExpiryMinutes));
        user.setOtpAttempts(0);
        user.setOtpPurpose(purpose);
        user.setOtpBlockedUntil(null);
        user.setOtpLastSentAt(now);
        userRepository.save(user);

        try {
            emailService.sendEmail(
                user.getEmail(),
                "VaxZone Security Verification Code",
                buildProfessionalEmailBody(user, otp, purpose)
            );
            return new OtpDispatchResult(otp, true);
        } catch (AppException exception) {
            log.warn("OTP email delivery failed for email={} purpose={}: {}", user.getEmail(), purpose, exception.getMessage());
            return new OtpDispatchResult(otp, false);
        }
    }

    @Transactional
    public void verifyOtp(User user, String otp, OtpPurpose expectedPurpose, boolean markUserVerified) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getOtpBlockedUntil() != null && user.getOtpBlockedUntil().isAfter(now)) {
            log.warn("Blocked OTP verification attempt for email={} purpose={} until={}", user.getEmail(), expectedPurpose, user.getOtpBlockedUntil());
            throw new AppException("Too many attempts. Try later.");
        }

        if (user.getOtpPurpose() != expectedPurpose) {
            log.warn("OTP purpose mismatch for email={} expected={} actual={}", user.getEmail(), expectedPurpose, user.getOtpPurpose());
            throw new AppException("Invalid OTP");
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(now)) {
            clearOtp(user);
            userRepository.save(user);
            throw new AppException("OTP expired");
        }

        if (user.getOtpAttempts() != null && user.getOtpAttempts() >= maxOtpAttempts) {
            user.setOtpBlockedUntil(now.plusMinutes(otpBlockMinutes));
            userRepository.save(user);
            log.warn("OTP attempt threshold exceeded for email={} purpose={}", user.getEmail(), expectedPurpose);
            throw new AppException("Too many attempts. Try later.");
        }

        if (user.getOtpHash() == null || !passwordEncoder.matches(otp, user.getOtpHash())) {
            int nextAttempts = (user.getOtpAttempts() == null ? 0 : user.getOtpAttempts()) + 1;
            user.setOtpAttempts(nextAttempts);
            if (nextAttempts >= maxOtpAttempts) {
                user.setOtpBlockedUntil(now.plusMinutes(otpBlockMinutes));
                log.warn("Suspicious OTP activity: email={} purpose={} failedAttempts={}", user.getEmail(), expectedPurpose, nextAttempts);
            }
            userRepository.save(user);
            throw new AppException(nextAttempts >= maxOtpAttempts ? "Too many attempts. Try later." : "Invalid OTP");
        }

        if (markUserVerified) {
            user.setEmailVerified(true);
        }
        clearOtp(user);
        userRepository.save(user);
    }

    @Transactional
    public void clearOtp(User user) {
        user.setOtpHash(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);
        user.setOtpPurpose(null);
        user.setOtpBlockedUntil(null);
    }

    public long secondsUntilExpiry(User user) {
        if (user.getOtpExpiry() == null) {
            return 0;
        }
        return Math.max(0, Duration.between(LocalDateTime.now(), user.getOtpExpiry()).getSeconds());
    }

    private void guardRequestRate(User user, LocalDateTime now, OtpPurpose purpose) {
        LocalDateTime windowStart = user.getOtpRequestWindowStart();
        int currentCount = user.getOtpRequestCount() == null ? 0 : user.getOtpRequestCount();

        if (windowStart == null || windowStart.plusMinutes(1).isBefore(now)) {
            user.setOtpRequestWindowStart(now);
            user.setOtpRequestCount(1);
            return;
        }

        if (currentCount >= maxOtpRequestsPerMinute) {
            log.warn("OTP request rate limit exceeded for email={} purpose={}", user.getEmail(), purpose);
            throw new AppException("Too many OTP requests. Please wait before trying again.");
        }

        user.setOtpRequestCount(currentCount + 1);
    }

    private String generateOtp() {
        return String.valueOf(secureRandom.nextInt(9_000_000) + 1_000_000);
    }

    private String buildProfessionalEmailBody(User user, String otp, OtpPurpose purpose) {
        return "Dear " + resolveDisplayName(user) + ",\n\n"
            + "Your One-Time Password (OTP) for " + describePurpose(purpose) + " is: " + otp + "\n\n"
            + "This OTP is valid for 10 minutes and is required to complete your request securely.\n\n"
            + "If you did not request this, please ignore this email or contact support immediately.\n\n"
            + "Do NOT share this OTP with anyone for security reasons.\n\n"
            + "Regards,\n"
            + "VaxZone Security Team";
    }

    private String resolveDisplayName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? "User" : user.getFullName().trim();
    }

    private String describePurpose(OtpPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> "account verification";
            case PASSWORD_RESET -> "password reset";
            case PASSWORD_CHANGE -> "password change";
            case ADMIN_SENSITIVE_ACTION -> "security confirmation";
        };
    }
}
