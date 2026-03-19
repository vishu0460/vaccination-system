package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.BookingRequest;
import com.vaccine.common.dto.NotificationResponse;
import com.vaccine.domain.Booking;
import com.vaccine.domain.Slot;
import com.vaccine.domain.User;
import com.vaccine.core.service.BookingService;
import com.vaccine.core.service.ProfileService;
import com.vaccine.core.service.UserService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/user")
@PreAuthorize("isAuthenticated()")
public class UserController {
    private final BookingService bookingService;
    private final ProfileService profileService;
    private final UserService userService;

    public UserController(BookingService bookingService, ProfileService profileService, UserService userService) {
        this.bookingService = bookingService;
        this.profileService = profileService;
        this.userService = userService;
    }

    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<Booking>> book(@Valid @RequestBody BookingRequest req, Authentication auth) {
        log.info("Booking slot for user={}, slotId={}", auth.getName(), req.slotId());
        Booking booking = bookingService.book(auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(booking, "Booking created successfully"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<Booking>>> myBookings(Authentication auth) {
        log.info("Get bookings for user={}", auth.getName());
        List<Booking> bookings = bookingService.getMyBookings(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancel(@PathVariable Long bookingId, Authentication auth) {
        log.info("Cancel booking {} for user={}", bookingId, auth.getName());
        Booking booking = bookingService.cancel(auth.getName(), bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking cancelled"));
    }

    @PatchMapping("/bookings/{bookingId}/reschedule")
    public ResponseEntity<ApiResponse<Booking>> reschedule(@PathVariable Long bookingId,
                                              @Valid @RequestBody BookingRequest req,
                                              Authentication auth) {
        log.info("Reschedule booking {} to slot {} for user={}", bookingId, req.slotId(), auth.getName());
        Booking booking = bookingService.reschedule(auth.getName(), bookingId, req);
        return ResponseEntity.ok(ApiResponse.success(booking, "Booking rescheduled"));
    }

    @GetMapping("/recommendations/slots")
    public ResponseEntity<ApiResponse<List<Slot>>> recommendSlots(Authentication auth,
                                                     @RequestParam(required = false) String city,
                                                     @RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        log.info("Recommend slots for user={}, city={}, limit={}", auth.getName(), city, safeLimit);
        List<Slot> slots = bookingService.recommendSlots(auth.getName(), city, safeLimit);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> notifications(Authentication auth) {
        log.info("Get notifications for user={}", auth.getName());
        List<NotificationResponse> notifications = userService.getNotificationsByEmail(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/account")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccount(Authentication auth) {
        log.info("Get account info for user={}", auth.getName());
        User user = profileService.getProfile(auth.getName());
        Map<String, Object> profile = Map.of(
            "fullName", user.getFullName(),
            "email", user.getEmail(),
            "age", user.getAge(),
            "emailVerified", user.getEmailVerified()
        );
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
