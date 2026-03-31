package com.vaccine.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaccine.common.dto.LogFeedEntryResponse;
import com.vaccine.common.dto.LogFeedResponse;
import com.vaccine.common.dto.SystemLogEntryResponse;
import com.vaccine.common.dto.SystemLogPageResponse;
import com.vaccine.common.exception.AppException;
import com.vaccine.domain.AuditLog;
import com.vaccine.domain.RoleName;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.AuditLogRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RECENT_LOGS = 500;
    private static final int APP_LOG_SCAN_LIMIT = 600;

    private final ObjectMapper objectMapper;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final LogFormatter logFormatter;

    @Value("${logging.file.name:../logs/app.log}")
    private String logFilePath;

    public List<SystemLogEntryResponse> getRecentLogs(String level, String search, int limit) {
        try {
            return buildFilteredSystemLogs(level, search).stream()
                .limit(Math.max(1, Math.min(limit, MAX_RECENT_LOGS)))
                .toList();
        } catch (IOException ex) {
            throw new AppException("Unable to read application logs");
        }
    }

    public SystemLogPageResponse getRecentLogsPage(String level, String search, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        try {
            List<SystemLogEntryResponse> filteredEntries = buildFilteredSystemLogs(level, search);
            int fromIndex = Math.min(normalizedPage * normalizedSize, filteredEntries.size());
            int toIndex = Math.min(fromIndex + normalizedSize, filteredEntries.size());
            int totalPages = filteredEntries.isEmpty() ? 0 : (int) Math.ceil((double) filteredEntries.size() / normalizedSize);

            return new SystemLogPageResponse(
                filteredEntries.subList(fromIndex, toIndex),
                filteredEntries.size(),
                totalPages,
                normalizedPage,
                normalizedSize,
                toIndex >= filteredEntries.size()
            );
        } catch (IOException ex) {
            throw new AppException("Unable to read application logs");
        }
    }

    private List<SystemLogEntryResponse> buildFilteredSystemLogs(String level, String search) throws IOException {
        return readRecentLogLines().stream()
                .filter(line -> line != null && !line.isBlank())
                .map(this::parseLogLine)
                .filter(entry -> matchesLevel(entry, level))
                .filter(entry -> matchesSearch(entry, search))
                .sorted(Comparator.comparing(SystemLogEntryResponse::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_RECENT_LOGS)
                .toList();
    }

    public LogFeedResponse getActivityLogs(String search, String user, String actionType, String startDate, String endDate, int page, int size) {
        return getAuditLogFeed(search, user, actionType, startDate, endDate, page, size, false);
    }

    public LogFeedResponse getSecurityLogs(String search, String user, String actionType, String startDate, String endDate, int page, int size) {
        return getAuditLogFeed(search, user, actionType, startDate, endDate, page, size, true);
    }

    private LogFeedResponse getAuditLogFeed(
        String search,
        String user,
        String actionType,
        String startDate,
        String endDate,
        int page,
        int size,
        boolean securityOnly
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        List<LogFeedEntryResponse> filteredEntries = auditLogRepository.findAll().stream()
            .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .filter(log -> !securityOnly || isSecurityAuditEvent(log))
            .map(this::toAuditFeedEntry)
            .filter(entry -> matchesFeedSearch(entry, search))
            .filter(entry -> matchesActor(entry, user))
            .filter(entry -> matchesActionType(entry, actionType))
            .filter(entry -> matchesDateRange(entry.timestamp(), startDate, endDate))
            .toList();

        int fromIndex = Math.min(normalizedPage * normalizedSize, filteredEntries.size());
        int toIndex = Math.min(fromIndex + normalizedSize, filteredEntries.size());
        List<LogFeedEntryResponse> pageContent = filteredEntries.subList(fromIndex, toIndex);
        boolean hasMore = toIndex < filteredEntries.size();
        return new LogFeedResponse(pageContent, filteredEntries.size(), normalizedPage, normalizedSize, hasMore);
    }

    private Path resolveLogPath() {
        Path configured = Paths.get(logFilePath);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return Paths.get("").toAbsolutePath().resolve(configured).normalize();
    }

    private List<String> readRecentLogLines() throws IOException {
        Path path = resolveLogPath();
        if (!Files.exists(path)) {
            return List.of();
        }

        List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (allLines.size() <= APP_LOG_SCAN_LIMIT) {
            return allLines;
        }
        return allLines.subList(allLines.size() - APP_LOG_SCAN_LIMIT, allLines.size());
    }

    private SystemLogEntryResponse parseLogLine(String rawLine) {
        try {
            JsonNode node = objectMapper.readTree(rawLine);
            return new SystemLogEntryResponse(
                text(node, "@timestamp", "timestamp"),
                text(node, "level"),
                text(node, "message"),
                text(node, "service"),
                text(node, "requestId"),
                text(node, "userId"),
                text(node, "userEmail"),
                text(node, "requestPath"),
                text(node, "httpMethod"),
                text(node, "logger_name", "logger"),
                text(node, "stack_trace", "stackTrace"),
                rawLine
            );
        } catch (Exception ignored) {
            return new SystemLogEntryResponse(null, "INFO", rawLine, null, null, null, null, null, null, null, null, rawLine);
        }
    }

    private String text(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode child = node.get(fieldName);
            if (child != null && !child.isNull()) {
                return child.asText();
            }
        }
        return null;
    }

    private boolean matchesLevel(SystemLogEntryResponse entry, String level) {
        if (level == null || level.isBlank()) {
            return true;
        }
        String normalized = level.trim().toUpperCase(Locale.ROOT);
        return normalized.equals(String.valueOf(entry.level()).toUpperCase(Locale.ROOT));
    }

    private boolean matchesSearch(SystemLogEntryResponse entry, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.trim().toLowerCase(Locale.ROOT);
        return String.join(" ",
                List.of(
                    value(entry.message()),
                    value(entry.requestPath()),
                    value(entry.userId()),
                    value(entry.userEmail()),
                    value(entry.logger()),
                    value(entry.stackTrace())
                ))
            .toLowerCase(Locale.ROOT)
            .contains(needle);
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private LogFeedEntryResponse toAuditFeedEntry(AuditLog log) {
        ActorDetails actorDetails = resolveActorDetails(log.getActor());
        String derivedActionType = deriveActionType(log.getAction());
        return new LogFeedEntryResponse(
            "audit-" + log.getId(),
            log.getCreatedAt() == null ? null : log.getCreatedAt().toString(),
            "AUDIT",
            isSecurityAuditEvent(log) ? "SECURITY" : "ACTIVITY",
            derivedActionType,
            actorDetails.name(),
            actorDetails.role(),
            logFormatter.formatAuditMessage(log, actorDetails.name(), actorDetails.role()),
            derivedActionType.equals("ERROR") ? "ERROR" : "INFO",
            null,
            null,
            actorDetails.email(),
            actorDetails.userId(),
            log.getIp(),
            log.getResource(),
            log.getResourceId(),
            null,
            null,
            log.getDetails()
        );
    }

    private ActorDetails resolveActorDetails(String actor) {
        if (actor == null || actor.isBlank() || "SYSTEM".equalsIgnoreCase(actor)) {
            return new ActorDetails("System", "SYSTEM", null, null);
        }

        String normalizedActor = actor.trim();
        return userRepository.findByEmail(normalizedActor.toLowerCase(Locale.ROOT))
            .map(user -> new ActorDetails(
                user.getFullName() == null || user.getFullName().isBlank() ? normalizedActor : user.getFullName().trim(),
                humanizeRole(user.getEffectiveRole()),
                user.getEmail(),
                user.getId() == null ? null : String.valueOf(user.getId())
            ))
            .orElseGet(() -> new ActorDetails(normalizedActor, inferRoleFromActor(normalizedActor), normalizedActor.contains("@") ? normalizedActor : null, null));
    }

    private String inferRoleFromActor(String actor) {
        String normalized = actor.toLowerCase(Locale.ROOT);
        if (normalized.contains("super")) {
            return humanizeRole(RoleName.SUPER_ADMIN.name());
        }
        if (normalized.contains("admin")) {
            return humanizeRole(RoleName.ADMIN.name());
        }
        return "User";
    }

    private String humanizeRole(String role) {
        String normalized = role == null || role.isBlank() ? RoleName.USER.name() : role.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SUPER_ADMIN" -> "Super Admin";
            case "ADMIN" -> "Admin";
            case "CENTER_ADMIN" -> "Center Admin";
            case "SYSTEM" -> "System";
            default -> "User";
        };
    }

    private boolean isSecurityAuditEvent(AuditLog log) {
        String action = log.getAction() == null ? "" : log.getAction().trim().toUpperCase(Locale.ROOT);
        return action.contains("LOGIN")
            || action.contains("LOGOUT")
            || action.contains("PASSWORD")
            || action.contains("ROLE")
            || action.contains("2FA")
            || action.contains("LOCK")
            || action.contains("FAIL")
            || action.contains("ADMIN");
    }

    private String deriveActionType(String action) {
        String normalized = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("LOGIN")) {
            return normalized.contains("FAIL") ? "ERROR" : "LOGIN";
        }
        if (normalized.contains("LOGOUT")) {
            return "LOGOUT";
        }
        if (normalized.contains("DELETE")) {
            return "DELETE";
        }
        if (normalized.contains("CREATE") || normalized.contains("REGISTER")) {
            return "CREATE";
        }
        if (normalized.contains("UPDATE") || normalized.contains("EDIT") || normalized.contains("ROLE") || normalized.contains("PASSWORD")) {
            return "UPDATE";
        }
        if (normalized.contains("ERROR") || normalized.contains("FAIL") || normalized.contains("LOCK")) {
            return "ERROR";
        }
        return "INFO";
    }

    private boolean matchesFeedSearch(LogFeedEntryResponse entry, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String needle = search.trim().toLowerCase(Locale.ROOT);
        return String.join(" ",
                List.of(
                    value(entry.readableMessage()),
                    value(entry.actorName()),
                    value(entry.actorRole()),
                    value(entry.userEmail()),
                    value(entry.resource()),
                    value(entry.rawDetails()),
                    value(entry.ipAddress())
                ))
            .toLowerCase(Locale.ROOT)
            .contains(needle);
    }

    private boolean matchesActor(LogFeedEntryResponse entry, String user) {
        if (user == null || user.isBlank()) {
            return true;
        }
        String needle = user.trim().toLowerCase(Locale.ROOT);
        return value(entry.actorName()).toLowerCase(Locale.ROOT).contains(needle)
            || value(entry.userEmail()).toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean matchesActionType(LogFeedEntryResponse entry, String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return true;
        }
        return actionType.trim().equalsIgnoreCase(entry.actionType());
    }

    private boolean matchesDateRange(String timestamp, String startDate, String endDate) {
        if ((startDate == null || startDate.isBlank()) && (endDate == null || endDate.isBlank())) {
            return true;
        }

        LocalDate logDate = extractDate(timestamp);
        if (logDate == null) {
            return false;
        }

        if (startDate != null && !startDate.isBlank() && logDate.isBefore(LocalDate.parse(startDate))) {
            return false;
        }

        if (endDate != null && !endDate.isBlank() && logDate.isAfter(LocalDate.parse(endDate))) {
            return false;
        }

        return true;
    }

    private LocalDate extractDate(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(timestamp).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(timestamp).toLocalDate();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private record ActorDetails(String name, String role, String email, String userId) {}
}
