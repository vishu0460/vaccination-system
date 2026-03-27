package com.vaccine.core.service;

import com.vaccine.common.exception.AppException;
import com.vaccine.domain.PhoneVerification;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.PhoneVerificationRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class PhoneVerificationService {

    public record PhoneOtpDispatchResult(String otp, boolean delivered) {}

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.sms.twilio.phone-number:}")
    private String twilioPhoneNumber;

    @Value("${app.dev.include-otp-in-messages:false}")
    private boolean includeOtpInMessages;

    public PhoneVerificationService(PhoneVerificationRepository phoneVerificationRepository) {
        this.phoneVerificationRepository = phoneVerificationRepository;
    }

    public PhoneOtpDispatchResult sendOtp(User user) {
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            throw new AppException("No phone number on file. Please add a phone number first.");
        }

        String otp = String.format("%06d", secureRandom.nextInt(900000) + 100000);
        phoneVerificationRepository.deleteByPhoneNumber(user.getPhoneNumber());
        phoneVerificationRepository.save(PhoneVerification.builder()
            .userId(user.getId())
            .phoneNumber(user.getPhoneNumber())
            .otpCode(otp)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .used(false)
            .build());

        if (!smsEnabled || hasMissingSmsCredentials()) {
            logSmsFallback(user, otp, "SMS provider is not configured");
            return new PhoneOtpDispatchResult(otp, false);
        }

        try {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            Message.creator(
                new PhoneNumber(user.getPhoneNumber()),
                new PhoneNumber(twilioPhoneNumber),
                "Your VaxZone verification OTP is " + otp + ". It expires in 5 minutes."
            ).create();
            return new PhoneOtpDispatchResult(otp, true);
        } catch (Exception exception) {
            logSmsFallback(user, otp, exception.getMessage());
            return new PhoneOtpDispatchResult(otp, false);
        }
    }

    public boolean verifyOTP(String phoneNumber, String otpCode) {
        return phoneVerificationRepository.findByPhoneNumberAndOtpCodeAndExpiresAtAfterAndUsedFalse(phoneNumber, otpCode, LocalDateTime.now())
            .map(pv -> {
                pv.setUsed(true);
                phoneVerificationRepository.save(pv);
                return true;
            }).orElse(false);
    }

    private boolean hasMissingSmsCredentials() {
        return twilioAccountSid == null || twilioAccountSid.isBlank()
            || twilioAuthToken == null || twilioAuthToken.isBlank()
            || twilioPhoneNumber == null || twilioPhoneNumber.isBlank();
    }

    private void logSmsFallback(User user, String otp, String reason) {
        if (includeOtpInMessages) {
            log.warn("SMS OTP delivery unavailable for userId={} reason={}. Dev fallback OTP={}", user.getId(), reason, otp);
            return;
        }
        log.warn("SMS OTP delivery unavailable for userId={} reason={}", user.getId(), reason);
    }
}
