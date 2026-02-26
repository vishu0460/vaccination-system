package com.vaccine.controller;

import com.vaccine.dto.*;
import com.vaccine.entity.User;
import com.vaccine.repository.UserRepository;
import com.vaccine.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private CenterService centerService;
    
    @Autowired
    private DriveService driveService;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        List<BookingDTO> pendingBookings = bookingService.getPendingBookings();
        dashboard.put("pendingBookings", pendingBookings.size());
        dashboard.put("recentPendingBookings", pendingBookings);
        
        return ResponseEntity.ok(dashboard);
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(this::mapUserToDTO)
                .toList());
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(mapUserToDTO(user));
    }
    
    @PutMapping("/users/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        if (dto.getIsActive() != null) {
            user.setIsActive(dto.getIsActive());
        }
        
        user = userRepository.save(user);
        return ResponseEntity.ok(mapUserToDTO(user));
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/centers")
    public ResponseEntity<CenterDTO> createCenter(@RequestBody CenterDTO dto) {
        return ResponseEntity.ok(centerService.createCenter(dto));
    }
    
    @PutMapping("/centers/{id}")
    public ResponseEntity<CenterDTO> updateCenter(@PathVariable Long id, @RequestBody CenterDTO dto) {
        return ResponseEntity.ok(centerService.updateCenter(id, dto));
    }
    
    @DeleteMapping("/centers/{id}")
    public ResponseEntity<Void> deleteCenter(@PathVariable Long id) {
        centerService.deleteCenter(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/drives")
    public ResponseEntity<DriveDTO> createDrive(@RequestBody DriveDTO dto) {
        return ResponseEntity.ok(driveService.createDrive(dto));
    }
    
    @PutMapping("/drives/{id}")
    public ResponseEntity<DriveDTO> updateDrive(@PathVariable Long id, @RequestBody DriveDTO dto) {
        return ResponseEntity.ok(driveService.updateDrive(id, dto));
    }
    
    @DeleteMapping("/drives/{id}")
    public ResponseEntity<Void> deleteDrive(@PathVariable Long id) {
        driveService.deleteDrive(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/bookings")
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }
    
    @GetMapping("/bookings/pending")
    public ResponseEntity<List<BookingDTO>> getPendingBookings() {
        return ResponseEntity.ok(bookingService.getPendingBookings());
    }
    
    private UserDTO mapUserToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setAadharNumber(user.getAadharNumber());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setGender(user.getGender() != null ? user.getGender().name() : null);
        dto.setIsActive(user.getIsActive());
        dto.setIsVerified(user.getIsVerified());
        dto.setRoles(user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet()));
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
