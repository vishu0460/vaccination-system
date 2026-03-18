package com.vaccine.core.service;

import com.vaccine.domain.Review;
import com.vaccine.domain.User;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.ReviewRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationCenterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final VaccinationCenterRepository centerRepository;

    @Transactional
    public Review createReview(String email, Long centerId, Integer rating, String comment) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException("User not found"));
        VaccinationCenter center = centerRepository.findById(centerId).orElseThrow(() -> new AppException("Center not found"));
        
        if (reviewRepository.existsByUserIdAndCenterId(user.getId(), centerId)) {
            throw new AppException("You have already reviewed this center");
        }
        
        if (rating < 1 || rating > 5) {
            throw new AppException("Rating must be between 1 and 5");
        }
        
        Review review = new Review();
        review.setUser(user);
        review.setCenter(center);
        review.setRating(rating);
        review.setComment(comment);
        review.setIsApproved(false);
        
        return reviewRepository.save(review);
    }

    public List<Review> getApprovedReviewsByCenter(Long centerId) {
        return reviewRepository.findByCenterIdAndIsApprovedTrueOrderByCreatedAtDesc(centerId);
    }

    public Page<Review> getApprovedReviewsByCenterPaged(Long centerId, Pageable pageable) {
        return reviewRepository.findByCenterIdAndIsApprovedTrue(centerId, pageable);
    }

    public Double getAverageRating(Long centerId) {
        return reviewRepository.getAverageRatingByCenterId(centerId);
    }

    public Long getReviewCount(Long centerId) {
        return reviewRepository.countApprovedReviewsByCenterId(centerId);
    }

    @Transactional
    public Review approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new AppException("Review not found"));
        review.setIsApproved(true);
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }
}
