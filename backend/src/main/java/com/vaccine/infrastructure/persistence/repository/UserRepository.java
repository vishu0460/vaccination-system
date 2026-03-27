package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationToken(String verificationToken);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM users WHERE lower(email) = lower(:email)", nativeQuery = true)
    boolean existsAnyByEmail(@Param("email") String email);
    long countByEnabledTrue();
    List<User> findByDobIsNotNull();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countUsersSince(LocalDateTime since);
}
