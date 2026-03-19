package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

@Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "created_by_id")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active && publishedAt == null) publishedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (active && publishedAt == null) publishedAt = LocalDateTime.now();
    }
}
