package com.vaccine.web.controller;

import com.vaccine.common.dto.ApiMessage;
import com.vaccine.common.dto.ApiResponse;
import com.vaccine.common.dto.ContactAnalyticsResponse;
import com.vaccine.common.dto.ContactRequest;
import com.vaccine.domain.User;
import com.vaccine.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.core.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/contact", "/api/contact"})
public class ContactController {
    private final ContactService contactService;
    private final UserRepository userRepository;

    public ContactController(ContactService contactService, UserRepository userRepository) {
        this.contactService = contactService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ApiMessage> submitContact(@Valid @RequestBody ContactRequest request, Authentication authentication) {
        User user = null;
        if (authentication != null && authentication.getName() != null && !"anonymousUser".equals(authentication.getName())) {
            user = userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return ResponseEntity.ok(contactService.submitContact(request, user));
    }

    @GetMapping("/my-inquiries")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getMyInquiries(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AppException("User not found"));
        return ResponseEntity.ok(contactService.getUserInquiries(user));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserContactHistory(
            @PathVariable Long userId,
            Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new AppException("User not found"));

        boolean privilegedUser = currentUser.isAdmin() || currentUser.isSuperAdmin();
        if (!privilegedUser && !currentUser.getId().equals(userId)) {
            throw new AppException("You are not allowed to view this contact history");
        }

        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new AppException("User not found"));

        return ResponseEntity.ok(ApiResponse.success(contactService.getUserInquiries(targetUser)));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ContactAnalyticsResponse>> getContactAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(contactService.getContactAnalytics()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllContactsForAdmin() {
        return ResponseEntity.ok(contactService.getAllContacts());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllContacts() {
        return ResponseEntity.ok(contactService.getAllContacts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getContactById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getContactById(id));
    }

    @PatchMapping("/{id}/respond")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiMessage> respondToContact(@PathVariable Long id,
                                                         @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(contactService.respondToContact(id, request.get("response")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiMessage> deleteContact(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.deleteContact(id));
    }
}
