package com.vaccine.core.service;

import com.vaccine.domain.AuditLog;
import com.vaccine.infrastructure.persistence.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(String action, String resource, Object resourceId, String details, HttpServletRequest request) {
        logActionAs(resolveActor(), action, resource, resourceId, details, request);
    }

    public void logAction(String action, String resource, Object resourceId, String details) {
        logAction(action, resource, resourceId, details, null);
    }

    public String getCurrentActor() {
        return resolveActor();
    }

    public void logActionAs(String actor, String action, String resource, Object resourceId, String details, HttpServletRequest request) {
        AuditLog log = AuditLog.builder()
            .actor(normalizeActor(actor))
            .action(action)
            .resource(resource)
            .resourceId(resourceId == null ? null : String.valueOf(resourceId))
            .details(details)
            .ip(resolveIp(request))
            .build();
        auditLogRepository.save(log);
    }

    public void log(String actor, String action, String resource, String details, HttpServletRequest request) {
        logActionAs(actor, action, resource, null, details, request);
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "SYSTEM";
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return "SYSTEM";
        }
        return name;
    }

    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "SYSTEM" : actor;
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "SYSTEM";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "SYSTEM" : remoteAddr;
    }
}
