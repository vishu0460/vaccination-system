package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {
    Optional<PhoneVerification> findByPhoneNumberAndOtpCodeAndExpiresAtAfterAndUsedFalse(String phoneNumber, String otpCode, LocalDateTime now);
    void deleteByPhoneNumber(String phoneNumber);
}
