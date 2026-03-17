package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {
    PhoneVerification findByPhoneNumberAndOtpCode(String phoneNumber, String otpCode);
}
