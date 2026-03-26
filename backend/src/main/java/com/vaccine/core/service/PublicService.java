package com.vaccine.core.service;

import com.vaccine.common.dto.SummaryResponse;
import com.vaccine.domain.Status;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.common.dto.SlotDetailResponse;
import com.vaccine.domain.Slot;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationCenterRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.util.DriveStatusResolver;
import com.vaccine.util.SlotStatusResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicService {
    private static final List<Status> BOOKABLE_DRIVE_STATUSES = List.of(Status.UPCOMING, Status.LIVE);
    private static final List<String> FALLBACK_CITIES = List.of(
        "Ahmedabad", "Bengaluru", "Bhopal", "Bhubaneswar", "Chandigarh", "Chennai",
        "Coimbatore", "Dehradun", "Delhi", "Faridabad", "Ghaziabad", "Gurugram",
        "Guwahati", "Hyderabad", "Indore", "Jaipur", "Kanpur", "Kochi", "Kolkata",
        "Lucknow", "Ludhiana", "Mumbai", "Mysuru", "Nagpur", "Noida", "Patna",
        "Pune", "Raipur", "Ranchi", "Surat", "Thane", "Vadodara", "Varanasi", "Visakhapatnam"
    );

    private final VaccinationCenterRepository centerRepository;
    private final VaccinationDriveRepository driveRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    @Cacheable(value = "public-centers", key = "T(java.lang.String).valueOf(#city) + ':' + #page + ':' + #size")
    public Map<String, Object> getCenters(String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedCity = normalize(city);
        Page<VaccinationCenter> centers = centerRepository.searchPublicCenters(normalizedCity, pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("centers", centers.getContent());
        result.put("totalPages", centers.getTotalPages());
        result.put("totalElements", centers.getTotalElements());
        return result;
    }

    public Map<String, Object> getDrives(String city, LocalDate date, String age, String vaccineType,
                                         Boolean available, String slot, int page, int size) {
        Specification<VaccinationDrive> specification = buildDriveSpecification(city, date, age, vaccineType);
        List<VaccinationDrive> drives = driveRepository.findAll(
            specification,
            Sort.by(Sort.Direction.ASC, "driveDate", "startTime")
        );

        List<Map<String, Object>> driveViews = drives.stream()
            .map(drive -> {
                List<Slot> allVisibleSlots = getVisibleSlots(drive.getId());
                List<Slot> matchingSlots = filterSlotsByWindow(allVisibleSlots, slot);
                long totalSlots = matchingSlots.stream()
                    .mapToLong(matchingSlot -> matchingSlot.getCapacity() == null ? 0 : matchingSlot.getCapacity())
                    .sum();
                long availableSlots = matchingSlots.stream()
                    .mapToLong(matchingSlot -> {
                        int capacity = matchingSlot.getCapacity() == null ? 0 : matchingSlot.getCapacity();
                        int bookedCount = matchingSlot.getBookedCount() == null ? 0 : matchingSlot.getBookedCount();
                        return Math.max(0, capacity - bookedCount);
                    })
                    .sum();
                boolean hasMatchingAvailability = available == null
                    || (available && availableSlots > 0)
                    || (!available && availableSlots == 0);
                if (!hasMatchingAvailability) {
                    return null;
                }

                long visibleTotalSlots = matchingSlots.isEmpty()
                    ? (drive.getTotalSlots() == null ? 0 : drive.getTotalSlots())
                    : totalSlots;
                String realtimeStatus = DriveStatusResolver.resolve(drive);

                Map<String, Object> view = new HashMap<>();
                view.put("id", drive.getId());
                view.put("title", drive.getTitle());
                view.put("description", drive.getDescription());
                view.put("vaccineType", drive.getVaccineType());
                view.put("driveDate", drive.getDriveDate());
                view.put("minAge", drive.getMinAge());
                view.put("maxAge", drive.getMaxAge());
                view.put("startTime", drive.getStartTime());
                view.put("endTime", drive.getEndTime());
                view.put("startDateTime", DriveStatusResolver.resolveStart(drive));
                view.put("endDateTime", DriveStatusResolver.resolveEnd(drive));
                view.put("status", drive.getStatus());
                view.put("realtimeStatus", realtimeStatus);
                view.put("bookable", !"EXPIRED".equals(realtimeStatus) && availableSlots > 0);
                view.put("available", availableSlots > 0);
                view.put("centerName", drive.getCenter() != null ? drive.getCenter().getName() : null);
                view.put("centerCity", drive.getCenter() != null ? drive.getCenter().getCity() : null);
                view.put("totalSlots", visibleTotalSlots);
                view.put("availableSlots", availableSlots);
                view.put("slotWindow", slot);
                view.put("ageLabel", formatAgeLabel(drive.getMinAge()));
                return view;
            })
            .filter(java.util.Objects::nonNull)
            .toList();

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        int startIndex = Math.min((int) pageable.getOffset(), driveViews.size());
        int endIndex = Math.min(startIndex + pageable.getPageSize(), driveViews.size());
        Page<Map<String, Object>> pagedDrives = new PageImpl<>(driveViews.subList(startIndex, endIndex), pageable, driveViews.size());

        Map<String, Object> result = new HashMap<>();
        result.put("drives", pagedDrives.getContent());
        result.put("totalPages", pagedDrives.getTotalPages());
        result.put("totalElements", pagedDrives.getTotalElements());
        return result;
    }

    @Cacheable(value = "public-cities", key = "T(java.lang.String).valueOf(#query) + ':' + #limit")
    public List<String> getCitySuggestions(String query, int limit) {
        String normalizedQuery = normalize(query);
        Set<String> suggestions = new LinkedHashSet<>();

        centerRepository.findDistinctCities().stream()
            .filter(city -> matchesCityQuery(city, normalizedQuery))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .limit(limit)
            .forEach(suggestions::add);

        if (suggestions.size() < limit) {
            FALLBACK_CITIES.stream()
                .filter(city -> matchesCityQuery(city, normalizedQuery))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(suggestions::add);
        }

        return suggestions.stream()
            .sorted(Comparator.comparing(String::toLowerCase))
            .limit(limit)
            .toList();
    }

    @Cacheable("public-summary")
    public SummaryResponse getSummary() {
        long totalCenters = centerRepository.count();
        long activeDrives = driveRepository.countByStatusIn(BOOKABLE_DRIVE_STATUSES);
        long totalSlots = slotRepository.findAll().stream()
            .filter(slot -> SlotStatusResolver.resolve(slot) != com.vaccine.domain.SlotStatus.EXPIRED)
            .mapToLong(slot -> {
                int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
                int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
                return Math.max(0, capacity - bookedCount);
            })
            .sum();
        long totalBookings = bookingRepository.count();
        return new SummaryResponse(totalCenters, activeDrives, totalSlots, totalBookings);
    }

    public Optional<VaccinationCenter> getCenterDetail(Long id) {
        return centerRepository.findById(id);
    }

    public List<SlotDetailResponse> getDriveSlots(Long driveId) {
        return getVisibleSlots(driveId).stream()
            .map(SlotDetailResponse::from)
            .toList();
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private Specification<VaccinationDrive> buildDriveSpecification(String city, LocalDate date, String age, String vaccineType) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(root.get("status").in(BOOKABLE_DRIVE_STATUSES));
            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            String normalizedCity = normalize(city);
            if (normalizedCity != null) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.join("center").get("city")),
                    "%" + normalizedCity.toLowerCase(Locale.ROOT) + "%"
                ));
            }

            if (date != null) {
                predicates.add(criteriaBuilder.equal(root.get("driveDate"), date));
            }

            Integer ageValue = parseAgeValue(age);
            if (ageValue != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("minAge"), ageValue));
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("maxAge"), ageValue));
            }

            String normalizedVaccineType = normalize(vaccineType);
            if (normalizedVaccineType != null) {
                String vaccineKey = normalizedVaccineType.toLowerCase(Locale.ROOT);
                if ("others".equals(vaccineKey)) {
                    predicates.add(criteriaBuilder.not(root.get("vaccineType").in(List.of("Covishield", "Covaxin"))));
                } else {
                    predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("vaccineType")), vaccineKey));
                }
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private boolean matchesCityQuery(String city, String query) {
        if (city == null || city.isBlank()) {
            return false;
        }

        if (query == null) {
            return true;
        }

        return city.toLowerCase().contains(query.toLowerCase());
    }

    private List<Slot> getVisibleSlots(Long driveId) {
        return slotRepository.findByDrive_IdOrderByDateTimeAsc(driveId).stream()
            .filter(slot -> SlotStatusResolver.resolve(slot) != com.vaccine.domain.SlotStatus.EXPIRED)
            .toList();
    }

    private List<Slot> filterSlotsByWindow(List<Slot> slots, String slotWindow) {
        String normalizedWindow = normalize(slotWindow);
        if (normalizedWindow == null) {
            return slots;
        }

        return slots.stream()
            .filter(slot -> matchesSlotWindow(slot, normalizedWindow))
            .toList();
    }

    private boolean matchesSlotWindow(Slot slot, String slotWindow) {
        LocalTime startTime = slot.getStartTime() != null
            ? slot.getStartTime()
            : slot.getDateTime() != null ? slot.getDateTime().toLocalTime() : null;
        if (startTime == null) {
            return false;
        }

        return switch (slotWindow.toLowerCase(Locale.ROOT)) {
            case "morning" -> !startTime.isBefore(LocalTime.of(5, 0)) && startTime.isBefore(LocalTime.NOON);
            case "afternoon" -> !startTime.isBefore(LocalTime.NOON) && startTime.isBefore(LocalTime.of(17, 0));
            case "evening" -> !startTime.isBefore(LocalTime.of(17, 0));
            default -> true;
        };
    }

    private Integer parseAgeValue(String age) {
        String normalizedAge = normalize(age);
        if (normalizedAge == null) {
            return null;
        }

        return switch (normalizedAge) {
            case "18+" -> 18;
            case "45+" -> 45;
            default -> null;
        };
    }

    private String formatAgeLabel(Integer minAge) {
        if (minAge == null) {
            return "All Ages";
        }
        return minAge + "+";
    }
}
