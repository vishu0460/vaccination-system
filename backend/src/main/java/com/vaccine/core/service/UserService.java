package com.vaccine.core.service;

import com.vaccine.common.dto.NotificationResponse;
import com.vaccine.domain.Role;
import com.vaccine.domain.RoleName;
import com.vaccine.domain.Notification;
import com.vaccine.domain.User;
import com.vaccine.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.NotificationRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<NotificationResponse> getNotificationsByEmail(String email) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(email).stream()
            .map(this::toNotificationResponse)
            .toList();
    }

    public void markNotificationsRead(String email) {
        List<Notification> unreadNotifications = notificationRepository.findByUserEmailAndIsReadFalseOrderByCreatedAtDesc(email);
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    // Add other user service methods as needed

    private NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getTitle(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getReplyMessage(),
            notification.getStatus(),
            notification.getCreatedAt(),
            Boolean.TRUE.equals(notification.getIsRead())
        );
    }
}
