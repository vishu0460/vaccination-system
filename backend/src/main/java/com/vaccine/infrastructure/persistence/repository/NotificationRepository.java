package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Notification;
import com.vaccine.domain.NotificationDeliveryStatus;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserEmailOrderByCreatedAtDesc(String email);
    List<Notification> findByUserEmailAndIsReadFalseOrderByCreatedAtDesc(String email);
    long countByUserEmailAndIsReadFalse(String email);
    Optional<Notification> findByIdAndUserEmail(Long id, String email);
    boolean existsByDedupeKey(String dedupeKey);
    Optional<Notification> findByDedupeKey(String dedupeKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT n FROM Notification n
        WHERE n.deliveryStatus IN :statuses
          AND n.nextAttemptAt <= :now
        ORDER BY n.nextAttemptAt ASC, n.createdAt ASC
    """)
    List<Notification> findDueForDispatch(@Param("statuses") List<NotificationDeliveryStatus> statuses,
                                          @Param("now") LocalDateTime now,
                                          Pageable pageable);
}
