package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByTokenAndExpiresAtAfter(String token, java.time.LocalDateTime now);
    void deleteByUserEmail(String userEmail);
}
