package com.vaccine.core.service;

import com.vaccine.domain.AuditLog;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class LogFormatter {

    public String formatAuditMessage(AuditLog log, String actorName, String actorRole) {
        String actorPrefix = buildActorPrefix(actorName, actorRole);
        String action = normalize(log.getAction());
        String subject = buildSubject(log);
        String details = normalizeDetails(log.getDetails());

        if (action.contains("LOGIN") && action.contains("FAIL")) {
            return actorPrefix + " had a failed login attempt";
        }
        if (action.contains("LOCK")) {
            return actorPrefix + " triggered an account lock";
        }
        if (action.contains("LOGIN")) {
            return actorPrefix + " signed in";
        }
        if (action.contains("LOGOUT")) {
            return actorPrefix + " signed out";
        }
        if (action.contains("PASSWORD")) {
            return actorPrefix + " changed their password";
        }
        if (action.contains("ROLE")) {
            return actorPrefix + " changed a user role" + appendDetails(details);
        }
        if (action.contains("ENABLE")) {
            return actorPrefix + " activated " + subject;
        }
        if (action.contains("DISABLE")) {
            return actorPrefix + " deactivated " + subject;
        }
        if (action.contains("DELETE")) {
            return actorPrefix + " deleted " + subject;
        }
        if (action.contains("CREATE") || action.contains("REGISTER")) {
            return actorPrefix + " created " + subject;
        }
        if (action.contains("UPDATE") || action.contains("EDIT")) {
            return actorPrefix + " updated " + subject;
        }
        if (action.contains("CANCEL")) {
            return actorPrefix + " cancelled " + subject;
        }
        if (action.contains("COMPLETE")) {
            return actorPrefix + " completed " + subject;
        }
        if (action.contains("RESCHEDULE")) {
            return actorPrefix + " rescheduled " + subject;
        }
        if (action.contains("2FA")) {
            return actorPrefix + " completed two-factor verification";
        }
        return actorPrefix + " performed " + humanizeToken(action) + appendSubject(subject, details);
    }

    private String buildActorPrefix(String actorName, String actorRole) {
        String normalizedName = actorName == null || actorName.isBlank() ? "System" : actorName.trim();
        String normalizedRole = actorRole == null || actorRole.isBlank() ? "" : actorRole.trim() + " ";
        return normalizedRole + normalizedName;
    }

    private String buildSubject(AuditLog log) {
        String resourceLabel = describeResource(log.getResource());
        String targetLabel = extractTargetLabel(log.getDetails());

        if (targetLabel != null && !targetLabel.isBlank()) {
            return resourceLabel + " '" + targetLabel + "'";
        }

        if (log.getResourceId() != null && !log.getResourceId().isBlank()) {
            return resourceLabel + " #" + log.getResourceId().trim();
        }

        return resourceLabel;
    }

    private String appendSubject(String subject, String details) {
        if (details == null || details.isBlank()) {
            return " on " + subject;
        }
        return " on " + subject + " (" + details + ")";
    }

    private String appendDetails(String details) {
        return details == null || details.isBlank() ? "" : " (" + details + ")";
    }

    private String describeResource(String resource) {
        return switch (normalize(resource)) {
            case "USER" -> "user";
            case "AUTH" -> "authentication session";
            case "CENTER" -> "center";
            case "DRIVE" -> "vaccination drive";
            case "SLOT" -> "slot";
            case "BOOKING" -> "booking";
            case "NEWS" -> "news post";
            case "FEEDBACK" -> "feedback item";
            case "CONTACT" -> "contact inquiry";
            default -> "record";
        };
    }

    private String extractTargetLabel(String details) {
        if (details == null || details.isBlank()) {
            return null;
        }

        int separatorIndex = details.indexOf(':');
        if (separatorIndex >= 0 && separatorIndex + 1 < details.length()) {
            return details.substring(separatorIndex + 1).trim();
        }

        return details.trim();
    }

    private String humanizeToken(String token) {
        String lower = normalize(token).replace('_', ' ').toLowerCase(Locale.ROOT);
        if (lower.isBlank()) {
            return "an action";
        }
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDetails(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
