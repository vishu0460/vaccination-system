package com.vaccine.core.service;

import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.PhoneVerificationRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.exception.AppException;
import org.springframework.stereotype.Service;

@Service
public class PhoneVerificationService {

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final UserRepository userRepository;

    public PhoneVerificationService(PhoneVerificationRepository phoneVerificationRepository, UserRepository userRepository) {
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.userRepository = userRepository;
    }

    public boolean sendOTP(User user) {
        // Mock SMS send (Twilio config not set in local)
        // Generate 6 digit OTP, save to PhoneVerification, "send" SMS
        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        // Save verification record
        return true; // Mock success
    }

    public boolean verifyOTP(String phoneNumber, String otpCode) {
        // Check OTP for phone, mark used
        // Mock verification
        return true; // Mock success for dev
    }
}

