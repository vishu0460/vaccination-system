package com.vaccine.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaccine.common.dto.SystemLogEntryResponse;
import com.vaccine.common.exception.AppException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogService {
    private final ObjectMapper objectMapper;

    @Value("${logging.file.name:../logs/app.log}")
    private String logFilePath;

    public List<SystemLogEntryResponse> getRecentLogs(String level, String search, int limit) {
        Path path = resolveLogPath();
        if (!Files.exists(path)) {
            return List.of();
        }

        try {
            List<SystemLogEntryResponse> entries = Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                .filter(line -> line != null && !line.isBlank())
                .map(this::parseLogLine)
                .filter(entry -> matchesLevel(entry, level))
                .filter(entry -> matchesSearch(entry, search))
                .sorted(Comparator.comparing(SystemLogEntryResponse::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(1, Math.min(limit, 500)))
                .toList();

            return entries;
        } catch (IOException ex) {
            throw new AppException("Unable to read application logs");
        }
    }

    private Path resolveLogPath() {
        Path configured = Paths.get(logFilePath);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return Paths.get("").toAbsolutePath().resolve(configured).normalize();
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
}
