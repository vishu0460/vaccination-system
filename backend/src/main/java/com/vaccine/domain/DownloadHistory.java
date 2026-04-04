package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "download_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DownloadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;

    @Enumerated(EnumType.STRING)
    @Column(name = "download_type", nullable = false, length = 20)
    private DownloadType downloadType;

    @Column(name = "downloaded_at", nullable = false)
    private LocalDateTime downloadedAt;

    @PrePersist
    protected void onCreate() {
        if (downloadedAt == null) {
            downloadedAt = LocalDateTime.now();
        }
    }
}
