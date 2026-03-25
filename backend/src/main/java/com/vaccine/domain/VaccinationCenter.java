package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "vaccination_centers")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccinationCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    private String city;

    private String state;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "latitude")
    private Double lat;

    @Column(name = "longitude")
    private Double lng;

    private String phone;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "working_hours")
    private String workingHours;

    private Integer dailyCapacity;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
