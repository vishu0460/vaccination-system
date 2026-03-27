package com.vaccine.core.service;

import com.vaccine.common.dto.NotificationResponse;
import com.vaccine.domain.Role;
import com.vaccine.domain.RoleName;
import com.vaccine.domain.User;
import com.vaccine.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final INotificationService notificationService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersWithBirthday(LocalDate today) {
        if (today == null) {
            return List.of();
        }

        return userRepository.findByDobIsNotNull().stream()
            .filter(user -> user.getDob() != null)
            .filter(user -> user.getDob().getMonth() == today.getMonth() && user.getDob().getDayOfMonth() == today.getDayOfMonth())
            .toList();
    }

    public List<NotificationResponse> getNotificationsByEmail(String email) {
        return notificationService.getNotificationsForUser(email);
    }

    public void markNotificationsRead(String email) {
        notificationService.markNotificationsRead(email);
    }
}
