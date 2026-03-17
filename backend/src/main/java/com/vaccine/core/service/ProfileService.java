package com.vaccine.core.service;

import com.vaccine.dto.ProfileUpdateRequest;
import com.vaccine.core.model.User;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getProfile(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User not found"));
    }

    @Transactional
    public User updateProfile(String email, ProfileUpdateRequest request) {
        User user = getProfile(email);
        
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.age() != null) {
            if (request.age() < 1 || request.age() > 120) {
                throw new AppException("Age must be between 1 and 120");
            }
            user.setAge(request.age());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhoneNumber(request.phone());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.city() != null) {
            user.setUserCity(request.city());
        }
        if (request.state() != null) {
            user.setUserState(request.state());
        }
        if (request.pincode() != null) {
            user.setUserPincode(request.pincode());
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = getProfile(email);

        if (currentPassword == null || currentPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new AppException("Current and new password are required");
        }
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AppException("Current password is incorrect");
        }

        if (newPassword.length() < 8) {
            throw new AppException("New password must be at least 8 characters");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deactivateAccount(String email) {
        User user = getProfile(email);
        user.setEnabled(false);
        userRepository.save(user);
    }
}
