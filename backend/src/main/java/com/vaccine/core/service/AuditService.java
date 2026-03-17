package com.vaccine.core.service;

import com.vaccine.domain.AuditLog;
import com.vaccine.infrastructure.persistence.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String actor, String action, String resource, String details, HttpServletRequest request) {
        AuditLog log = AuditLog.builder()
            .actor(actor)
            .action(action)
            .resource(resource)
            .details(details)
            .ip(request.getRemoteAddr())
            .build();
        auditLogRepository.save(log);
    }
}

