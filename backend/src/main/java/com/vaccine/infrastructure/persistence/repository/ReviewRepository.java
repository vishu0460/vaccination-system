package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByCenterIdAndApprovedTrueOrderByCreatedAtDesc(Long centerId);
    
    @Query("SELECT r FROM Review r WHERE r.center.id = :centerId AND r.approved = true")
    List<Review> findApprovedByCenterId(@Param("centerId") Long centerId);

    Optional<Review> findByIdAndApprovedTrue(Long id);
    
    Double getAverageRatingByCenterId(Long centerId);
}

