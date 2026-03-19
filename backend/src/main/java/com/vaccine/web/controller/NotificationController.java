package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
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
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {
    private final SlotNotificationService slotNotificationService;

    public NotificationController(SlotNotificationService slotNotificationService) {
        this.slotNotificationService = slotNotificationService;
    }

    @PostMapping("/slots/subscribe/{driveId}")
    public ResponseEntity<ApiResponse<Void>> subscribeToSlot(@PathVariable Long driveId, Authentication auth) {
        log.info("User {} subscribing to drive {}", auth.getName(), driveId);
        slotNotificationService.subscribe(auth.getName(), driveId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "Subscribed to slot notifications"));
    }

    @PostMapping("/slots/unsubscribe/{driveId}")
    public ResponseEntity<ApiResponse<Void>> unsubscribeFromSlot(@PathVariable Long driveId, Authentication auth) {
        log.info("User {} unsubscribing from drive {}", auth.getName(), driveId);
        slotNotificationService.unsubscribe(auth.getName(), driveId);
        return ResponseEntity.ok(ApiResponse.success(null, "Unsubscribed from slot notifications"));
    }

    @GetMapping("/slots/subscriptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubscriptions(Authentication auth) {
        log.info("Get subscriptions for user {}", auth.getName());
        List<Map<String, Object>> subscriptions = slotNotificationService.getUserSubscriptions(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(subscriptions));
    }
}
