package com.vaccine.core.service;

import com.vaccine.common.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRealtimeService {
    private final SimpMessagingTemplate messagingTemplate;

    public void pushNotification(String userEmail, NotificationResponse notification, long unreadCount) {
        if (userEmail == null || userEmail.isBlank() || notification == null) {
            return;
        }

        try {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications", notification);
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications/unread", Map.of("unreadCount", unreadCount));
        } catch (Exception ex) {
            log.warn("Failed to push websocket notification to {}: {}", userEmail, ex.getMessage());
        }
    }

    public void pushUnreadCount(String userEmail, long unreadCount) {
        if (userEmail == null || userEmail.isBlank()) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications/unread", Map.of("unreadCount", unreadCount));
        } catch (Exception ex) {
            log.warn("Failed to push unread count to {}: {}", userEmail, ex.getMessage());
        }
    }
}
