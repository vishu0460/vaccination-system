package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs", indexes = {
    @Index(name = "idx_search_logs_normalized_query", columnList = "normalized_query"),
    @Index(name = "idx_search_logs_city", columnList = "city"),
    @Index(name = "idx_search_logs_searched_at", columnList = "searched_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String query;

    @Column(name = "normalized_query", nullable = false, length = 120)
    private String normalizedQuery;

    @Column(length = 120)
    private String city;

    @Column(name = "detected_city", length = 120)
    private String detectedCity;

    @Column(length = 40)
    private String source;

    @Column(name = "result_count", nullable = false)
    private Integer resultCount;

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;
}
