package com.vaccine.web.controller;

import com.vaccine.common.dto.CertificateResponseMapper;
import com.vaccine.common.dto.VerifyCertificateResponse;
import com.vaccine.common.exception.AppException;
import com.vaccine.core.service.CertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/certificate", "/api/certificate"})
public class CertificateVerificationController {
    private final CertificateService certificateService;
    private final CertificateResponseMapper certificateResponseMapper;

    public CertificateVerificationController(CertificateService certificateService,
                                             CertificateResponseMapper certificateResponseMapper) {
        this.certificateService = certificateService;
        this.certificateResponseMapper = certificateResponseMapper;
    }

    @GetMapping("/verify/{certificateId}")
    public ResponseEntity<VerifyCertificateResponse> verifyCertificate(@PathVariable String certificateId) {
        try {
            var certificate = certificateService.getCertificateByVerificationId(certificateId);
            return ResponseEntity.ok(new VerifyCertificateResponse(
                true,
                "VALID",
                certificateResponseMapper.toResponse(certificate)
            ));
        } catch (AppException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new VerifyCertificateResponse(false, "INVALID", null)
            );
        }
    }
}
