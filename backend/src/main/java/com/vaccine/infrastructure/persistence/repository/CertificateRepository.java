package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByBookingId(Long bookingId);
    Optional<Certificate> findByCertificateNumber(String certificateNumber);
    List<Certificate> findByBookingUserIdOrderByIssuedAtDesc(Long userId);
}

