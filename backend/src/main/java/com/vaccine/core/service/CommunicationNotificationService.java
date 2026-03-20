package com.vaccine.core.service;

import com.vaccine.domain.Notification;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunicationNotificationService {
    private final NotificationRepository notificationRepository;

    public void notifyReply(User user, String type, String title, String sourceMessage, String replyMessage, Long referenceId) {
        if (user == null) {
            return;
        }

        Notification notification = Notification.builder()
            .user(user)
            .title(title)
            .type(type)
            .message(sourceMessage)
            .replyMessage(replyMessage)
            .status("REPLIED")
            .referenceId(referenceId)
            .isRead(false)
            .build();

        notificationRepository.save(notification);
    }
}
