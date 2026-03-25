package com.vaccine.core.service;

import com.vaccine.domain.NotificationType;
import com.vaccine.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunicationNotificationService {
    private final INotificationService notificationService;

    public void notifyReply(User user, String type, String title, String sourceMessage, String replyMessage, Long referenceId) {
        if (user == null) {
            return;
        }

        NotificationType notificationType = "CONTACT_REPLY".equalsIgnoreCase(type)
            ? NotificationType.CONTACT_REPLY
            : NotificationType.FEEDBACK_REPLY;

        notificationService.queueReplyNotification(
            user,
            notificationType,
            title,
            sourceMessage,
            replyMessage,
            referenceId,
            notificationType.name() + ":" + referenceId + ":reply"
        );
    }
}
