package com.vaccine.core.service;

import com.vaccine.common.dto.ChangePasswordRequest;
import com.vaccine.common.dto.ProfileUpdateRequest;
import com.vaccine.common.exception.AppException;
import com.vaccine.domain.OtpPurpose;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.util.AgeCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ProfileService {
    private static final String STRONG_PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuditService auditService;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder, OtpService otpService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.auditService = auditService;
    }

    public User getProfile(String email) {
        log.info("Resolving profile for email={}", email);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found"));
    }

    public User getFirstEnabledUserForDebug() {
        User user = userRepository.findFirstByEnabledTrueOrderByIdAsc()
            .orElseThrow(() -> new AppException("No enabled user found for debug profile access"));
        log.warn("Using debug profile fallback userId={} email={}", user.getId(), user.getEmail());
        return user;
    }

    @Transactional
    public User updateProfile(String email, ProfileUpdateRequest request) {
        User user = getProfile(email);
        
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.dob() != null) {
            user.setDob(request.dob());
            user.setAge(AgeCalculator.calculateAge(request.dob()));
        } else if (request.age() != null) {
            if (request.age() < 1 || request.age() > 120) {
                throw new AppException("Age must be between 1 and 120");
            }
            user.setAge(request.age());
            user.setDob(null);
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhoneNumber(request.phone());
        }
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.city() != null) {
            user.setUserCity(request.city());
        }
        if (request.state() != null) {
            user.setUserState(request.state());
        }
        if (request.pincode() != null) {
            user.setUserPincode(request.pincode());
        }
        if (request.profileImage() != null) {
            user.setProfileImage(request.profileImage().isBlank() ? null : request.profileImage());
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public void sendPasswordChangeOtp(String email) {
        User user = getProfile(email);
        otpService.sendOtp(user, OtpPurpose.PASSWORD_CHANGE);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getProfile(email);
        String currentPassword = request.currentPassword();
        String newPassword = request.newPassword();

        if (currentPassword == null || currentPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new AppException("Current and new password are required");
        }
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AppException("Current password is incorrect");
        }

        if (!newPassword.matches(STRONG_PASSWORD_REGEX)) {
            throw new AppException("New password must include uppercase, lowercase, number, and special character");
        }

        otpService.verifyOtp(user, request.otp(), OtpPurpose.PASSWORD_CHANGE, false);
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditService.logActionAs(user.getEmail(), "PASSWORD_CHANGE", "AUTH", user.getId(), "Password changed successfully", null);
    }

    @Transactional
    public void deactivateAccount(String email) {
        User user = getProfile(email);
        user.setEnabled(false);
        userRepository.save(user);
    }
}
