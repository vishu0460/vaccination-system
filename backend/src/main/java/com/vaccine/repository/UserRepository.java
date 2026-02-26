package com.vaccine.repository;

import com.vaccine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<User> findByIsActiveTrue();
    
    @Query("SELECT u FROM User u WHERE u.isVerified = false AND u.verificationToken IS NOT NULL")
    List<User> findUnverifiedUsers();
    
    @Query("SELECT u FROM User u WHERE u.lockoutTime IS NOT NULL AND u.lockoutTime > :now")
    List<User> findLockedUsers(LocalDateTime now);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();
    
    Optional<User> findByPasswordResetToken(String token);
}
