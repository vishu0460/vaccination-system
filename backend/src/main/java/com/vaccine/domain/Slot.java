package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;

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

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer bookedCount;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

    @PrePersist
    protected void onCreate() {
        bookedCount = 0;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
