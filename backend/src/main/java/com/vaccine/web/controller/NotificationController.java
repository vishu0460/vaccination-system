package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.NotificationResponse;
import com.vaccine.core.service.INotificationService;
import com.vaccine.core.service.SlotNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Validated
@RequestMapping({"/v1/notifications", "/notifications"})
@PreAuthorize("isAuthenticated()")
public class NotificationController {
    private final SlotNotificationService slotNotificationService;
    private final INotificationService notificationService;

    public NotificationController(SlotNotificationService slotNotificationService, INotificationService notificationService) {
        this.slotNotificationService = slotNotificationService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsForUser(auth.getName())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication auth) {
        long unreadCount = notificationService.getUnreadCount(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", unreadCount)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long notificationId, Authentication auth) {
        notificationService.markNotificationRead(auth.getName(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(Authentication auth) {
        notificationService.markNotificationsRead(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Notifications marked as read"));
    }

    @PostMapping("/slots/subscribe/{driveId}")
    public ResponseEntity<ApiResponse<Void>> subscribeToSlot(@PathVariable Long driveId, Authentication auth) {
        slotNotificationService.subscribe(auth.getName(), driveId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "Subscribed to slot notifications"));
    }

    @PostMapping("/slots/unsubscribe/{driveId}")
    public ResponseEntity<ApiResponse<Void>> unsubscribeFromSlot(@PathVariable Long driveId, Authentication auth) {
        slotNotificationService.unsubscribe(auth.getName(), driveId);
        return ResponseEntity.ok(ApiResponse.success(null, "Unsubscribed from slot notifications"));
    }

    @GetMapping("/slots/subscriptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubscriptions(Authentication auth) {
        List<Map<String, Object>> subscriptions = slotNotificationService.getUserSubscriptions(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(subscriptions));
    }
}
