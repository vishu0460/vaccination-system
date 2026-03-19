package com.vaccine.core.service;

import com.vaccine.common.dto.ApiMessage;
import com.vaccine.common.dto.FeedbackRequest;
import com.vaccine.common.dto.FeedbackResponse;
import com.vaccine.domain.Feedback;
import com.vaccine.domain.FeedbackStatus;
import com.vaccine.domain.User;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.FeedbackRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    public ApiMessage submitFeedback(FeedbackRequest request, User user) {
        // Validate required fields
        if (request.subject() == null || request.subject().isBlank()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setRating(request.rating());
        feedback.setMessage(request.message());
        feedback.setSubject(request.subject());
        feedback.setStatus(FeedbackStatus.PENDING);

        feedbackRepository.save(feedback);
        return new ApiMessage("Feedback submitted successfully");
    }

    public List<Map<String, Object>> getUserFeedback(Long userId) {
        List<Feedback> feedbacks = feedbackRepository.findAll().stream()
            .filter(f -> f.getUser() != null && f.getUser().getId().equals(userId))
            .toList();
        return feedbacks.stream().map(this::toMap).toList();
    }

    public List<Map<String, Object>> getAllFeedback() {
        return feedbackRepository.findAll().stream()
            .map(this::toMap)
            .toList();
    }

    public Map<String, Object> getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new AppException("Feedback not found"));
        return toMap(feedback);
    }

    public FeedbackResponse getFeedbackByIdResponse(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new AppException("Feedback not found"));
        return toResponse(feedback);
    }

    public Page<FeedbackResponse> getAllFeedback(Pageable pageable) {
        return feedbackRepository.findAll(pageable)
            .map(this::toResponse);
    }

    public ApiMessage respondToFeedback(Long id, String response) {
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new AppException("Feedback not found"));

        feedback.setResponse(response);
        feedback.setStatus(FeedbackStatus.RESPONDED);
        feedbackRepository.save(feedback);

        return new ApiMessage("Response sent successfully");
    }

    public FeedbackResponse respondToFeedbackWithResponse(Long id, String response) {
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new AppException("Feedback not found"));

        feedback.setResponse(response);
        feedback.setStatus(FeedbackStatus.RESPONDED);
        feedbackRepository.save(feedback);

        return toResponse(feedback);
    }

    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found"));
        return user.getId();
    }

    private Map<String, Object> toMap(Feedback feedback) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", feedback.getId());
        map.put("userEmail", feedback.getUser() != null ? feedback.getUser().getEmail() : null);
        map.put("rating", feedback.getRating());
        map.put("subject", feedback.getSubject());
        map.put("message", feedback.getMessage());
        map.put("response", feedback.getResponse());
        map.put("status", feedback.getStatus().name());
        map.put("createdAt", feedback.getCreatedAt());
        return map;
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        FeedbackResponse response = new FeedbackResponse();
        response.setId(feedback.getId());
        response.setUserEmail(feedback.getUser() != null ? feedback.getUser().getEmail() : null);
        response.setRating(feedback.getRating());
        response.setSubject(feedback.getSubject());
        response.setMessage(feedback.getMessage());
        response.setStatus(feedback.getStatus().name());
        response.setAdminResponse(feedback.getResponse());
        response.setCreatedAt(feedback.getCreatedAt());
        return response;
    }
}

