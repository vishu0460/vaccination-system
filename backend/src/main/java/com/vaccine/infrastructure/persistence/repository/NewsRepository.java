package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    long countByActiveTrue();
    
    @Query("SELECT n FROM News n WHERE n.active = true AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    Page<News> findActiveNews(@Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT n FROM News n WHERE n.publishedAt IS NOT NULL AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    Page<News> findByPublishedTrueAndExpiresAtIsNullOrExpiresAtAfter(@Param("now") LocalDateTime now, Pageable pageable);

    Page<News> findByAdminId(Long adminId, Pageable pageable);
}
