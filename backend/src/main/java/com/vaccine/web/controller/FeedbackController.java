package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.FeedbackRequest;
import com.vaccine.domain.User;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.core.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Validated
@RequestMapping({"/feedback", "/api/feedback"})
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

    public FeedbackController(FeedbackService feedbackService, UserRepository userRepository) {
        this.feedbackService = feedbackService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(@Valid @RequestBody FeedbackRequest request,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Submit feedback request from user: {}", userDetails.getUsername());
        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new AppException("User not found"));
        feedbackService.submitFeedback(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "Feedback submitted successfully"));
    }

    @GetMapping("/my-feedback")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyFeedback(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Get my feedback for user: {}", userDetails.getUsername());
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AppException("User not found"));
        List<Map<String, Object>> feedbackList = feedbackService.getUserFeedback(user.getId());
        return ResponseEntity.ok(ApiResponse.success(feedbackList));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllFeedback() {
        log.info("Get all feedback");
        List<Map<String, Object>> feedbackList = feedbackService.getAllFeedback();
        return ResponseEntity.ok(ApiResponse.success(feedbackList));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeedbackById(@PathVariable Long id) {
        log.info("Get feedback by ID: {}", id);
        Map<String, Object> feedback = feedbackService.getFeedbackById(id);
        return ResponseEntity.ok(ApiResponse.success(feedback));
    }

    @PatchMapping("/{id}/respond")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> respondToFeedback(@PathVariable Long id,
                                                          @RequestBody Map<String, String> request) {
        log.info("Respond to feedback ID: {}", id);
        String responseText = request.get("response");
        if (responseText == null || responseText.trim().isEmpty()) {
            throw new AppException("Response text is required");
        }
        feedbackService.respondToFeedback(id, responseText.trim());
        return ResponseEntity.ok(ApiResponse.success(null, "Response sent successfully"));
    }
}
