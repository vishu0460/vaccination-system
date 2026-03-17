package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vaccination_centers")
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
    private Integer pincode;

    private Double lat;

    private Double lng;

    private String phone;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String description;
}
