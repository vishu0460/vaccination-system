package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByTokenAndExpiresAtAfter(String token, java.time.LocalDateTime now);
    
    void deleteByUserEmail(String email);
}
