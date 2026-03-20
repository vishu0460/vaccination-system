package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String certificateNumber;

    @Column(nullable = false)
    private String vaccineName;

    @Column(nullable = false)
    private Integer doseNumber;

    @Column(nullable = false)
    private LocalDateTime vaccinationDate;

    @Column(columnDefinition = "TEXT")
    private String qrCodeData;

    @Column(name = "digital_verification_code")
    private String digitalVerificationCode;

    @Column(name = "next_dose_date")
    private LocalDateTime nextDoseDate;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    private String qrCode;

    @PrePersist
    protected void onCreate() {
        issuedAt = LocalDateTime.now();
    }
}
