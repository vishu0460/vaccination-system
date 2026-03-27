package com.vaccine.web.controller;

import com.vaccine.common.dto.*;
import com.vaccine.core.service.AdminService;
import com.vaccine.core.service.AuditService;
import com.vaccine.core.service.ContactService;
import com.vaccine.core.service.FeedbackService;
import com.vaccine.domain.Booking;
import com.vaccine.domain.BookingStatus;
import com.vaccine.domain.SlotStatus;
import com.vaccine.domain.Slot;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/admin", "/api/admin"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;
    private final FeedbackService feedbackService;
    private final ContactService contactService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    @GetMapping("/dashboard/analytics")
    public ResponseEntity<ApiResponse<DashboardAnalyticsResponse>> getDashboardAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardAnalytics()));
    }

    @GetMapping("/search-analytics")
    public ResponseEntity<ApiResponse<SearchAnalyticsResponse>> getSearchAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSearchAnalytics()));
    }

    @GetMapping("/bookings")
    public ResponseEntity<Map<String, Object>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(adminService.getAllBookings(PageRequest.of(page, size), status, city));
    }

    @PatchMapping("/bookings/{bookingId}/{action}")
    public ResponseEntity<BookingResponse> updateBookingStatus(@PathVariable Long bookingId, @PathVariable String action, HttpServletRequest request) {
        return ResponseEntity.ok(adminService.updateBookingStatus(bookingId, action, request));
    }

    @PutMapping({"/booking/{bookingId}/complete", "/bookings/{bookingId}/complete"})
    public ResponseEntity<BookingResponse> completeBooking(@PathVariable Long bookingId, HttpServletRequest request) {
        return ResponseEntity.ok(adminService.completeBookingResponse(bookingId, request));
    }

    @DeleteMapping({"/booking/{bookingId}", "/bookings/{bookingId}"})
    public ResponseEntity<Map<String, Object>> deleteBooking(@PathVariable Long bookingId, HttpServletRequest request) {
        adminService.deleteBooking(bookingId, request);
        return ResponseEntity.ok(Map.of("message", "Booking deleted successfully"));
    }

    @GetMapping("/centers")
    public ResponseEntity<Map<String, Object>> getAllCenters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllCentersPaginated(PageRequest.of(page, size)));
    }

    @PostMapping("/centers")
    public ResponseEntity<VaccinationCenter> createCenter(@Valid @RequestBody CenterRequest req) {
        return ResponseEntity.ok(adminService.createCenter(req));
    }

    @PutMapping({"/centers/{centerId}", "/center/{centerId}"})
    public ResponseEntity<VaccinationCenter> updateCenter(@PathVariable Long centerId, @Valid @RequestBody CenterRequest req) {
        return ResponseEntity.ok(adminService.updateCenter(centerId, req));
    }

    @DeleteMapping({"/centers/{centerId}", "/center/{centerId}"})
    public ResponseEntity<Map<String, Object>> deleteCenter(@PathVariable Long centerId, HttpServletRequest request) {
        adminService.deleteCenter(centerId, request);
        return ResponseEntity.ok(Map.of("message", "Center deleted successfully"));
    }

    @GetMapping("/drives")
    public ResponseEntity<Map<String, Object>> getAllDrives(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllDrives(PageRequest.of(page, size)));
    }

    @PostMapping("/drives")
    public ResponseEntity<VaccinationDrive> createDrive(@Valid @RequestBody DriveRequest req) {
        return ResponseEntity.ok(adminService.createDrive(req));
    }

    @PutMapping({"/drives/{driveId}", "/drive/{driveId}"})
    public ResponseEntity<VaccinationDrive> updateDrive(@PathVariable Long driveId, @Valid @RequestBody DriveRequest req) {
        return ResponseEntity.ok(adminService.updateDrive(driveId, req));
    }

    @DeleteMapping("/drives/{driveId}")
    public ResponseEntity<Map<String, Object>> deleteDrive(@PathVariable Long driveId, HttpServletRequest request) {
        adminService.deleteDrive(driveId, request);
        return ResponseEntity.ok(Map.of("message", "Drive deleted successfully"));
    }

    @PostMapping("/slots")
    public ResponseEntity<SlotDetailResponse> createSlot(@Valid @RequestBody SlotRequest req) {
        log.info("Create slot for driveId={} startDate={} endDate={}", req.getDriveId(), req.getStartDate(), req.getEndDate());
        Slot createdSlot = adminService.createSlot(req);
        return ResponseEntity.ok(SlotDetailResponse.from(createdSlot));
    }

    @PutMapping({"/slots/{slotId}", "/slot/{slotId}"})
    public ResponseEntity<SlotDetailResponse> updateSlot(@PathVariable Long slotId, @Valid @RequestBody SlotRequest req) {
        log.info("Update slot id={} driveId={} startDate={} endDate={}", slotId, req.getDriveId(), req.getStartDate(), req.getEndDate());
        Slot updatedSlot = adminService.updateSlot(slotId, req);
        return ResponseEntity.ok(SlotDetailResponse.from(updatedSlot));
    }

    @GetMapping("/drives/{driveId}/slots")
    public ResponseEntity<Map<String, Object>> getDriveSlots(@PathVariable Long driveId) {
        return ResponseEntity.ok(adminService.getDriveSlots(driveId));
    }

    @GetMapping("/slots")
    public ResponseEntity<Map<String, Object>> getAllSlots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SlotStatus status,
            @RequestParam(required = false) Long centerId,
            @RequestParam(required = false) Long driveId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.getAllSlots(PageRequest.of(page, size), status, centerId, driveId, date));
    }

    @GetMapping("/slots/all")
    public ResponseEntity<List<AdminSlotResponse>> getAllSlotsList(
            @RequestParam(required = false) SlotStatus status,
            @RequestParam(required = false) Long centerId,
            @RequestParam(required = false) Long driveId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.getAllSlots(status, centerId, driveId, date));
    }

    @DeleteMapping({"/slots/{slotId}", "/slot/{slotId}"})
    public ResponseEntity<Map<String, Object>> deleteSlot(@PathVariable Long slotId, HttpServletRequest request) {
        adminService.deleteSlot(slotId, request);
        return ResponseEntity.ok(Map.of("message", "Slot deleted successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllUsersPaginated(PageRequest.of(page, size)));
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllFeedback(PageRequest.of(page, size)));
    }

    @PatchMapping("/feedback/{feedbackId}/respond")
    public ResponseEntity<Map<String, Object>> respondToFeedback(@PathVariable Long feedbackId, @RequestBody Map<String, String> response) {
        return ResponseEntity.ok(adminService.respondToFeedback(feedbackId, response.get("response")));
    }

    @PutMapping("/feedback/{feedbackId}/reply")
    public ResponseEntity<Map<String, Object>> replyToFeedback(@PathVariable Long feedbackId, @RequestBody Map<String, String> response) {
        return ResponseEntity.ok(adminService.respondToFeedback(feedbackId, response.get("replyMessage")));
    }

    @GetMapping("/contacts")
    public ResponseEntity<Map<String, Object>> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllContacts(PageRequest.of(page, size)));
    }

    @PatchMapping("/contacts/{contactId}/respond")
    public ResponseEntity<Map<String, Object>> respondToContact(@PathVariable Long contactId, @RequestBody Map<String, String> response) {
        return ResponseEntity.ok(adminService.respondToContact(contactId, response.get("response")));
    }

    @PutMapping("/contact/{contactId}/reply")
    public ResponseEntity<Map<String, Object>> replyToContact(@PathVariable Long contactId, @RequestBody Map<String, String> response) {
        return ResponseEntity.ok(adminService.respondToContact(contactId, response.get("replyMessage")));
    }

    @DeleteMapping("/contacts/{contactId}")
    public ResponseEntity<Map<String, Object>> deleteContact(@PathVariable Long contactId) {
        adminService.deleteContact(contactId);
        return ResponseEntity.ok(Map.of("message", "Contact deleted successfully"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAuditLogs(PageRequest.of(page, size)));
    }

    @GetMapping(value = "/bookings/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportBookings() {
        String csv = adminService.exportBookingsToCsv();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bookings.csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
