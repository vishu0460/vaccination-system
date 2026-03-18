package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Certificate;
import com.vaccine.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByBookingId(Long bookingId);
    Optional<Certificate> findByCertificateNumber(String certificateNumber);
    List<Certificate> findByBookingUserIdOrderByIssuedAtDesc(Long userId);
    
    @Query("SELECT c FROM Certificate c WHERE c.booking.id = :bookingId")
    Optional<Certificate> findByBooking_Id(@Param("bookingId") Long bookingId);
    
    boolean existsByBookingId(Long bookingId);
}

