package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {
    boolean existsBySlotIdAndUserIdAndStatus(Long slotId, Long userId, String status);

    List<WaitlistEntry> findByUserEmailOrderByCreatedAtDesc(String email);

    long countBySlotIdAndStatus(Long slotId, String status);

    Optional<WaitlistEntry> findFirstBySlotIdAndStatusOrderByCreatedAtAsc(Long slotId, String status);
}
