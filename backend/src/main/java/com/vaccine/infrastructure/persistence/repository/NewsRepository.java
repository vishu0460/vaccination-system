package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {\n\n    long countByActiveTrue();\n}

