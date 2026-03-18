package com.vaccine.core.service;

import com.vaccine.common.exception.AppException;
import com.vaccine.domain.PhoneVerification;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.PhoneVerificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PhoneVerificationService {

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final Random random = new Random();

    public PhoneVerificationService(PhoneVerificationRepository phoneVerificationRepository) {
        this.phoneVerificationRepository = phoneVerificationRepository;
    }

    public boolean sendOTP(User user) {
        String otp = String.format("%06d", random.nextInt(1000000));
        phoneVerificationRepository.save(PhoneVerification.builder()
            .userId(user.getId())
            .phoneNumber(user.getPhoneNumber())
            .otpCode(otp)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .used(false)
            .build());
        return true;
    }

    public boolean verifyOTP(String phoneNumber, String otpCode) {
        return phoneVerificationRepository.findByPhoneNumberAndOtpCodeAndExpiresAtAfterAndUsedFalse(phoneNumber, otpCode, LocalDateTime.now())
            .map(pv -> {
                pv.setUsed(true);
                phoneVerificationRepository.save(pv);
                return true;
            }).orElse(false);
    }
}
