package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.ReviewRequest;
import com.vaccine.common.dto.ReviewResponse;
import com.vaccine.domain.Review;
import com.vaccine.core.service.ReviewService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Validated
@RequestMapping({"/v1/reviews", "/reviews"})
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {
        log.info("Create review for centerId={}, rating={}, user={}", request.centerId(), request.rating(), auth.getName());
        Review review = reviewService.createReview(
            auth.getName(), request.centerId(), request.rating(), request.comment());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toResponse(review), "Review created"));
    }

    @GetMapping("/center/{centerId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getCenterReviews(@PathVariable Long centerId) {
        log.info("Get reviews for center={}", centerId);
        List<ReviewResponse> reviews = reviewService.getApprovedReviewsByCenter(centerId)
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/center/{centerId}/paged")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getCenterReviewsPaged(
            @PathVariable Long centerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get paged reviews for center={}, page={}, size={}", centerId, page, size);
        Page<ReviewResponse> reviews = reviewService.getApprovedReviewsByCenterPaged(
            centerId, PageRequest.of(page, size))
            .map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/center/{centerId}/rating")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCenterRating(@PathVariable Long centerId) {
        log.info("Get rating for center={}", centerId);
        Double avg = reviewService.getAverageRating(centerId);
        Long count = reviewService.getReviewCount(centerId);
        Map<String, Object> ratingInfo = Map.of(
            "averageRating", avg != null ? avg : 0.0,
            "reviewCount", count
        );
        return ResponseEntity.ok(ApiResponse.success(ratingInfo));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getAllReviews() {
        log.info("Get all reviews");
        List<ReviewResponse> reviews = reviewService.getAllReviews()
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @PatchMapping("/{reviewId}/approve")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(@PathVariable Long reviewId) {
        log.info("Approve review={}", reviewId);
        Review review = reviewService.approveReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(review), "Review approved"));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        log.info("Delete review={}", reviewId);
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
