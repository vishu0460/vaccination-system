package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
    List<SearchLog> findBySearchedAtAfter(LocalDateTime searchedAt);
}
