package com.vaccine.web.controller;

import com.vaccine.common.dto.*;
import com.vaccine.core.service.AdminService;
import com.vaccine.domain.Booking;
import com.vaccine.domain.BookingStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;
    private final FeedbackService feedbackService;
    private final ContactService contactService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/bookings")
    public ResponseEntity<Map<String, Object>> getAllBookings(
            @RequestParam(defaultValue = \"0\") int page,
            @RequestParam(defaultValue = \"20\") int size,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(adminService.getAllBookings(PageRequest.of(page, size), status, city));
    }

    @PatchMapping("/bookings/{bookingId}/{action}")
    public ResponseEntity<Booking> updateBookingStatus(@PathVariable Long bookingId, @PathVariable String action, HttpServletRequest request) {
        return ResponseEntity.ok(adminService.updateBookingStatus(bookingId, action, request));
    }

    @GetMapping("/centers")
    public ResponseEntity<Map<String, Object>> getAllCenters(
            @RequestParam(defaultValue = \"0\") int page,
            @RequestParam(defaultValue = \"10\") int size) {
        return ResponseEntity.ok(adminService.getAllCenters(PageRequest.of(page, size)));
    }

    @PostMapping("/centers")
    public ResponseEntity<Map<String, Object>> createCenter(@Valid @RequestBody CenterRequest req) {
        return ResponseEntity.ok(adminService.createCenter(req));
    }

    @GetMapping("/drives")
    public ResponseEntity<Map<String, Object>> getAllDrives(
            @RequestParam(defaultValue = \"0\") int page,
            @RequestParam(defaultValue = \"10\") int size) {
        return ResponseEntity.ok(adminService.getAllDrives(PageRequest.of(page, size)));
    }

    @PostMapping("/drives")
    public ResponseEntity<Map<String, Object>> createDrive(@Valid @RequestBody DriveRequest req) {
        return ResponseEntity.ok(adminService.createDrive(req));
    }

    @PostMapping("/slots")
    public ResponseEntity<Map<String, Object>> createSlot(@Valid @RequestBody SlotRequest req) {
        return ResponseEntity.ok(adminService.createSlot(req));
    }

    @GetMapping("/drives/{driveId}/slots")
    public ResponseEntity<Map<String, Object>> getDriveSlots(@PathVariable Long driveId) {
        return ResponseEntity.ok(adminService.getDriveSlots(driveId));
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = \"0\") int page,
            @RequestParam(defaultValue = \"10\") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(PageRequest.of(page, size)));
    }

    @PatchMapping("/users/{userId}/enable")
    public ResponseEntity<Map<String, Object>> enableUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.enableUser(userId));
    }

    @PatchMapping("/users/{userId}/disable")
    public ResponseEntity<Map<String, Object>> disableUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.disableUser(userId));
    }

    @GetMapping("/feedback")
    public ResponseEntity<Map<String, Object>> getAllFeedback(
            @RequestParam(defaultValue = \"0\") int page,
            @RequestParam(defaultValue = \"10\") int size) {
        return ResponseEntity.ok(adminService.getAllFeedback(PageRequest.of(page, size)));
    }

    @PatchMapping("/feedback/{feedbackId}/respond")
    public ResponseEntity<Map<String, Object>> respondToFeedback(@PathVariable Long feedbackId, @RequestBody Map<String, String> response) {
        return ResponseEntity.ok(adminService.respondToFeedback(feedbackId, response.get(\"response\")));
    }

    @GetMapping("/contacts")
    public ResponseEntity<Map<String, Object>> getAllContacts(
            @RequestParam(defaultValue = \"0\") int page,
            @RequestParam(defaultValue = \"10\") int size) {
        return ResponseEntity.ok(adminService.getAllContacts(PageRequest.of(page, size)));
    }

    @PatchMapping("/contacts/{contactId}/respond")
    public ResponseEntity<Map<String, Object>> respondToContact(@PathVariable Long contactId, @RequestBody Map<String, String> response) {
        return ResponseEntity.ok(adminService.respondToContact(contactId, response.get(\"response\")));
    }

    @DeleteMapping("/contacts/{contactId}")
    public ResponseEntity<Map<String, Object>> deleteContact(@PathVariable Long contactId) {
        adminService.deleteContact(contactId);
        return ResponseEntity.ok(Map.of(\"message\", \"Contact deleted successfully\"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = \"0\") int page,
            @RequestParam(defaultValue = \"20\") int size) {
        return ResponseEntity.ok(adminService.getAuditLogs(PageRequest.of(page, size)));
    }

    @GetMapping("/bookings/export")
    public ResponseEntity<Map<String, Object>> exportBookings() {
        return ResponseEntity.ok(adminService.exportBookingsToCsv());
    }
}
