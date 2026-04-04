package com.vaccine.core.service;

import com.vaccine.common.dto.DownloadHistoryResponse;
import com.vaccine.common.exception.AppException;
import com.vaccine.domain.Certificate;
import com.vaccine.domain.DownloadHistory;
import com.vaccine.domain.DownloadType;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.CertificateRepository;
import com.vaccine.infrastructure.persistence.repository.DownloadHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DownloadHistoryService {
    private final DownloadHistoryRepository downloadHistoryRepository;
    private final CertificateRepository certificateRepository;

    public DownloadHistoryService(DownloadHistoryRepository downloadHistoryRepository,
                                  CertificateRepository certificateRepository) {
        this.downloadHistoryRepository = downloadHistoryRepository;
        this.certificateRepository = certificateRepository;
    }

    @Transactional
    public DownloadHistoryResponse recordDownload(User user, Long certificateId, String downloadTypeValue) {
        Certificate certificate = certificateRepository.findById(certificateId)
            .orElseThrow(() -> new AppException("Certificate not found"));
        if (!certificate.getUser().getId().equals(user.getId())) {
            throw new AppException("Certificate not found");
        }

        DownloadType downloadType;
        try {
            downloadType = DownloadType.valueOf(downloadTypeValue.trim().toUpperCase());
        } catch (Exception exception) {
            throw new AppException("Invalid download type");
        }

        DownloadHistory history = downloadHistoryRepository.save(
            DownloadHistory.builder()
                .user(user)
                .certificate(certificate)
                .downloadType(downloadType)
                .build()
        );

        return toResponse(history);
    }

    public List<DownloadHistoryResponse> getUserHistory(Long userId) {
        return downloadHistoryRepository.findByUserIdOrderByDownloadedAtDesc(userId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private DownloadHistoryResponse toResponse(DownloadHistory history) {
        return new DownloadHistoryResponse(
            history.getId(),
            history.getCertificate().getId(),
            history.getCertificate().getCertificateNumber(),
            history.getDownloadType().name(),
            history.getDownloadedAt()
        );
    }
}
