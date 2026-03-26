package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.DriveSubscription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriveSubscriptionRepository extends JpaRepository<DriveSubscription, Long> {
    boolean existsByUserEmailAndDriveId(String userEmail, Long driveId);

    void deleteByUserEmailAndDriveId(String userEmail, Long driveId);

    List<DriveSubscription> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    List<DriveSubscription> findByDriveId(Long driveId);
}
