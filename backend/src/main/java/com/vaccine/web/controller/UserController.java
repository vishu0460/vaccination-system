package com.vaccine.web.controller;

import com.vaccine.common.dto.BookingRequest;
import com.vaccine.domain.Booking;
import com.vaccine.domain.Slot;
import com.vaccine.domain.User;
import com.vaccine.core.service.BookingService;
import com.vaccine.core.service.ProfileService;
import com.vaccine.core.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
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
    public ResponseEntity<Booking> book(@Valid @RequestBody BookingRequest req, Authentication auth) {
        return ResponseEntity.ok(bookingService.book(auth.getName(), req));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> myBookings(Authentication auth) {
return ResponseEntity.ok(bookingService.getBookingsByEmail(auth.getName()));
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<Booking> cancel(@PathVariable Long bookingId, Authentication auth) {
        return ResponseEntity.ok(bookingService.cancel(auth.getName(), bookingId));
    }

    @PatchMapping("/bookings/{bookingId}/reschedule")
    public ResponseEntity<Booking> reschedule(@PathVariable Long bookingId,
                                              @Valid @RequestBody BookingRequest req,
                                              Authentication auth) {
        return ResponseEntity.ok(bookingService.reschedule(auth.getName(), bookingId, req));
    }

    @GetMapping("/recommendations/slots")
    public ResponseEntity<List<Slot>> recommendSlots(Authentication auth,
                                                     @RequestParam(required = false) String city,
                                                     @RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return ResponseEntity.ok(bookingService.recommendSlots(auth.getName(), city, safeLimit));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> notifications(Authentication auth) {
        return ResponseEntity.ok(userService.getNotificationsByEmail(auth.getName()));
    }

    @GetMapping("/account")
    public ResponseEntity<Map<String, Object>> getAccount(Authentication auth) {
        User user = profileService.getProfile(auth.getName());
        Map<String, Object> profile = Map.of(
            "fullName", user.getFullName(),
            "email", user.getEmail(),
            "age", user.getAge(),
            "emailVerified", user.getEmailVerified()
        );
        return ResponseEntity.ok(profile);
    }
}
