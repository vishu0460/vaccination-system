package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "bookings")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "slot_id")
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "booked_at")
    private LocalDateTime bookedAt;

    @Column(name = "assigned_time")
    private LocalDateTime assignedTime;

    @Column(length = 500)
    private String notes;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "dose_number", nullable = false)
    @Builder.Default
    private Integer doseNumber = 1;

    @Column(name = "first_dose_date")
    private LocalDateTime firstDoseDate;

    @Column(name = "next_dose_due_date")
    private LocalDateTime nextDoseDueDate;

    @Column(name = "second_dose_required", nullable = false)
    @Builder.Default
    private Boolean secondDoseRequired = false;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

    @PrePersist
    protected void onCreate() {
        bookedAt = LocalDateTime.now();
        if (doseNumber == null) {
            doseNumber = 1;
        }
        if (secondDoseRequired == null) {
            secondDoseRequired = false;
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
