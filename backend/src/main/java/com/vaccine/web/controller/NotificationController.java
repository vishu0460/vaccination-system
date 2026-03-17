package com.vaccine.web.controller;

import com.vaccine.core.model.SlotNotification;
import com.vaccine.core.service.SlotNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final SlotNotificationService slotNotificationService;

    public NotificationController(SlotNotificationService slotNotificationService) {
        this.slotNotificationService = slotNotificationService;
    }

    @PostMapping("/slots/subscribe/{driveId}")
    public ResponseEntity<Map<String, String>> subscribeToSlot(@PathVariable Long driveId, Authentication auth) {
        slotNotificationService.subscribe(auth.getName(), driveId);
        return ResponseEntity.ok(Map.of("message", "Subscribed to slot notifications"));
    }

    @PostMapping("/slots/unsubscribe/{driveId}")
    public ResponseEntity<Map<String, String>> unsubscribeFromSlot(@PathVariable Long driveId, Authentication auth) {
        slotNotificationService.unsubscribe(auth.getName(), driveId);
        return ResponseEntity.ok(Map.of("message", "Unsubscribed from slot notifications"));
    }

    @GetMapping("/slots/subscriptions")
    public ResponseEntity<List<SlotNotification>> getSubscriptions(Authentication auth) {
        return ResponseEntity.ok(slotNotificationService.getUserSubscriptions(auth.getName()));
    }
}
