package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiMessage;
import com.vaccine.common.dto.FeedbackRequest;
import com.vaccine.domain.User;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.core.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

    public FeedbackController(FeedbackService feedbackService, UserRepository userRepository) {
        this.feedbackService = feedbackService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ApiMessage> submitFeedback(@Valid @RequestBody FeedbackRequest request,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = null;
        if (userDetails != null) {
            user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        }
        return ResponseEntity.ok(feedbackService.submitFeedback(request, user));
    }

    @GetMapping("/my-feedback")
    public ResponseEntity<List<Map<String, Object>>> getMyFeedback(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AppException("User not found"));
        return ResponseEntity.ok(feedbackService.getUserFeedback(user.getId()));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllFeedback() {
        return ResponseEntity.ok(feedbackService.getAllFeedback());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFeedbackById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    @PatchMapping("/{id}/respond")
    public ResponseEntity<ApiMessage> respondToFeedback(@PathVariable Long id,
                                                          @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(feedbackService.respondToFeedback(id, request.get("response")));
    }
}
