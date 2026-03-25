package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "vaccination_drives")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccinationDrive {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "center_id")
    private VaccinationCenter center;

    @OneToMany(mappedBy = "drive", cascade = CascadeType.ALL, orphanRemoval = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private List<Slot> slots = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String vaccineType;

    @Column(nullable = false)
    private LocalDate driveDate;

    @Column(nullable = false)
    private Integer minAge;

    @Column(nullable = false)
    private Integer maxAge;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer totalSlots;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.UPCOMING;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "second_dose_required", nullable = false)
    private Boolean secondDoseRequired = false;

    @Column(name = "second_dose_gap_days")
    private Integer secondDoseGapDays;

    private String description;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
