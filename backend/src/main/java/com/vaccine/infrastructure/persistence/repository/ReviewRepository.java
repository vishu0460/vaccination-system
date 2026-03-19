package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCenterIdAndIsApprovedTrueOrderByCreatedAtDesc(Long centerId);

    Page<Review> findByCenterIdAndIsApprovedTrue(Long centerId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.center.id = :centerId AND r.isApproved = true")
    Double getAverageRatingByCenterId(@Param("centerId") Long centerId);

    long countByCenterIdAndIsApprovedTrue(Long centerId);

boolean existsByUserIdAndCenterId(Long userId, Long centerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.center.id = :centerId AND r.isApproved = true")
    long countApprovedReviewsByCenterId(@Param("centerId") Long centerId);

    Optional<Review> findByIdAndIsApprovedTrue(Long id);

}
