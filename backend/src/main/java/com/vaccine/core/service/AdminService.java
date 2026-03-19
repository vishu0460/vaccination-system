package com.vaccine.core.service;

import com.vaccine.common.dto.*;
import com.vaccine.domain.*;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationDriveRepository driveRepository;
    private final SlotRepository slotRepository;
    private final NewsRepository newsRepository;
    private final FeedbackRepository feedbackRepository;
    private final ContactRepository contactRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long approvedBookings = bookingRepository.countByStatus(BookingStatus.APPROVED);
        long rejectedBookings = bookingRepository.countByStatus(BookingStatus.REJECTED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long totalCenters = centerRepository.count();
        long totalDrives = driveRepository.count();
        long activeDrives = driveRepository.countByActiveTrue();
        long totalSlots = slotRepository.count();
        long availableSlots = slotRepository.countAvailableSlots();
        long totalNews = newsRepository.countByActiveTrue();
        
        LocalDateTime monthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        long newUsersThisMonth = userRepository.countUsersSince(monthStart);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long bookingsToday = bookingRepository.countBookingsSince(todayStart);
        long completedVaccinations = bookingRepository.countByStatus(BookingStatus.COMPLETED);

        return AdminDashboardStatsResponse.builder()
            .totalUsers(totalUsers)
            .totalBookings(totalBookings)
            .pendingBookings(pendingBookings)
            .approvedBookings(approvedBookings)
            .rejectedBookings(rejectedBookings)
            .cancelledBookings(cancelledBookings)
            .totalCenters(totalCenters)
            .totalDrives(totalDrives)
            .activeDrives(activeDrives)
            .totalSlots(totalSlots)
            .availableSlots(availableSlots)
            .newUsersThisMonth(newUsersThisMonth)
            .bookingsToday(bookingsToday)
            .totalNews(totalNews)
            .build();
    }

    public Map<String, Object> getAllBookings(PageRequest pageRequest, BookingStatus status, String city) {
        List<Booking> bookings = bookingRepository.findAll();
        return Map.of("content", bookings, "totalElements", bookingRepository.count());
    }

    public Booking updateBookingStatus(Long bookingId, String action, HttpServletRequest request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        BookingStatus newStatus = switch (action) {
            case "approve" -> BookingStatus.APPROVED;
            case "reject" -> BookingStatus.REJECTED;
            case "cancel" -> BookingStatus.CANCELLED;
            default -> throw new AppException("Invalid action");
        };
        booking.setStatus(newStatus);
        return bookingRepository.save(booking);
    }

    public List<VaccinationCenter> getAllCenters() {
        return centerRepository.findAll();
    }

    public Map<String, Object> getAllCentersPaginated(PageRequest pageable) {
        List<VaccinationCenter> centers = centerRepository.findAll();
        long total = centers.size();
        List<VaccinationCenter> pageContent = centers.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .collect(Collectors.toList());
        return Map.of("content", pageContent, "totalElements", total);
    }

    public VaccinationCenter createCenter(CenterRequest req) {
        VaccinationCenter center = VaccinationCenter.builder()
                .name(req.name())
                .address(req.address())
                .city(req.city())
                .state(req.state())
                .pincode(req.pincode())
                .phone(req.phone())
                .email(req.email())
                .build();
        return centerRepository.save(center);
    }

    public Map<String, Object> getAllDrives(PageRequest pageable) {
        List<VaccinationDrive> drives = driveRepository.findAll();
        long total = drives.size();
        List<VaccinationDrive> pageContent = drives.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .collect(Collectors.toList());
        return Map.of("content", pageContent, "totalElements", total);
    }

    public VaccinationDrive createDrive(DriveRequest req) {
        VaccinationDrive drive = VaccinationDrive.builder()
                .title(req.title())
                .description(req.description())
                .vaccineType(req.vaccineType())
                .driveDate(req.driveDate())
                .minAge(req.minAge())
                .maxAge(req.maxAge())
                .totalSlots(req.totalSlots())
                .build();
        return driveRepository.save(drive);
    }

    public Slot createSlot(SlotRequest req) {
        Slot slot = Slot.builder()
                .drive(driveRepository.findById(req.driveId()).orElseThrow())
                .dateTime(req.startTime())
                .capacity(req.capacity())
                .build();
        return slotRepository.save(slot);
    }

    public Map<String, Object> getDriveSlots(Long driveId) {
        List<Slot> slots = slotRepository.findByDriveIdOrderByStartTimeAsc(driveId);
        return Map.of("slots", slots);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Map<String, Object> getAllUsersPaginated(PageRequest pageable) {
        List<User> users = userRepository.findAll();
        long total = users.size();
        List<User> pageContent = users.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .collect(Collectors.toList());
        return Map.of("content", pageContent, "totalElements", total);
    }

    public Map<String, Object> enableUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setEnabled(true);
        userRepository.save(user);
        return Map.of("message", "User enabled");
    }

    public Map<String, Object> disableUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);
        return Map.of("message", "User disabled");
    }

    public Map<String, Object> getAllFeedback(PageRequest pageRequest) {
        List<Feedback> feedback = feedbackRepository.findAll();
        return Map.of("content", feedback, "totalElements", feedbackRepository.count());
    }

    public Map<String, Object> respondToFeedback(Long feedbackId, String response) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new AppException("Feedback not found"));
        feedback.setResponse(response);
        feedbackRepository.save(feedback);
        return Map.of("message", "Response saved", "feedbackId", feedbackId);
    }

    public Map<String, Object> getAllContacts(PageRequest pageRequest) {
        List<Contact> contacts = contactRepository.findAll();
        return Map.of("content", contacts, "totalElements", contactRepository.count());
    }

    public Map<String, Object> respondToContact(Long contactId, String response) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new AppException("Contact not found"));
        contact.setResponse(response);
        contactRepository.save(contact);
        return Map.of("message", "Response saved", "contactId", contactId);
    }

    public void deleteContact(Long contactId) {
        contactRepository.deleteById(contactId);
    }

    public Map<String, Object> getAuditLogs(PageRequest pageRequest) {
        List<AuditLog> logs = auditLogRepository.findAll();
        return Map.of("content", logs, "totalElements", auditLogRepository.count());
    }

    public Map<String, Object> exportBookingsToCsv() {
        List<Booking> bookings = bookingRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,User,Status,City,Booked At\n");
        for (Booking b : bookings) {
            csv.append(b.getId()).append(",")
               .append(b.getUser().getEmail()).append(",")
               .append(b.getStatus()).append(",")
               .append(b.getUser().getUserCity()).append(",")
               .append(b.getBookedAt()).append("\n");
        }
        return Map.of("csv", csv.toString());
    }

    @Transactional
    public void createAdmin(AdminCreateRequest req, HttpServletRequest request) {
        if (userRepository.existsByEmail(req.email())) {
            throw new AppException("Email already exists");
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        User admin = User.builder()
            .email(req.email())
            .fullName(req.fullName())
            .password(passwordEncoder.encode(req.password()))
            .phoneNumber(req.phoneNumber())
            .age(req.age())
            .roles(Set.of(adminRole))
            .enabled(true)
            .build();
        userRepository.save(admin);
        auditService.log(req.email(), "CREATE_ADMIN", "USER", "Admin created", request);
    }

    public List<User> getAllAdmins() {
        return userRepository.findAll().stream()
            .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN))
            .toList();
    }

    @Transactional
    public void deleteAdmin(Long adminId, HttpServletRequest request) {
        User admin = userRepository.findById(adminId).orElseThrow();
        userRepository.delete(admin);
        auditService.log(admin.getEmail(), "DELETE_ADMIN", "USER", "Admin deleted", request);
    }

    @Transactional
    public void deleteCenter(Long centerId, HttpServletRequest request) {
        VaccinationCenter center = centerRepository.findById(centerId).orElseThrow();
        centerRepository.delete(center);
        auditService.log("center-" + centerId, "DELETE_CENTER", "CENTER", "Center deleted", request);
    }

    @Transactional
    public void updateUserRole(Long userId, RoleName role, HttpServletRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        Role roleEntity = roleRepository.findByName(role).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(roleEntity);
        userRepository.save(user);
        auditService.log(user.getEmail(), "UPDATE_ROLE", "USER", "Role updated to " + role, request);
    }
}

