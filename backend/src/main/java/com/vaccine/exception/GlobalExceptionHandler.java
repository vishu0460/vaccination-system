package com.vaccine.exception;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.logging.LogContextKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Order(-2)
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        List<String> errorList = errors.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.toList());
        log.warn("Validation failed {}", buildRequestLog(request), ex);
        return ResponseEntity.badRequest().body(
            ApiResponse.builder()
                .success(false)
                .message("Validation failed")
                .timestamp(java.time.LocalDateTime.now())
                .status(400)
                .errors(errorList)
                .metadata(buildMetadata(Map.of("fieldErrors", errors)))
                .build()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> errors = ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.toList());
        log.warn("Constraint violation {}", buildRequestLog(request), ex);
        return ResponseEntity.badRequest().body(errorResponse(ex.getMessage(), errors, 400));
    }

    @ExceptionHandler({AppException.class, com.vaccine.common.exception.AppException.class, IllegalArgumentException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<?>> handleBusiness(RuntimeException ex, HttpServletRequest request) {
        log.warn("Business exception {} message={}", buildRequestLog(request), ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(errorResponse(ex.getMessage(), null, 400));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied {}", buildRequestLog(request), ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Access denied to this resource", null, 403));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<?>> handleNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Resource not found {}", buildRequestLog(request), ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse("Resource not found", null, 404));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleJsonParse(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null
            ? ex.getMostSpecificCause().getMessage()
            : ex.getMessage();
        log.warn("JSON parse error {} message={}", buildRequestLog(request), message, ex);
        return ResponseEntity.badRequest().body(errorResponse("Invalid JSON format: " + message, null, 400));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<?>> handleNPE(NullPointerException ex, HttpServletRequest request) {
        log.error("Null reference {}", buildRequestLog(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("Internal null reference error", null, 500));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnknown(Exception ex, HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        log.error("Unhandled exception {} message={}", buildRequestLog(request), message, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse(message, null, 500));
    }

    private ApiResponse<?> errorResponse(String message, List<String> errors, int status) {
        return ApiResponse.builder()
            .success(false)
            .message(message)
            .timestamp(java.time.LocalDateTime.now())
            .status(status)
            .errors(errors)
            .metadata(buildMetadata(null))
            .build();
    }

    private Map<String, Object> buildMetadata(Map<String, Object> extra) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        String requestId = MDC.get(LogContextKeys.REQUEST_ID);
        if (requestId != null && !requestId.isBlank()) {
            metadata.put(LogContextKeys.REQUEST_ID, requestId);
        }
        if (extra != null && !extra.isEmpty()) {
            metadata.putAll(extra);
        }
        return metadata.isEmpty() ? null : metadata;
    }

    private String buildRequestLog(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("method", request.getMethod());
        details.put("path", request.getRequestURI());
        if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
            details.put("query", sanitizeQuery(request.getQueryString()));
        }
        details.put("remoteAddr", request.getRemoteAddr());

        String requestId = MDC.get(LogContextKeys.REQUEST_ID);
        if (requestId != null && !requestId.isBlank()) {
            details.put(LogContextKeys.REQUEST_ID, requestId);
        }

        return details.toString();
    }

    private String sanitizeQuery(String query) {
        return query
            .replaceAll("(?i)(password=)[^&]+", "$1***")
            .replaceAll("(?i)(token=)[^&]+", "$1***")
            .replaceAll("(?i)(authorization=)[^&]+", "$1***");
    }
}
