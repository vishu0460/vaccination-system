package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;
import jakarta.persistence.Transient;

@Entity
@Table(name = "slots")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "drive_id")
    private VaccinationDrive drive;

    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (bookedCount == null) {
            bookedCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Transient
    public LocalDateTime getStartDateTime() {
        return dateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.dateTime = startDateTime;
        this.startTime = startDateTime != null ? startDateTime.toLocalTime() : null;
    }

    @Transient
    public LocalDateTime getEndDateTime() {
        if (dateTime == null) {
            return null;
        }
        if (endTime == null) {
            return dateTime;
        }

        LocalDateTime endDateTime = dateTime.toLocalDate().atTime(endTime);
        return endDateTime.isBefore(dateTime) ? endDateTime.plusDays(1) : endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endTime = endDateTime != null ? endDateTime.toLocalTime() : null;
    }
}
