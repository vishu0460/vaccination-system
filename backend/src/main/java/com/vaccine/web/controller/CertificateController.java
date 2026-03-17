package com.vaccine.web.controller;

import com.vaccine.common.dto.CertificateRequest;
import com.vaccine.common.dto.CertificateResponse;
import com.vaccine.domain.Certificate;
import com.vaccine.domain.User;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.core.service.CertificateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/certificates")
public class CertificateController {
    private final CertificateService certificateService;
    private final UserRepository userRepository;

    public CertificateController(CertificateService certificateService, UserRepository userRepository) {
        this.certificateService = certificateService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<CertificateResponse> generateCertificate(
            @Valid @RequestBody CertificateRequest request) {
        Certificate cert = certificateService.generateCertificate(
            request.bookingId(), request.vaccineName(), request.doseNumber());
        return ResponseEntity.ok(toResponse(cert));
    }

    @GetMapping("/my-certificates")
    public ResponseEntity<List<CertificateResponse>> myCertificates(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow(() -> new AppException("User not found"));
        List<CertificateResponse> certs = certificateService.getUserCertificates(user.getId())
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(certs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificateResponse> getCertificate(@PathVariable Long id) {
        Certificate cert = certificateService.getCertificateById(id);
        return ResponseEntity.ok(toResponse(cert));
    }

    @GetMapping("/verify/{certificateNumber}")
    public ResponseEntity<CertificateResponse> verifyCertificate(@PathVariable String certificateNumber) {
        Certificate cert = certificateService.getCertificateByNumber(certificateNumber);
        return ResponseEntity.ok(toResponse(cert));
    }

    private CertificateResponse toResponse(Certificate cert) {
        return new CertificateResponse(
            cert.getId(),
            cert.getCertificateNumber(),
            cert.getVaccineName(),
            cert.getDoseNumber(),
            cert.getNextDoseDate(),
            cert.getQrCode(),
            cert.getIssuedAt(),
            cert.getBooking().getUser().getFullName(),
            cert.getBooking().getUser().getEmail(),
            cert.getBooking().getSlot().getDrive().getCenter().getName(),
            cert.getBooking().getSlot().getDrive().getTitle(),
            cert.getBooking().getSlot().getStartTime().toString(),
            cert.getDigitalVerificationCode()
        );
    }
}
