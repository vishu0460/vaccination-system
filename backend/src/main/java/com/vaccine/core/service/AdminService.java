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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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
    private final SearchLogRepository searchLogRepository;
    private final CertificateService certificateService;
    private final AuditService auditService;
    private final FeedbackService feedbackService;
    private final ContactService contactService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final INotificationService notificationService;

    public AdminDashboardStatsResponse getDashboardStats() {
        if (getRestrictedAdminIdOrNull() != null) {
            throw new AppException("Global analytics are available only for super admin");
        }
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long totalCenters = centerRepository.count();
        long totalDrives = driveRepository.count();
        long activeDrives = driveRepository.countByStatusIn(List.of(Status.UPCOMING, Status.LIVE));
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
            .activeUsers(activeUsers)
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
            .completedVaccinations(completedVaccinations)
            .totalNews(totalNews)
            .build();
    }

    public SearchAnalyticsResponse getSearchAnalytics() {
        if (getRestrictedAdminIdOrNull() != null) {
            throw new AppException("Global analytics are available only for super admin");
        }
        LocalDateTime since = LocalDate.now().minusDays(29).atStartOfDay();
        List<SearchLog> logs = searchLogRepository.findBySearchedAtAfter(since);

        List<SearchMetricResponse> topCities = logs.stream()
            .map(log -> {
                String primary = log.getCity();
                if (primary != null && !primary.isBlank()) {
                    return primary.trim();
                }
                String fallback = log.getDetectedCity();
                return fallback == null || fallback.isBlank() ? null : fallback.trim();
            })
            .filter(value -> value != null && !value.isBlank())
            .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(6)
            .map(entry -> new SearchMetricResponse(entry.getKey(), entry.getValue()))
            .toList();

        List<SearchMetricResponse> topKeywords = logs.stream()
            .map(SearchLog::getNormalizedQuery)
            .filter(value -> value != null && !value.isBlank() && !"nearby centers".equals(value))
            .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(6)
            .map(entry -> new SearchMetricResponse(entry.getKey(), entry.getValue()))
            .toList();

        List<SearchTrendPointResponse> trends = logs.stream()
            .collect(Collectors.groupingBy(log -> log.getSearchedAt().toLocalDate(), TreeMap::new, Collectors.counting()))
            .entrySet().stream()
            .map(entry -> new SearchTrendPointResponse(entry.getKey(), entry.getValue()))
            .toList();

        return new SearchAnalyticsResponse(logs.size(), topCities, topKeywords, trends);
    }

    public DashboardAnalyticsResponse getDashboardAnalytics() {
        if (getRestrictedAdminIdOrNull() != null) {
            throw new AppException("Global analytics are available only for super admin");
        }
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();
        long activeDrives = driveRepository.countByStatusIn(List.of(Status.UPCOMING, Status.LIVE));
        long availableSlots = slotRepository.sumAvailableCapacity();
        long totalSlotCapacity = slotRepository.findAll().stream()
            .mapToLong(slot -> slot.getCapacity() == null ? 0 : slot.getCapacity())
            .sum();
        long totalBookedSlots = slotRepository.findAll().stream()
            .mapToLong(slot -> slot.getBookedCount() == null ? 0 : slot.getBookedCount())
            .sum();
        double slotFillRate = totalSlotCapacity <= 0 ? 0d : ((double) totalBookedSlots / totalSlotCapacity) * 100.0;

        String mostSearchedCity = searchLogRepository.findAll().stream()
            .map(log -> {
                String primary = log.getCity();
                if (primary != null && !primary.isBlank()) {
                    return primary.trim();
                }
                String fallback = log.getDetectedCity();
                return fallback == null || fallback.isBlank() ? null : fallback.trim();
            })
            .filter(value -> value != null && !value.isBlank())
            .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry::getKey))
            .map(Map.Entry::getKey)
            .orElse("N/A");

        String mostBookedVaccine = bookingRepository.findAll().stream()
            .map(booking -> booking.getSlot())
            .filter(slot -> slot != null && slot.getDrive() != null)
            .map(slot -> slot.getDrive().getVaccineType())
            .filter(vaccine -> vaccine != null && !vaccine.isBlank())
            .collect(Collectors.groupingBy(vaccine -> vaccine, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry::getKey))
            .map(Map.Entry::getKey)
            .orElse("N/A");

        List<PopularSlotInsightResponse> mostPopularSlots = slotRepository.findAll().stream()
            .map(slot -> {
                long bookingCount = bookingRepository.findBySlotId(slot.getId()).stream()
                    .filter(booking -> booking.getStatus() != null && booking.getStatus() != BookingStatus.CANCELLED)
                    .count();
                double fillRateValue = (slot.getCapacity() == null || slot.getCapacity() <= 0)
                    ? 0d
                    : ((double) (slot.getBookedCount() == null ? 0 : slot.getBookedCount()) / slot.getCapacity()) * 100.0;
                return new PopularSlotInsightResponse(
                    slot.getId(),
                    slot.getDrive() != null ? slot.getDrive().getTitle() : "N/A",
                    slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getName() : "N/A",
                    bookingCount,
                    Math.round(fillRateValue * 100.0) / 100.0
                );
            })
            .sorted(Comparator
                .comparing(PopularSlotInsightResponse::bookingCount, Comparator.reverseOrder())
                .thenComparing(PopularSlotInsightResponse::fillRate, Comparator.reverseOrder()))
            .limit(5)
            .toList();

        List<BookingTrendPointResponse> dailyBookings = bookingRepository.findAll().stream()
            .filter(booking -> booking.getBookedAt() != null)
            .collect(Collectors.groupingBy(booking -> booking.getBookedAt().toLocalDate(), TreeMap::new, Collectors.counting()))
            .entrySet().stream()
            .map(entry -> new BookingTrendPointResponse(entry.getKey().toString(), entry.getValue()))
            .toList();

        List<BookingTrendPointResponse> slotUsage = slotRepository.findAll().stream()
            .sorted(Comparator.comparing(Slot::getId))
            .limit(10)
            .map(slot -> new BookingTrendPointResponse(
                "Slot #" + slot.getId(),
                Long.valueOf(slot.getBookedCount() == null ? 0 : slot.getBookedCount())
            ))
            .toList();

        return new DashboardAnalyticsResponse(
            totalUsers,
            totalBookings,
            activeDrives,
            availableSlots,
            Math.round(slotFillRate * 100.0) / 100.0,
            mostSearchedCity,
            mostBookedVaccine,
            mostPopularSlots,
            dailyBookings,
            slotUsage
        );
    }

    public Map<String, Object> getAllBookings(PageRequest pageRequest, BookingStatus status, String city) {
        User currentUser = getCurrentUserOrNull();
        List<Booking> scopedBookings = isRestrictedAdmin(currentUser)
            ? bookingRepository.findByAdminId(currentUser.getId())
            : bookingRepository.findAll();

        List<BookingResponse> bookings = scopedBookings.stream()
            .filter(booking -> status == null || booking.getStatus() == status)
            .filter(booking -> city == null || city.isBlank() || (
                booking.getUser() != null
                    && booking.getUser().getUserCity() != null
                    && booking.getUser().getUserCity().equalsIgnoreCase(city.trim())
            ))
            .sorted(Comparator.comparing(Booking::getBookedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(BookingResponse::from)
            .toList();
        List<BookingResponse> pageContent = bookings.stream()
            .skip(pageRequest.getOffset())
            .limit(pageRequest.getPageSize())
            .toList();
        return Map.of("content", pageContent, "totalElements", bookings.size());
    }

    public BookingResponse updateBookingStatus(Long bookingId, String action, HttpServletRequest request) {
        if ("complete".equalsIgnoreCase(action)) {
            return completeBookingResponse(bookingId);
        }

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found"));
        assertBookingAccess(booking);
        BookingStatus previousStatus = booking.getStatus();
        BookingStatus newStatus = switch (action.toLowerCase()) {
            case "approve", "confirm" -> BookingStatus.CONFIRMED;
            case "reject", "cancel" -> BookingStatus.CANCELLED;
            default -> throw new AppException("Invalid action");
        };

        if (previousStatus == newStatus) {
            return BookingResponse.from(booking);
        }

        booking.setStatus(newStatus);
        if (newStatus == BookingStatus.CANCELLED) {
            booking.setCancelledAt(LocalDateTime.now());
            Slot slot = booking.getSlot();
            if (slot != null && previousStatus != BookingStatus.CANCELLED) {
                int currentBooked = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
                slot.setBookedCount(Math.max(0, currentBooked - 1));
                slotRepository.save(slot);
            }
        }

        Booking savedBooking = bookingRepository.save(booking);
        if (newStatus == BookingStatus.CONFIRMED) {
            notificationService.queueBookingConfirmedNotification(savedBooking);
        }
        auditService.logAction("UPDATE_BOOKING_STATUS", "BOOKING", bookingId, "Booking status updated from " + previousStatus + " to " + newStatus, request);
        return BookingResponse.from(savedBooking);
    }

    @Transactional
    public Booking completeBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("Booking not found"));
        assertBookingAccess(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException("Cancelled bookings cannot be completed");
        }

        if (booking.getStatus() == BookingStatus.PENDING) {
            throw new AppException("Booking must be confirmed before it can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        LocalDateTime completionTime = booking.getAssignedTime() != null
            ? booking.getAssignedTime()
            : booking.getSlot() != null ? booking.getSlot().getDateTime() : LocalDateTime.now();
        booking.setFirstDoseDate(completionTime);
        boolean secondDoseRequired = booking.getSlot() != null
            && booking.getSlot().getDrive() != null
            && Boolean.TRUE.equals(booking.getSlot().getDrive().getSecondDoseRequired());
        booking.setSecondDoseRequired(secondDoseRequired);
        if (secondDoseRequired && booking.getSlot() != null && booking.getSlot().getDrive() != null) {
            Integer gapDays = booking.getSlot().getDrive().getSecondDoseGapDays();
            if (gapDays != null && gapDays > 0) {
                booking.setNextDoseDueDate(completionTime.plusDays(gapDays));
            }
        } else {
            booking.setNextDoseDueDate(null);
        }
        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        if (!certificateService.certificateExistsForBooking(savedBooking.getId())) {
            certificateService.generate(savedBooking);
        }
        notificationService.queueVaccinationCompletedNotification(savedBooking);
        auditService.logAction("COMPLETE_BOOKING", "BOOKING", savedBooking.getId(), "Booking marked as completed");

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
        assertBookingAccess(booking);

        Slot slot = booking.getSlot();
        if (slot != null && booking.getStatus() != BookingStatus.CANCELLED) {
            int currentBooked = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
            slot.setBookedCount(Math.max(0, currentBooked - 1));
            slotRepository.save(slot);
        }

        softDeleteBooking(booking, auditService.getCurrentActor());
        auditService.logAction("SOFT_DELETE_BOOKING", "BOOKING", bookingId, "Booking soft deleted", request);
    }

    public List<VaccinationCenter> getAllCenters() {
        Long restrictedAdminId = getRestrictedAdminIdOrNull();
        return restrictedAdminId == null ? centerRepository.findAll() : centerRepository.findByAdminId(restrictedAdminId);
    }

    public Map<String, Object> getAllCentersPaginated(PageRequest pageable) {
        Long restrictedAdminId = getRestrictedAdminIdOrNull();
        List<VaccinationCenter> centers = restrictedAdminId == null
            ? centerRepository.findAll()
            : centerRepository.findByAdminId(restrictedAdminId);
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
                .adminId(resolveAdminOwnerForWrite())
                .build();
        VaccinationCenter savedCenter = centerRepository.save(center);
        log.info("Center created id={} city={} name={}", savedCenter.getId(), savedCenter.getCity(), savedCenter.getName());
        auditService.logAction("CREATE_CENTER", "CENTER", savedCenter.getId(), "Center created: " + savedCenter.getName());
        return savedCenter;
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public VaccinationCenter updateCenter(Long centerId, CenterRequest req) {
        VaccinationCenter center = centerRepository.findById(centerId)
            .orElseThrow(() -> new AppException("Center not found"));
        assertCenterAccess(center);

        center.setName(req.name());
        center.setAddress(req.address());
        center.setCity(req.city());
        center.setState(req.state());
        center.setPincode(req.pincode());
        center.setPhone(req.phone());
        center.setEmail(req.email());
        center.setWorkingHours(req.workingHours());
        center.setDailyCapacity(req.dailyCapacity());
        VaccinationCenter savedCenter = centerRepository.save(center);
        log.info("Center updated id={} city={} name={}", savedCenter.getId(), savedCenter.getCity(), savedCenter.getName());
        auditService.logAction("UPDATE_CENTER", "CENTER", savedCenter.getId(), "Center updated: " + savedCenter.getName());
        return savedCenter;
    }

    public Map<String, Object> getAllDrives(PageRequest pageable) {
        User currentUser = getCurrentUserOrNull();
        List<VaccinationDrive> drives = isRestrictedAdmin(currentUser)
            ? driveRepository.findByAdminId(currentUser.getId())
            : driveRepository.findAll();
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
        User currentUser = getCurrentUserOrNull();
        List<Slot> scopedSlots = isRestrictedAdmin(currentUser)
            ? slotRepository.findByAdminId(currentUser.getId())
            : slotRepository.findAll();

        return scopedSlots.stream()
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
        if (req.centerId() == null) {
            throw new AppException("Center is required");
        }
        if (req.driveDate() == null) {
            throw new AppException("Drive date is required");
        }
        VaccinationCenter center = centerRepository.findById(req.centerId())
                .orElseThrow(() -> new AppException("Center not found"));
        assertCenterAccess(center);

        VaccinationDrive drive = VaccinationDrive.builder()
                .title(req.title())
                .description(req.description())
                .center(center)
                .adminId(resolveDriveOwnerId(center))
                .vaccineType(req.vaccineType() != null && !req.vaccineType().isBlank() ? req.vaccineType() : "General Vaccination")
                .driveDate(req.driveDate())
                .minAge(req.minAge() != null ? req.minAge() : 18)
                .maxAge(req.maxAge() != null ? req.maxAge() : 120)
                .startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(17, 0))
                .totalSlots(req.totalSlots() != null ? req.totalSlots() : 100)
                .secondDoseRequired(req.secondDoseRequired() != null ? req.secondDoseRequired() : false)
                .secondDoseGapDays(Boolean.TRUE.equals(req.secondDoseRequired()) ? req.secondDoseGapDays() : null)
                .status(req.status() != null ? req.status() : Status.UPCOMING)
                .active(req.active() != null ? req.active() : true)
                .build();
        VaccinationDrive savedDrive = driveRepository.save(drive);
        log.info("Drive created id={} centerId={} date={} vaccine={}", savedDrive.getId(), center.getId(), savedDrive.getDriveDate(), savedDrive.getVaccineType());
        auditService.logAction("CREATE_DRIVE", "DRIVE", savedDrive.getId(), "Drive created: " + savedDrive.getTitle());
        return savedDrive;
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public VaccinationDrive updateDrive(Long driveId, DriveRequest req) {
        VaccinationDrive drive = driveRepository.findById(driveId)
            .orElseThrow(() -> new AppException("Drive not found"));
        assertDriveAccess(drive);

        if (req.centerId() != null) {
            VaccinationCenter center = centerRepository.findById(req.centerId())
                .orElseThrow(() -> new AppException("Center not found"));
            assertCenterAccess(center);
            drive.setCenter(center);
            drive.setAdminId(resolveDriveOwnerId(center));
        }
        if (req.title() != null && !req.title().isBlank()) {
            drive.setTitle(req.title().trim());
        }
        if (req.description() != null) {
            drive.setDescription(req.description().trim());
        }
        if (req.vaccineType() != null && !req.vaccineType().isBlank()) {
            drive.setVaccineType(req.vaccineType().trim());
        }
        if (req.driveDate() != null) {
            drive.setDriveDate(req.driveDate());
        }
        if (req.minAge() != null) {
            drive.setMinAge(req.minAge());
        }
        if (req.maxAge() != null) {
            drive.setMaxAge(req.maxAge());
        }
        if (req.totalSlots() != null) {
            drive.setTotalSlots(req.totalSlots());
        }
        if (req.secondDoseRequired() != null) {
            drive.setSecondDoseRequired(req.secondDoseRequired());
            if (!req.secondDoseRequired()) {
                drive.setSecondDoseGapDays(null);
            }
        }
        if (req.secondDoseGapDays() != null) {
            drive.setSecondDoseGapDays(req.secondDoseGapDays());
        }
        if (req.status() != null) {
            drive.setStatus(req.status());
        }
        if (req.active() != null) {
            drive.setActive(req.active());
        }
        VaccinationDrive savedDrive = driveRepository.save(drive);
        log.info("Drive updated id={} centerId={} date={} vaccine={}", savedDrive.getId(), savedDrive.getCenter() != null ? savedDrive.getCenter().getId() : null, savedDrive.getDriveDate(), savedDrive.getVaccineType());
        auditService.logAction("UPDATE_DRIVE", "DRIVE", savedDrive.getId(), "Drive updated: " + savedDrive.getTitle());
        return savedDrive;
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public Slot createSlot(SlotRequest req) {
        VaccinationDrive drive = findDriveOrThrow(req.getDriveId());
        assertDriveAccess(drive);
        validateSlotRequest(req, null);
        Slot slot = Slot.builder()
                .drive(drive)
                .adminId(resolveSlotOwnerId(drive))
                .capacity(req.getCapacity())
                .build();
        slot.setStartDateTime(req.getStartDate());
        slot.setEndDateTime(req.getEndDate());
        Slot savedSlot = slotRepository.save(slot);
        log.info("Slot created id={} driveId={} startDate={} endDate={} capacity={}", savedSlot.getId(), drive.getId(), req.getStartDate(), req.getEndDate(), savedSlot.getCapacity());
        auditService.logAction("CREATE_SLOT", "SLOT", savedSlot.getId(), "Slot created for drive " + drive.getId());
        return savedSlot;
    }

    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public Slot updateSlot(Long slotId, SlotRequest req) {
        Slot slot = slotRepository.findById(slotId)
            .orElseThrow(() -> new AppException("Slot not found"));
        assertSlotAccess(slot);
        SlotRequest normalizedRequest = normalizeSlotRequestForUpdate(req, slot);
        validateSlotRequest(normalizedRequest, slotId);
        VaccinationDrive drive = findDriveOrThrow(normalizedRequest.getDriveId());
        assertDriveAccess(drive);

        int currentBooked = slot.getBookedCount() != null ? slot.getBookedCount() : 0;
        int requestedCapacity = normalizedRequest.getCapacity() != null ? normalizedRequest.getCapacity() : slot.getCapacity();
        if (requestedCapacity < currentBooked) {
            throw new AppException("Capacity cannot be less than existing bookings");
        }

        slot.setDrive(drive);
        slot.setAdminId(resolveSlotOwnerId(drive));
        slot.setStartDateTime(normalizedRequest.getStartDate());
        slot.setEndDateTime(normalizedRequest.getEndDate());
        slot.setCapacity(requestedCapacity);
        Slot savedSlot = slotRepository.save(slot);
        log.info("Slot updated id={} driveId={} startDate={} endDate={} capacity={}", savedSlot.getId(), drive.getId(), normalizedRequest.getStartDate(), normalizedRequest.getEndDate(), savedSlot.getCapacity());
        auditService.logAction("UPDATE_SLOT", "SLOT", savedSlot.getId(), "Slot updated for drive " + drive.getId());
        return savedSlot;
    }

    public Map<String, Object> getDriveSlots(Long driveId) {
        VaccinationDrive drive = findDriveOrThrow(driveId);
        assertDriveAccess(drive);
        Long currentAdminId = getRestrictedAdminIdOrNull();
        List<AdminSlotResponse> slots = slotRepository.findByDrive_IdOrderByDateTimeAsc(driveId).stream()
            .filter(slot -> currentAdminId == null || currentAdminId.equals(slot.getAdminId()))
            .sorted(Comparator.comparing(Slot::getDateTime, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(this::toAdminSlotResponse)
            .toList();
        return Map.of("slots", slots);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Map<String, Object> getAllUsersPaginated(PageRequest pageable) {
        Long restrictedAdminId = getRestrictedAdminIdOrNull();
        List<User> users = (restrictedAdminId == null ? userRepository.findAll().stream()
            : bookingRepository.findByAdminId(restrictedAdminId).stream()
                .map(Booking::getUser)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream())
            .sorted(Comparator
                .comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(User::getId, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
        long total = users.size();
        List<User> pageContent = users.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .collect(Collectors.toList());
        return Map.of("content", pageContent, "totalElements", total);
    }

    public Map<String, Object> enableUser(Long userId) {
        if (getRestrictedAdminIdOrNull() != null) {
            throw new AppException("Only super admin can update user status");
        }
        User user = userRepository.findById(userId).orElseThrow();
        user.setEnabled(true);
        userRepository.save(user);
        auditService.logAction("ENABLE_USER", "USER", userId, "User enabled: " + user.getEmail());
        return Map.of("message", "User enabled");
    }

    public Map<String, Object> disableUser(Long userId) {
        if (getRestrictedAdminIdOrNull() != null) {
            throw new AppException("Only super admin can update user status");
        }
        User user = userRepository.findById(userId).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);
        auditService.logAction("DISABLE_USER", "USER", userId, "User disabled: " + user.getEmail());
        return Map.of("message", "User disabled");
    }

    public User updateUser(Long userId, UserUpdateRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException("User not found"));

        if (req.email() != null && !req.email().isBlank()) {
            String email = req.email().trim().toLowerCase();
            if (!email.equals(user.getEmail()) && userRepository.existsAnyByEmail(email)) {
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

        User savedUser = userRepository.save(user);
        auditService.logAction("UPDATE_USER", "USER", savedUser.getId(), "User updated: " + savedUser.getEmail());
        return savedUser;
    }

    public Map<String, Object> getAllFeedback(PageRequest pageRequest) {
        List<Map<String, Object>> feedback = feedbackService.getAllFeedback(getRestrictedAdminIdOrNull());
        return Map.of("content", feedback, "totalElements", feedback.size());
    }

    public Map<String, Object> respondToFeedback(Long feedbackId, String response) {
        Map<String, Object> feedbackRecord = feedbackService.getFeedbackById(feedbackId);
        assertFeedbackAccess(feedbackRecord);
        feedbackService.respondToFeedback(feedbackId, response);
        return Map.of(
            "message", "Response saved",
            "feedbackId", feedbackId,
            "feedback", feedbackRecord
        );
    }

    public Map<String, Object> getAllContacts(PageRequest pageRequest) {
        Long adminId = getRestrictedAdminIdOrNull();
        List<Map<String, Object>> contacts = adminId == null
            ? contactService.getAllContacts()
            : contactService.getAllContacts(adminId);
        return Map.of("content", contacts, "totalElements", contacts.size());
    }

    public Map<String, Object> respondToContact(Long contactId, String response) {
        Contact contact = contactRepository.findById(contactId).orElseThrow(() -> new AppException("Contact not found"));
        assertContactAccess(contact);
        contactService.respondToContact(contactId, response);
        return Map.of(
            "message", "Response saved",
            "contactId", contactId,
            "contact", contactService.getContactById(contactId)
        );
    }

    public void deleteContact(Long contactId) {
        Contact contact = contactRepository.findById(contactId).orElseThrow(() -> new AppException("Contact not found"));
        assertContactAccess(contact);
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
        User currentUser = getCurrentUserOrNull();
        if (currentUser != null && !currentUser.isSuperAdmin()) {
            throw new AppException("Only super admin can create admin accounts");
        }
        if (userRepository.existsAnyByEmail(req.email())) {
            throw new AppException("Email already exists");
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        User admin = User.builder()
            .email(req.email().trim().toLowerCase())
            .fullName(req.fullName().trim())
            .password(passwordEncoder.encode(req.password()))
            .phoneNumber(req.phoneNumber())
            .age(req.age() != null ? req.age() : 30)
            .createdBy(currentUserId)
            .role(RoleName.ADMIN.name())
            .isAdmin(true)
            .isSuperAdmin(false)
            .roles(new HashSet<>(Set.of(adminRole)))
            .enabled(true)
            .emailVerified(true)
            .build();
        userRepository.save(admin);
        auditService.logAction("CREATE_ADMIN", "USER", admin.getId(), "Admin created: " + req.email(), request);
    }

    public List<User> getAllAdmins() {
        return userRepository.findAll().stream()
            .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN))
            .toList();
    }

    @Transactional
    public void deleteAdmin(Long adminId, HttpServletRequest request) {
        User admin = userRepository.findById(adminId).orElseThrow();
        softDeleteUser(admin, auditService.getCurrentActor());
        auditService.logAction("SOFT_DELETE_ADMIN", "USER", adminId, "Admin soft deleted: " + admin.getEmail(), request);
    }

    @Transactional
    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public void deleteCenter(Long centerId, HttpServletRequest request) {
        VaccinationCenter center = centerRepository.findById(centerId).orElseThrow();
        assertCenterAccess(center);
        String actor = auditService.getCurrentActor();
        for (VaccinationDrive drive : driveRepository.findByCenterId(centerId)) {
            deleteDriveInternal(drive, actor);
        }
        reviewRepository.deleteByCenterId(centerId);
        softDeleteCenter(center, actor);
        auditService.logAction("SOFT_DELETE_CENTER", "CENTER", centerId, "Center soft deleted: " + center.getName(), request);
    }

    @Transactional
    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public void deleteDrive(Long driveId, HttpServletRequest request) {
        VaccinationDrive drive = driveRepository.findById(driveId).orElseThrow();
        assertDriveAccess(drive);
        deleteDriveInternal(drive, auditService.getCurrentActor());
        auditService.logAction("SOFT_DELETE_DRIVE", "DRIVE", driveId, "Drive soft deleted: " + drive.getTitle(), request);
    }

    @Transactional
    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public void deleteSlot(Long slotId, HttpServletRequest request) {
        Slot slot = slotRepository.findById(slotId).orElseThrow(() -> new AppException("Slot not found"));
        assertSlotAccess(slot);
        deleteSlotInternal(slot, auditService.getCurrentActor());
        auditService.logAction("SOFT_DELETE_SLOT", "SLOT", slotId, "Slot soft deleted", request);
    }

    @Transactional
    @CacheEvict(cacheNames = {"public-summary", "public-centers"}, allEntries = true)
    public void deleteUser(Long userId, HttpServletRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException("User not found"));
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        String actor = auditService.getCurrentActor();

        for (Booking booking : bookings) {
            Slot slot = booking.getSlot();
            if (slot != null && booking.getStatus() != BookingStatus.CANCELLED) {
                slot.setBookedCount(Math.max(0, (slot.getBookedCount() == null ? 0 : slot.getBookedCount()) - 1));
                slotRepository.save(slot);
            }
            softDeleteBooking(booking, actor);
        }

        feedbackRepository.deleteByUserId(userId);
        contactRepository.deleteByUser_Id(userId);
        reviewRepository.deleteByUserId(userId);
        emailVerificationRepository.deleteByUserEmail(user.getEmail());
        passwordResetRepository.deleteByUserEmail(user.getEmail());
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            phoneVerificationRepository.deleteByPhoneNumber(user.getPhoneNumber());
        }
        softDeleteUser(user, actor);
        auditService.logAction("SOFT_DELETE_USER", "USER", userId, "User soft deleted: " + user.getEmail(), request);
    }

    @Transactional
    public void updateUserRole(Long userId, RoleName role, HttpServletRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        Role roleEntity = roleRepository.findByName(role).orElseThrow();
        user.getRoles().clear();
        user.getRoles().add(roleEntity);
        user.setRole(role.name());
        user.setIsSuperAdmin(role == RoleName.SUPER_ADMIN);
        user.setIsAdmin(role == RoleName.ADMIN || role == RoleName.SUPER_ADMIN);
        userRepository.save(user);
        auditService.logAction("UPDATE_ROLE", "USER", userId, "Role updated to " + role, request);
    }

    private User getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private Long getCurrentUserIdOrNull() {
        User currentUser = getCurrentUserOrNull();
        return currentUser != null ? currentUser.getId() : null;
    }

    private boolean isRestrictedAdmin(User user) {
        return user != null && user.isAdmin() && !user.isSuperAdmin();
    }

    private Long getRestrictedAdminIdOrNull() {
        User currentUser = getCurrentUserOrNull();
        return isRestrictedAdmin(currentUser) ? currentUser.getId() : null;
    }

    private Long resolveAdminOwnerForWrite() {
        return getRestrictedAdminIdOrNull();
    }

    private Long resolveDriveOwnerId(VaccinationCenter center) {
        if (center != null && center.getAdminId() != null) {
            return center.getAdminId();
        }
        return resolveAdminOwnerForWrite();
    }

    private Long resolveSlotOwnerId(VaccinationDrive drive) {
        if (drive.getAdminId() != null) {
            return drive.getAdminId();
        }
        return resolveAdminOwnerForWrite();
    }

    private void assertDriveAccess(VaccinationDrive drive) {
        Long restrictedAdminId = getRestrictedAdminIdOrNull();
        if (restrictedAdminId == null) {
            return;
        }
        if (!restrictedAdminId.equals(drive.getAdminId())) {
            throw new AppException("You can only access your own drives");
        }
    }

    private void assertCenterAccess(VaccinationCenter center) {
        Long restrictedAdminId = getRestrictedAdminIdOrNull();
        if (restrictedAdminId == null) {
            return;
        }
        if (!restrictedAdminId.equals(center.getAdminId())) {
            throw new AppException("You can only access your own centers");
        }
    }

    private void assertSlotAccess(Slot slot) {
        Long restrictedAdminId = getRestrictedAdminIdOrNull();
        if (restrictedAdminId == null) {
            return;
        }
        if (!restrictedAdminId.equals(slot.getAdminId())) {
            throw new AppException("You can only access your own slots");
        }
    }

    private void assertBookingAccess(Booking booking) {
        Long restrictedAdminId = getRestrictedAdminIdOrNull();
        if (restrictedAdminId == null) {
            return;
        }
        if (!restrictedAdminId.equals(booking.getAdminId())) {
            throw new AppException("You can only access your own bookings");
        }
    }

    private void assertContactAccess(Contact contact) {
        Long restrictedAdminId = getRestrictedAdminIdOrNull();
        if (restrictedAdminId == null) {
            return;
        }
        if (!restrictedAdminId.equals(contact.getAdminId())) {
            throw new AppException("You can only access your own contacts");
        }
    }

    private void assertFeedbackAccess(Map<String, Object> feedback) {
        Long restrictedAdminId = getRestrictedAdminIdOrNull();
        if (restrictedAdminId == null) {
            return;
        }
        Object adminId = feedback.get("adminId");
        if (!(adminId instanceof Number numberValue) || numberValue.longValue() != restrictedAdminId) {
            throw new AppException("You can only access your own feedback");
        }
    }

    private void deleteDriveInternal(VaccinationDrive drive, String actor) {
        feedbackRepository.deleteByDriveId(drive.getId());
        for (Slot slot : slotRepository.findByDrive_Id(drive.getId())) {
            deleteSlotInternal(slot, actor);
        }
        softDeleteDrive(drive, actor);
    }

    private void deleteSlotInternal(Slot slot, String actor) {
        for (Booking booking : bookingRepository.findBySlotId(slot.getId())) {
            softDeleteBooking(booking, actor);
        }
        softDeleteSlot(slot, actor);
    }

    private void softDeleteBooking(Booking booking, String actor) {
        if (booking.isDeleted()) {
            return;
        }
        booking.setDeletedAt(LocalDateTime.now());
        booking.setDeletedBy(actor);
        bookingRepository.save(booking);
    }

    private void softDeleteUser(User user, String actor) {
        if (user.isDeleted()) {
            return;
        }
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(actor);
        user.setEnabled(false);
        userRepository.save(user);
    }

    private void softDeleteCenter(VaccinationCenter center, String actor) {
        if (center.isDeleted()) {
            return;
        }
        center.setDeletedAt(LocalDateTime.now());
        center.setDeletedBy(actor);
        centerRepository.save(center);
    }

    private void softDeleteDrive(VaccinationDrive drive, String actor) {
        if (drive.isDeleted()) {
            return;
        }
        drive.setDeletedAt(LocalDateTime.now());
        drive.setDeletedBy(actor);
        drive.setActive(false);
        driveRepository.save(drive);
    }

    private void softDeleteSlot(Slot slot, String actor) {
        if (slot.isDeleted()) {
            return;
        }
        slot.setDeletedAt(LocalDateTime.now());
        slot.setDeletedBy(actor);
        slotRepository.save(slot);
    }

    private AdminSlotResponse toAdminSlotResponse(Slot slot) {
        int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
        int remaining = Math.max(0, capacity - bookedCount);
        boolean available = remaining > 0;
        boolean bookable = available && SlotStatusResolver.resolve(slot) != SlotStatus.EXPIRED;
        double fillRate = capacity <= 0 ? 0d : (double) bookedCount / capacity;
        boolean almostFull = capacity > 0 && remaining <= Math.max(1, Math.ceil(capacity * 0.2));
        String demandLevel = fillRate >= 0.85 ? "HIGH_DEMAND" : almostFull ? "ALMOST_FULL" : "NORMAL";

        return new AdminSlotResponse(
            slot.getId(),
            slot.getStartDateTime(),
            SlotStatusResolver.resolveEnd(slot),
            slot.getStartDateTime(),
            SlotStatusResolver.resolveEnd(slot),
            capacity,
            bookedCount,
            remaining,
            available,
            bookable,
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getId() : null,
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getName() : null,
            slot.getDrive() != null && slot.getDrive().getCenter() != null ? slot.getDrive().getCenter().getCity() : null,
            slot.getDrive() != null ? slot.getDrive().getId() : null,
            slot.getDrive() != null ? slot.getDrive().getTitle() : null,
            almostFull,
            demandLevel,
            SlotStatusResolver.resolve(slot).name()
        );
    }

    private VaccinationDrive findDriveOrThrow(Long driveId) {
        if (driveId == null) {
            throw new AppException("Drive is required");
        }

        return driveRepository.findById(driveId)
            .orElseThrow(() -> new AppException("Drive not found"));
    }

    private void validateSlotRequest(SlotRequest req, Long currentSlotId) {
        if (req.getDriveId() == null) {
            throw new AppException("Drive is required");
        }
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new AppException("Slot start date and end date are required");
        }
        if (!req.getEndDate().isAfter(req.getStartDate())) {
            throw new AppException("Slot end date must be after start date");
        }
        if (req.getCapacity() == null || req.getCapacity() < 1) {
            throw new AppException("Slot capacity must be at least 1");
        }

        LocalDateTime requestedStart = req.getStartDate();
        LocalDateTime requestedEnd = req.getEndDate();

        boolean overlapsExistingSlot = slotRepository.findByDrive_Id(req.getDriveId()).stream()
            .filter(existing -> currentSlotId == null || !existing.getId().equals(currentSlotId))
            .anyMatch(existing -> {
                LocalDateTime existingStart = existing.getStartDateTime();
                LocalDateTime existingEnd = SlotStatusResolver.resolveEnd(existing);

                if (existingStart == null || existingEnd == null || !existingEnd.isAfter(existingStart)) {
                    return false;
                }

                return requestedStart.isBefore(existingEnd)
                    && requestedEnd.isAfter(existingStart);
            });

        if (overlapsExistingSlot) {
            throw new AppException("Slot timing overlaps with another slot for this drive");
        }
    }

    private SlotRequest normalizeSlotRequestForUpdate(SlotRequest req, Slot slot) {
        LocalDateTime existingStart = slot.getDateTime();
        LocalDateTime existingEnd = SlotStatusResolver.resolveEnd(slot);
        Duration existingDuration = resolveSlotDuration(existingStart, existingEnd);

        LocalDateTime resolvedStart = req.getStartDate();
        if (resolvedStart == null) {
            resolvedStart = parseLegacySlotDateTime(req.getDate(), req.getTime(), existingStart);
        }
        if (resolvedStart == null) {
            resolvedStart = existingStart;
        }

        LocalDateTime resolvedEnd = req.getEndDate();
        if (resolvedEnd == null && req.getTime() != null && req.getDate() == null) {
            resolvedEnd = parseLegacySlotDateTime(
                resolvedStart != null ? resolvedStart.toLocalDate().toString() : null,
                req.getTime(),
                existingEnd
            );
        }
        if (resolvedEnd == null && resolvedStart != null) {
            resolvedEnd = resolvedStart.plus(existingDuration);
        }
        if (resolvedEnd == null) {
            resolvedEnd = existingEnd;
        }

        Integer resolvedCapacity = req.getCapacity() != null ? req.getCapacity() : slot.getCapacity();
        Long resolvedDriveId = req.getDriveId() != null
            ? req.getDriveId()
            : slot.getDrive() != null ? slot.getDrive().getId() : null;

        SlotRequest normalizedRequest = new SlotRequest();
        normalizedRequest.setDriveId(resolvedDriveId);
        normalizedRequest.setStartDate(resolvedStart);
        normalizedRequest.setEndDate(resolvedEnd);
        normalizedRequest.setCapacity(resolvedCapacity);
        normalizedRequest.setDate(req.getDate());
        normalizedRequest.setTime(req.getTime());
        normalizedRequest.setAvailable(req.getAvailable());
        return normalizedRequest;
    }

    private Duration resolveSlotDuration(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            return Duration.ofHours(1);
        }
        return Duration.between(startDate, endDate);
    }

    private LocalDateTime parseLegacySlotDateTime(String dateValue, String timeValue, LocalDateTime fallback) {
        if ((dateValue == null || dateValue.isBlank()) && (timeValue == null || timeValue.isBlank())) {
            return null;
        }

        if (timeValue != null && !timeValue.isBlank()) {
            try {
                return LocalDateTime.parse(timeValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                // Fall through to date + time parsing below.
            }
        }

        LocalDate resolvedDate;
        if (dateValue != null && !dateValue.isBlank()) {
            try {
                resolvedDate = LocalDate.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ex) {
                throw new AppException("Slot date must use yyyy-MM-dd format");
            }
        } else if (fallback != null) {
            resolvedDate = fallback.toLocalDate();
        } else {
            throw new AppException("Slot date is required");
        }

        LocalTime resolvedTime;
        if (timeValue != null && !timeValue.isBlank()) {
            try {
                resolvedTime = LocalTime.parse(timeValue, DateTimeFormatter.ofPattern("H:mm[:ss]"));
            } catch (DateTimeParseException ex) {
                throw new AppException("Slot time must use HH:mm or HH:mm:ss format");
            }
        } else if (fallback != null) {
            resolvedTime = fallback.toLocalTime();
        } else {
            throw new AppException("Slot time is required");
        }

        return resolvedDate.atTime(resolvedTime);
    }
}
