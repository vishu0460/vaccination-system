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
import org.springframework.cache.annotation.CacheEvict;
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
    private final ReviewRepository reviewRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final CertificateService certificateService;
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
        List<BookingResponse> bookings = bookingRepository.findAll().stream()
            .map(BookingResponse::from)
            .toList();
        return Map.of("content", bookings, "totalElements", bookingRepository.count());
    }

    public BookingResponse updateBookingStatus(Long bookingId, String action, HttpServletRequest request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        BookingStatus newStatus = switch (action) {
            case "approve", "confirm" -> BookingStatus.CONFIRMED;
            case "reject" -> BookingStatus.CANCELLED;
            case "cancel" -> BookingStatus.CANCELLED;
            case "complete" -> BookingStatus.COMPLETED;
            default -> throw new AppException("Invalid action");
        };
        booking.setStatus(newStatus);
        Booking savedBooking = bookingRepository.save(booking);

        if (newStatus == BookingStatus.COMPLETED && !certificateService.certificateExistsForBooking(savedBooking.getId())) {
            String vaccineName = savedBooking.getSlot() != null && savedBooking.getSlot().getDrive() != null
                ? savedBooking.getSlot().getDrive().getVaccineType()
                : "Vaccination";
            certificateService.generateCertificate(savedBooking.getId(), vaccineName, 1);
        }

        return BookingResponse.from(savedBooking);
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

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public VaccinationCenter createCenter(CenterRequest req) {
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
        return centerRepository.save(center);
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public VaccinationCenter updateCenter(Long centerId, CenterRequest req) {
        VaccinationCenter center = centerRepository.findById(centerId)
            .orElseThrow(() -> new AppException("Center not found"));

        center.setName(req.name());
        center.setAddress(req.address());
        center.setCity(req.city());
        center.setState(req.state());
        center.setPincode(req.pincode());
        center.setPhone(req.phone());
        center.setEmail(req.email());
        center.setWorkingHours(req.workingHours());
        center.setDailyCapacity(req.dailyCapacity());
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

    public Map<String, Object> getAllSlots(PageRequest pageable) {
        List<Slot> slots = slotRepository.findAll();
        long total = slots.size();
        List<Slot> pageContent = slots.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .collect(Collectors.toList());
        return Map.of("content", pageContent, "totalElements", total);
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public VaccinationDrive createDrive(DriveRequest req) {
        VaccinationCenter center = centerRepository.findById(req.centerId())
                .orElseThrow(() -> new AppException("Center not found"));

        VaccinationDrive drive = VaccinationDrive.builder()
                .title(req.title())
                .description(req.description())
                .center(center)
                .vaccineType(req.vaccineType() != null && !req.vaccineType().isBlank() ? req.vaccineType() : "General Vaccination")
                .driveDate(req.driveDate())
                .minAge(req.minAge() != null ? req.minAge() : 18)
                .maxAge(req.maxAge() != null ? req.maxAge() : 120)
                .startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(17, 0))
                .totalSlots(req.totalSlots() != null ? req.totalSlots() : 100)
                .active(req.active() != null ? req.active() : true)
                .build();
        return driveRepository.save(drive);
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public VaccinationDrive updateDrive(Long driveId, DriveRequest req) {
        VaccinationDrive drive = driveRepository.findById(driveId)
            .orElseThrow(() -> new AppException("Drive not found"));
        VaccinationCenter center = centerRepository.findById(req.centerId())
            .orElseThrow(() -> new AppException("Center not found"));

        drive.setTitle(req.title());
        drive.setDescription(req.description());
        drive.setVaccineType(req.vaccineType() != null && !req.vaccineType().isBlank() ? req.vaccineType() : drive.getVaccineType());
        drive.setCenter(center);
        drive.setDriveDate(req.driveDate());
        drive.setMinAge(req.minAge() != null ? req.minAge() : drive.getMinAge());
        drive.setMaxAge(req.maxAge() != null ? req.maxAge() : drive.getMaxAge());
        drive.setTotalSlots(req.totalSlots() != null ? req.totalSlots() : drive.getTotalSlots());
        drive.setActive(req.active() != null ? req.active() : drive.getActive());
        return driveRepository.save(drive);
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public Slot createSlot(SlotRequest req) {
        Slot slot = Slot.builder()
                .drive(driveRepository.findById(req.driveId()).orElseThrow())
                .dateTime(req.startTime())
                .startTime(req.startTime().toLocalTime())
                .endTime(req.endTime().toLocalTime())
                .capacity(req.capacity())
                .build();
        return slotRepository.save(slot);
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public Slot updateSlot(Long slotId, SlotRequest req) {
        Slot slot = slotRepository.findById(slotId)
            .orElseThrow(() -> new AppException("Slot not found"));
        VaccinationDrive drive = driveRepository.findById(req.driveId())
            .orElseThrow(() -> new AppException("Drive not found"));

        int currentBooked = slot.getBookedCount() != null ? slot.getBookedCount() : 0;
        int requestedCapacity = req.capacity() != null ? req.capacity() : slot.getCapacity();
        if (requestedCapacity < currentBooked) {
            throw new AppException("Capacity cannot be less than existing bookings");
        }

        slot.setDrive(drive);
        slot.setDateTime(req.startTime());
        slot.setStartTime(req.startTime().toLocalTime());
        slot.setEndTime(req.endTime().toLocalTime());
        slot.setCapacity(requestedCapacity);
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

    public User updateUser(Long userId, UserUpdateRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException("User not found"));

        if (req.email() != null && !req.email().isBlank()) {
            String email = req.email().trim().toLowerCase();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new AppException("Email already exists");
            }
            user.setEmail(email);
        }
        if (req.fullName() != null && !req.fullName().isBlank()) {
            user.setFullName(req.fullName().trim());
        }
        if (req.age() != null) {
            user.setAge(req.age());
        }
        if (req.phoneNumber() != null) {
            user.setPhoneNumber(req.phoneNumber().trim());
        }
        if (req.enabled() != null) {
            user.setEnabled(req.enabled());
        }

        return userRepository.save(user);
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
            .roles(new HashSet<>(Set.of(adminRole)))
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
    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public void deleteCenter(Long centerId, HttpServletRequest request) {
        VaccinationCenter center = centerRepository.findById(centerId).orElseThrow();
        for (VaccinationDrive drive : driveRepository.findByCenterId(centerId)) {
            deleteDriveInternal(drive);
        }
        reviewRepository.deleteByCenterId(centerId);
        centerRepository.delete(center);
        auditService.log("center-" + centerId, "DELETE_CENTER", "CENTER", "Center deleted", request);
    }

    @Transactional
    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public void deleteDrive(Long driveId, HttpServletRequest request) {
        VaccinationDrive drive = driveRepository.findById(driveId).orElseThrow();
        deleteDriveInternal(drive);
        auditService.log("drive-" + driveId, "DELETE_DRIVE", "DRIVE", "Drive deleted", request);
    }

    @Transactional
    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public void deleteSlot(Long slotId, HttpServletRequest request) {
        Slot slot = slotRepository.findById(slotId).orElseThrow(() -> new AppException("Slot not found"));
        deleteSlotInternal(slot);
        auditService.log("slot-" + slotId, "DELETE_SLOT", "SLOT", "Slot deleted", request);
    }

    @Transactional
    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public void deleteUser(Long userId, HttpServletRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException("User not found"));
        List<Booking> bookings = bookingRepository.findByUserId(userId);

        for (Booking booking : bookings) {
            Slot slot = booking.getSlot();
            if (slot != null && booking.getStatus() != BookingStatus.CANCELLED) {
                slot.setBookedCount(Math.max(0, (slot.getBookedCount() == null ? 0 : slot.getBookedCount()) - 1));
                slotRepository.save(slot);
            }
        }

        bookingRepository.deleteAll(bookings);
        feedbackRepository.deleteByUserId(userId);
        contactRepository.deleteByUserId(userId);
        reviewRepository.deleteByUserId(userId);
        emailVerificationRepository.deleteByUserEmail(user.getEmail());
        passwordResetRepository.deleteByUserEmail(user.getEmail());
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            phoneVerificationRepository.deleteByPhoneNumber(user.getPhoneNumber());
        }
        userRepository.delete(user);
        auditService.log(user.getEmail(), "DELETE_USER", "USER", "User deleted", request);
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

    private void deleteDriveInternal(VaccinationDrive drive) {
        feedbackRepository.deleteByDriveId(drive.getId());
        for (Slot slot : slotRepository.findByDriveId(drive.getId())) {
            deleteSlotInternal(slot);
        }
        driveRepository.delete(drive);
    }

    private void deleteSlotInternal(Slot slot) {
        bookingRepository.deleteAll(bookingRepository.findBySlotId(slot.getId()));
        slotRepository.delete(slot);
    }
}
