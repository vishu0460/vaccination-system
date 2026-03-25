package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_notifications_due", columnList = "delivery_status, next_attempt_at"),
    @Index(name = "idx_notifications_read", columnList = "user_id, is_read"),
    @Index(name = "idx_notifications_dedupe", columnList = "dedupe_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "title")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "reply_message", columnDefinition = "TEXT")
    private String replyMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationReadStatus status = NotificationReadStatus.UNREAD;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    @Builder.Default
    private NotificationDeliveryStatus deliveryStatus = NotificationDeliveryStatus.PENDING;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "dedupe_key", length = 191, unique = true)
    private String dedupeKey;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (scheduledTime == null) {
            scheduledTime = now;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = scheduledTime;
        }
        if (status == null) {
            status = NotificationReadStatus.UNREAD;
        }
        if (deliveryStatus == null) {
            deliveryStatus = NotificationDeliveryStatus.PENDING;
        }
        if (isRead == null) {
            isRead = false;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
