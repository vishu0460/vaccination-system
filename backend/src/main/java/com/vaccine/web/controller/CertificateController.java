package com.vaccine.web.controller;

import com.vaccine.common.dto.CertificateRequest;
import com.vaccine.common.dto.CertificateResponse;
import com.vaccine.common.dto.CertificateResponseMapper;
import com.vaccine.common.dto.CertificateDownloadRequest;
import com.vaccine.common.dto.DownloadHistoryResponse;
import com.vaccine.domain.Certificate;
import com.vaccine.domain.User;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.core.service.CertificateService;
import com.vaccine.core.service.DownloadHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/certificates", "/api/certificates"})
public class CertificateController {
    private final CertificateService certificateService;
    private final UserRepository userRepository;
    private final DownloadHistoryService downloadHistoryService;
    private final CertificateResponseMapper certificateResponseMapper;

    public CertificateController(CertificateService certificateService,
                                 UserRepository userRepository,
                                 DownloadHistoryService downloadHistoryService,
                                 CertificateResponseMapper certificateResponseMapper) {
        this.certificateService = certificateService;
        this.userRepository = userRepository;
        this.downloadHistoryService = downloadHistoryService;
        this.certificateResponseMapper = certificateResponseMapper;
    }

    @PostMapping
    public ResponseEntity<CertificateResponse> generateCertificate(
            @Valid @RequestBody CertificateRequest request) {
        Certificate cert = certificateService.generateCertificate(
            request.bookingId(), request.vaccineName(), request.doseNumber());
        return ResponseEntity.ok(certificateResponseMapper.toResponse(cert));
    }

    @GetMapping("/my-certificates")
    public ResponseEntity<List<CertificateResponse>> myCertificates(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow(() -> new AppException("User not found"));
        List<CertificateResponse> certs = certificateService.getUserCertificates(user.getId())
            .stream()
            .map(certificateResponseMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(certs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificateResponse> getCertificate(@PathVariable Long id) {
        Certificate cert = certificateService.getCertificateById(id);
        return ResponseEntity.ok(certificateResponseMapper.toResponse(cert));
    }

    @GetMapping
    public ResponseEntity<List<CertificateResponse>> getAllCertificates() {
        List<CertificateResponse> certs = certificateService.getAllCertificates()
            .stream()
            .map(certificateResponseMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(certs);
    }

    @GetMapping("/verify/{certificateNumber}")
    public ResponseEntity<CertificateResponse> verifyCertificate(@PathVariable String certificateNumber) {
        Certificate cert = certificateService.getCertificateByNumber(certificateNumber);
        return ResponseEntity.ok(certificateResponseMapper.toResponse(cert));
    }

    @PostMapping("/{certificateId}/downloads")
    public ResponseEntity<DownloadHistoryResponse> recordDownload(@PathVariable Long certificateId,
                                                                  @Valid @RequestBody CertificateDownloadRequest request,
                                                                  Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow(() -> new AppException("User not found"));
        return ResponseEntity.ok(downloadHistoryService.recordDownload(user, certificateId, request.downloadType()));
    }

    @GetMapping("/download-history")
    public ResponseEntity<List<DownloadHistoryResponse>> getDownloadHistory(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow(() -> new AppException("User not found"));
        return ResponseEntity.ok(downloadHistoryService.getUserHistory(user.getId()));
    }
}
