package com.vaccine.web.controller;

import com.vaccine.common.dto.ReviewRequest;
import com.vaccine.common.dto.ReviewResponse;
import com.vaccine.domain.Review;
import com.vaccine.core.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {
        Review review = reviewService.createReview(
            auth.getName(), request.centerId(), request.rating(), request.comment());
        return ResponseEntity.ok(toResponse(review));
    }

    @GetMapping("/center/{centerId}")
    public ResponseEntity<List<ReviewResponse>> getCenterReviews(@PathVariable Long centerId) {
        List<ReviewResponse> reviews = reviewService.getApprovedReviewsByCenter(centerId)
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/center/{centerId}/paged")
    public ResponseEntity<Page<ReviewResponse>> getCenterReviewsPaged(
            @PathVariable Long centerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResponse> reviews = reviewService.getApprovedReviewsByCenterPaged(
            centerId, PageRequest.of(page, size))
            .map(this::toResponse);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/center/{centerId}/rating")
    public ResponseEntity<Map<String, Object>> getCenterRating(@PathVariable Long centerId) {
        Double avg = reviewService.getAverageRating(centerId);
        Long count = reviewService.getReviewCount(centerId);
        return ResponseEntity.ok(Map.of(
            "averageRating", avg != null ? avg : 0.0,
            "reviewCount", count
        ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        List<ReviewResponse> reviews = reviewService.getAllReviews()
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(reviews);
    }

    @PatchMapping("/{reviewId}/approve")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<ReviewResponse> approveReview(@PathVariable Long reviewId) {
        Review review = reviewService.approveReview(reviewId);
        return ResponseEntity.ok(toResponse(review));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getRating(),
            review.getComment(),
            review.getCreatedAt(),
            review.getUser().getFullName(),
            review.getCenter().getId(),
            review.getCenter().getName()
        );
    }
}
