package com.vaccine.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "verification_token", unique = true, length = 120)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private LocalDateTime verificationTokenExpiry;

    @Column(name = "reset_otp", length = 255)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String resetOtp;

    @Column(name = "otp_expiry")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private LocalDateTime otpExpiry;

    @Column(name = "otp_hash", length = 255)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String otpHash;

    @Column(name = "otp_attempts", nullable = false)
    @Builder.Default
    private Integer otpAttempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_purpose", length = 50)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private OtpPurpose otpPurpose;

    @Column(name = "otp_blocked_until")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private LocalDateTime otpBlockedUntil;

    @Column(name = "otp_request_window_start")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private LocalDateTime otpRequestWindowStart;

    @Column(name = "otp_request_count", nullable = false)
    @Builder.Default
    private Integer otpRequestCount = 0;

    @Column(name = "otp_last_sent_at")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private LocalDateTime otpLastSentAt;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    // Phone number fields
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private Boolean phoneVerified = false;

    // Two-factor authentication fields
    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(name = "two_factor_secret")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String twoFactorSecret;

    // Admin role flags
    @Column(name = "is_super_admin", nullable = false)
    @Builder.Default
    private Boolean isSuperAdmin = false;

    @Column(name = "is_admin", nullable = false)
    @Builder.Default
    private Boolean isAdmin = false;

    @Column(name = "role", length = 32)
    private String role;

    @Column(name = "created_by")
    private Long createdBy;

    // Address fields
    @Column(name = "address")
    private String address;

    @Column(name = "user_city")
    private String userCity;

    @Column(name = "user_state")
    private String userState;

    @Column(name = "user_pincode", length = 10)
    private String userPincode;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
@com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Role> roles;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enabled == null) enabled = true;
        if (emailVerified == null) emailVerified = false;
        if (phoneVerified == null) phoneVerified = false;
        if (twoFactorEnabled == null) twoFactorEnabled = false;
        if (isSuperAdmin == null) isSuperAdmin = false;
        if (isAdmin == null) isAdmin = false;
        if (failedLoginAttempts == null) failedLoginAttempts = 0;
        if (otpAttempts == null) otpAttempts = 0;
        if (otpRequestCount == null) otpRequestCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    // Helper methods
public boolean hasRole(RoleName roleName) {
        return roles != null && roles.stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public boolean isSuperAdmin() {
        return hasRole(RoleName.SUPER_ADMIN) || Boolean.TRUE.equals(isSuperAdmin);
    }

    public boolean isAdmin() {
        return hasRole(RoleName.ADMIN) || Boolean.TRUE.equals(isAdmin);
    }

    public String getEffectiveRole() {
        if (role != null && !role.isBlank()) {
            return role.trim();
        }
        if (isSuperAdmin()) {
            return RoleName.SUPER_ADMIN.name();
        }
        if (isAdmin()) {
            return RoleName.ADMIN.name();
        }
        return RoleName.USER.name();
    }
}
