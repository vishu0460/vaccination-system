package com.vaccine.core.service;

import com.vaccine.domain.*;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.CertificateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CertificateService {
    private final CertificateRepository certificateRepository;
    private final BookingRepository bookingRepository;

    public CertificateService(CertificateRepository certificateRepository, BookingRepository bookingRepository) {
        this.certificateRepository = certificateRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Certificate generateCertificate(Long bookingId, String vaccineName, Integer doseNumber) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found"));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new AppException("Booking must be completed to generate certificate");
        }

        boolean exists = certificateRepository.existsByBookingId(bookingId);
        Optional<Certificate> existing = Optional.ofNullable(certificateRepository.findByBookingId(bookingId))
            .orElse(Optional.empty());
        if (exists || existing.isPresent()) {
            throw new AppException("Certificate already exists for this booking");
        }

        String certificateNumber = generateCertificateNumber();
        
        Certificate certificate = Certificate.builder()
            .booking(booking)
            .user(booking.getUser())
            .certificateNumber(certificateNumber)
            .vaccineName(vaccineName)
            .doseNumber(doseNumber != null ? doseNumber : (booking.getDoseNumber() != null ? booking.getDoseNumber() : 1))
            .vaccinationDate(booking.getFirstDoseDate() != null ? booking.getFirstDoseDate() : booking.getBookedAt())
            .qrCodeData(certificateNumber)
            .qrCode(generateQrCode(certificateNumber))
            .digitalVerificationCode(generateDigitalVerificationCode(certificateNumber, booking))
            .nextDoseDate(booking.getNextDoseDueDate())
            .issuedAt(LocalDateTime.now())
            .build();

        Certificate savedCertificate = certificateRepository.save(certificate);
        String verificationUrl = buildFrontendVerificationUrl(savedCertificate.getId());
        savedCertificate.setQrCodeData(verificationUrl);
        savedCertificate.setQrCode(generateQrCode(verificationUrl));
        return certificateRepository.save(savedCertificate);
    }

    @Transactional
    public Certificate generate(Booking booking) {
        if (booking == null || booking.getId() == null) {
            throw new AppException("Booking not found");
        }

        String vaccineName = booking.getSlot() != null && booking.getSlot().getDrive() != null
            ? booking.getSlot().getDrive().getVaccineType()
            : "Vaccination";

        return generateCertificate(booking.getId(), vaccineName, booking.getDoseNumber());
    }

    public Certificate getCertificateById(Long id) {
        return certificateRepository.findById(id)
            .orElseThrow(() -> new AppException("Certificate not found"));
    }

    public Certificate getCertificateByBookingId(Long bookingId) {
        return certificateRepository.findByBooking_Id(bookingId)
            .orElseThrow(() -> new AppException("Certificate not found"));
    }

    public Certificate getCertificateByNumber(String certificateNumber) {
        return certificateRepository.findByCertificateNumber(certificateNumber)
            .orElseThrow(() -> new AppException("Certificate not found"));
    }

    public Certificate getCertificateByVerificationId(String certificateId) {
        if (certificateId == null || certificateId.isBlank()) {
            throw new AppException("Certificate not found");
        }

        return certificateRepository.findByCertificateNumber(certificateId)
            .or(() -> parseNumericCertificateId(certificateId).flatMap(certificateRepository::findById))
            .orElseThrow(() -> new AppException("Certificate not found"));
    }

    public List<Certificate> getUserCertificates(Long userId) {
        return certificateRepository.findByBookingUserIdOrderByIssuedAtDesc(userId);
    }

    public List<Certificate> getAllCertificates() {
        return certificateRepository.findAll();
    }

    public boolean certificateExistsForBooking(Long bookingId) {
        return certificateRepository.existsByBookingId(bookingId);
    }

    private String generateCertificateNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "VAX-" + timestamp + "-" + uuid;
    }

    private String generateQrCode(String certificateNumber) {
        return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + certificateNumber;
    }

    private String buildFrontendVerificationUrl(Long certificateId) {
        return "http://localhost:5173/verify-certificate?certId=" + certificateId;
    }

    private Optional<Long> parseNumericCertificateId(String value) {
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String generateDigitalVerificationCode(String certificateNumber, Booking booking) {
        try {
            String data = certificateNumber + ":" + booking.getUser().getEmail() + ":" + 
                          LocalDateTime.now().toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 32).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().substring(0, 32).toUpperCase();
        }
    }
}
