package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.DownloadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownloadHistoryRepository extends JpaRepository<DownloadHistory, Long> {
    List<DownloadHistory> findByUserIdOrderByDownloadedAtDesc(Long userId);
}
