package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.BookingRequest;
import com.vaccine.common.dto.BookingResponse;
import com.vaccine.common.dto.NotificationResponse;
import com.vaccine.common.dto.WaitlistEntryResponse;
import com.vaccine.domain.Booking;
import com.vaccine.domain.Slot;
import com.vaccine.domain.User;
import com.vaccine.core.service.BookingService;
import com.vaccine.core.service.ProfileService;
import com.vaccine.core.service.UserService;
import com.vaccine.core.service.WaitlistService;
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
@RequestMapping({"/user", "/api/user"})
@PreAuthorize("isAuthenticated()")
public class UserController {
    private final BookingService bookingService;
    private final ProfileService profileService;
    private final UserService userService;
    private final WaitlistService waitlistService;

    public UserController(BookingService bookingService, ProfileService profileService, UserService userService, WaitlistService waitlistService) {
        this.bookingService = bookingService;
        this.profileService = profileService;
        this.userService = userService;
        this.waitlistService = waitlistService;
    }

    @PostMapping("/bookings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BookingResponse>> book(@Valid @RequestBody BookingRequest req, Authentication auth) {
        log.info("Booking slot for user={}, slotId={}", auth.getName(), req.slotId());
        Booking booking = bookingService.book(auth.getName(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(BookingResponse.from(booking), "Booking created successfully"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookings(Authentication auth) {
        log.info("Get bookings for user={}", auth.getName());
        List<BookingResponse> bookings = bookingService.getMyBookings(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(@PathVariable Long bookingId, Authentication auth) {
        log.info("Cancel booking {} for user={}", bookingId, auth.getName());
        Booking booking = bookingService.cancel(auth.getName(), bookingId);
        return ResponseEntity.ok(ApiResponse.success(BookingResponse.from(booking), "Booking cancelled"));
    }

    @PatchMapping("/bookings/{bookingId}/reschedule")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BookingResponse>> reschedule(@PathVariable Long bookingId,
                                              @Valid @RequestBody BookingRequest req,
                                              Authentication auth) {
        log.info("Reschedule booking {} to slot {} for user={}", bookingId, req.slotId(), auth.getName());
        Booking booking = bookingService.reschedule(auth.getName(), bookingId, req);
        return ResponseEntity.ok(ApiResponse.success(BookingResponse.from(booking), "Booking rescheduled"));
    }

    @GetMapping("/recommendations/slots")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<Slot>>> recommendSlots(Authentication auth,
                                                     @RequestParam(required = false) String city,
                                                     @RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        log.info("Recommend slots for user={}, city={}, limit={}", auth.getName(), city, safeLimit);
        List<Slot> slots = bookingService.recommendSlots(auth.getName(), city, safeLimit);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> notifications(Authentication auth) {
        log.info("Get notifications for user={}", auth.getName());
        List<NotificationResponse> notifications = userService.getNotificationsByEmail(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @PatchMapping("/notifications/read-all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> markNotificationsRead(Authentication auth) {
        log.info("Mark notifications as read for user={}", auth.getName());
        userService.markNotificationsRead(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Notifications marked as read"));
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

    @PostMapping("/slots/{slotId}/waitlist")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<WaitlistEntryResponse>> joinWaitlist(@PathVariable Long slotId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(waitlistService.joinWaitlist(auth.getName(), slotId), "Joined waitlist successfully"));
    }

    @GetMapping("/waitlist")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<WaitlistEntryResponse>>> getWaitlist(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(waitlistService.getUserWaitlist(auth.getName())));
    }
}
