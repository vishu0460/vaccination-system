package com.vaccine.web.controller;

import com.vaccine.common.dto.*;
import com.vaccine.domain.Role;
import com.vaccine.domain.RoleName;
import com.vaccine.domain.User;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.RoleRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationCenterRepository;
import com.vaccine.core.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/super-admin")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SuperAdminController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VaccinationCenterRepository centerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public SuperAdminController(UserRepository userRepository,
                               RoleRepository roleRepository,
                               VaccinationCenterRepository centerRepository,
                               PasswordEncoder passwordEncoder,
                               AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.centerRepository = centerRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @PostMapping("/admins")
    public ResponseEntity<ApiMessage> createAdmin(@Valid @RequestBody AdminCreateRequest req, HttpServletRequest request) {
        if (userRepository.existsByEmail(req.email())) {
            throw new AppException("Email already registered");
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
            .orElseThrow(() -> new AppException("ADMIN role not found"));

        User admin = User.builder()
            .email(req.email().toLowerCase())
            .fullName(req.fullName())
            .password(passwordEncoder.encode(req.password()))
            .age(req.age())
            .phoneNumber(req.phoneNumber())
            .phoneVerified(false)
            .enabled(true)
            .emailVerified(true)
            .isAdmin(true)
            .isSuperAdmin(false)
            .roles(Set.of(adminRole))
            .createdBy(req.createdBy())
            .build();
        userRepository.save(admin);

        auditService.log(admin.getEmail(), "CREATE_ADMIN", "USER", "Admin created by super admin", request);
        return ResponseEntity.ok(new ApiMessage("Admin created successfully"));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<User>> allAdmins() {
        List<User> admins = userRepository.findAll().stream()
            .filter(User::isAdmin)
            .toList();
        return ResponseEntity.ok(admins);
    }

    @DeleteMapping("/admins/{adminId}")
    public ResponseEntity<ApiMessage> deleteAdmin(@PathVariable Long adminId, HttpServletRequest request) {
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new AppException("Admin not found"));

        if (!admin.isAdmin()) {
            throw new AppException("User is not an admin");
        }

        if (admin.getIsSuperAdmin()) {
            throw new AppException("Cannot delete super admin");
        }

        userRepository.delete(admin);
        auditService.log(admin.getEmail(), "DELETE_ADMIN", "USER", "Admin deleted by super admin", request);
        return ResponseEntity.ok(new ApiMessage("Admin deleted successfully"));
    }

    @GetMapping("/centers")
    public ResponseEntity<List<VaccinationCenter>> allCenters() {
        return ResponseEntity.ok(centerRepository.findAll());
    }

    @PostMapping("/centers")
    public ResponseEntity<VaccinationCenter> createCenter(@Valid @RequestBody CenterRequest req) {
        VaccinationCenter center = VaccinationCenter.builder()
            .name(req.name())
            .address(req.address())
            .city(req.city())
            .state(req.state())
            .pincode(req.pincode())
            .phone(req.phone())
            .email(req.email())
            .workingHours(req.workingHours())
            .dailyCapacity(req.dailyCapacity())
            .build();
        return ResponseEntity.ok(centerRepository.save(center));
    }

    @DeleteMapping("/centers/{centerId}")
    public ResponseEntity<ApiMessage> deleteCenter(@PathVariable Long centerId, HttpServletRequest request) {
        VaccinationCenter center = centerRepository.findById(centerId)
            .orElseThrow(() -> new AppException("Center not found"));
        centerRepository.delete(center);
        auditService.log("SYSTEM", "DELETE_CENTER", "CENTER", "Center deleted by super admin", request);
        return ResponseEntity.ok(new ApiMessage("Center deleted successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> allUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<ApiMessage> updateUserRole(@PathVariable Long userId, 
                                                      @RequestParam RoleName role,
                                                      HttpServletRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException("User not found"));

        Role newRole = roleRepository.findByName(role)
            .orElseThrow(() -> new AppException("Role not found"));

        user.setRoles(Set.of(newRole));
        
        if (role == RoleName.ADMIN) {
            user.setIsAdmin(true);
        } else if (role == RoleName.SUPER_ADMIN) {
            user.setIsSuperAdmin(true);
            user.setIsAdmin(true);
        } else {
            user.setIsAdmin(false);
            user.setIsSuperAdmin(false);
        }
        
        userRepository.save(user);
        auditService.log(user.getEmail(), "UPDATE_ROLE", "USER", "Role updated to " + role, request);
        return ResponseEntity.ok(new ApiMessage("Role updated successfully"));
    }
}

