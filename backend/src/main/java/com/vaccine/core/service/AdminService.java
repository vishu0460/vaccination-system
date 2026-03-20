package com.vaccine.core.service;

import com.vaccine.common.dto.*;
import com.vaccine.domain.*;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.*;
import com.vaccine.util.SlotStatusResolver;
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
import java.util.Comparator;
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
    private final NotificationRepository notificationRepository;
    private final ReviewRepository reviewRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final CertificateService certificateService;
    private final AuditService auditService;
    private final FeedbackService feedbackService;
    private final ContactService contactService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
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
            .approvedBookings(confirmedBookings)
            .rejectedBookings(0L)
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
        if ("complete".equalsIgnoreCase(action)) {
            return completeBookingResponse(bookingId);
        }

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found"));
        BookingStatus newStatus = switch (action.toLowerCase()) {
            case "approve", "confirm" -> BookingStatus.CONFIRMED;
            case "reject", "cancel" -> BookingStatus.CANCELLED;
            default -> throw new AppException("Invalid action");
        };
        booking.setStatus(newStatus);
        Booking savedBooking = bookingRepository.save(booking);
        return BookingResponse.from(savedBooking);
    }

    @Transactional
    public Booking completeBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found"));

        System.out.println("Before: " + booking.getStatus());

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException("Cancelled bookings cannot be completed");
        }

        if (booking.getStatus() == BookingStatus.PENDING) {
            throw new AppException("Booking must be confirmed before it can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        System.out.println("After: " + booking.getStatus());
        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        if (!certificateService.certificateExistsForBooking(savedBooking.getId())) {
            certificateService.generate(savedBooking);
        }

        return savedBooking;
    }

    @Transactional
    public Booking completeBooking(Long bookingId, HttpServletRequest request) {
        return completeBooking(bookingId);
    }

    @Transactional
    public BookingResponse completeBookingResponse(Long bookingId) {
        return BookingResponse.from(completeBooking(bookingId));
    }

    @Transactional
    public BookingResponse completeBookingResponse(Long bookingId, HttpServletRequest request) {
        return BookingResponse.from(completeBooking(bookingId, request));
    }

    public void deleteBooking(Long bookingId, HttpServletRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found"));

        Slot slot = booking.getSlot();
        if (slot != null && booking.getStatus() != BookingStatus.CANCELLED) {
            int currentBooked = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
            slot.setBookedCount(Math.max(0, currentBooked - 1));
            slotRepository.save(slot);
        }

        bookingRepository.delete(booking);
        auditService.log("booking-" + bookingId, "DELETE_BOOKING", "BOOKING", "Booking deleted", request);
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

    public Map<String, Object> getAllSlots(PageRequest pageable, SlotStatus status, Long centerId, Long driveId, LocalDate date) {
        List<AdminSlotResponse> slots = getAllSlots(status, centerId, driveId, date);
        long total = slots.size();
        List<AdminSlotResponse> pageContent = slots.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .collect(Collectors.toList());
        return Map.of("content", pageContent, "totalElements", total);
    }

    public List<AdminSlotResponse> getAllSlots(SlotStatus status, Long centerId, Long driveId, LocalDate date) {
        return slotRepository.findAll().stream()
            .filter(slot -> status == null || SlotStatusResolver.resolve(slot) == status)
            .filter(slot -> centerId == null || (slot.getDrive() != null && slot.getDrive().getCenter() != null
                && centerId.equals(slot.getDrive().getCenter().getId())))
            .filter(slot -> driveId == null || (slot.getDrive() != null && driveId.equals(slot.getDrive().getId())))
            .filter(slot -> date == null || (slot.getDateTime() != null && date.equals(slot.getDateTime().toLocalDate())))
            .sorted(Comparator.comparing(Slot::getDateTime, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(this::toAdminSlotResponse)
            .toList();
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
        validateSlotRequest(req);
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
        validateSlotRequest(req);
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
        List<Map<String, Object>> feedback = feedbackService.getAllFeedback();
        return Map.of("content", feedback, "totalElements", feedback.size());
    }

    public Map<String, Object> respondToFeedback(Long feedbackId, String response) {
        feedbackService.respondToFeedback(feedbackId, response);
        return Map.of(
            "message", "Response saved",
            "feedbackId", feedbackId,
            "feedback", feedbackService.getFeedbackById(feedbackId)
        );
    }

    public Map<String, Object> getAllContacts(PageRequest pageRequest) {
        List<Map<String, Object>> contacts = contactService.getAllContacts();
        return Map.of("content", contacts, "totalElements", contacts.size());
    }

    public Map<String, Object> respondToContact(Long contactId, String response) {
        contactService.respondToContact(contactId, response);
        return Map.of(
            "message", "Response saved",
            "contactId", contactId,
            "contact", contactService.getContactById(contactId)
        );
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

    private AdminSlotResponse toAdminSlotResponse(Slot slot) {
        int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();

        return new AdminSlotResponse(
            slot.getId(),
            slot.getDateTime(),
            SlotStatusResolver.resolveEnd(slot),
            capacity,
            bookedCount,
            Math.max(0, capacity - bookedCount),
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getId() : null,
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getName() : null,
            slot.getDrive() != null ? slot.getDrive().getId() : null,
            slot.getDrive() != null ? slot.getDrive().getTitle() : null,
            SlotStatusResolver.resolve(slot).name()
        );
    }

    private void validateSlotRequest(SlotRequest req) {
        if (req.startTime() == null || req.endTime() == null) {
            throw new AppException("Slot start time and end time are required");
        }
        if (!req.endTime().isAfter(req.startTime())) {
            throw new AppException("Slot end time must be after start time");
        }
        if (req.capacity() == null || req.capacity() < 1) {
            throw new AppException("Slot capacity must be at least 1");
        }
    }
}
