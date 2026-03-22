package com.vaccine.core.service;

import com.vaccine.common.dto.SummaryResponse;
import com.vaccine.domain.Status;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.domain.Slot;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationCenterRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicService {
    private static final List<Status> BOOKABLE_DRIVE_STATUSES = List.of(Status.UPCOMING, Status.LIVE);

    private final VaccinationCenterRepository centerRepository;
    private final VaccinationDriveRepository driveRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    @Cacheable(value = "public-centers", key = "T(java.lang.String).valueOf(#city) + ':' + #page + ':' + #size")
    public Map<String, Object> getCenters(String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VaccinationCenter> centers;
        if (city != null && !city.isBlank()) {
            centers = centerRepository.findByCityContainingIgnoreCase(city, pageable);
        } else {
            centers = centerRepository.findAll(pageable);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("centers", centers.getContent());
        result.put("totalPages", centers.getTotalPages());
        result.put("totalElements", centers.getTotalElements());
        return result;
    }

    public Map<String, Object> getDrives(String city, LocalDate fromDate, Integer age, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<VaccinationDrive> drives = driveRepository.findVisibleDrives(BOOKABLE_DRIVE_STATUSES, city, fromDate, age);
        List<Map<String, Object>> driveViews = drives.stream()
            .map(drive -> {
                long totalSlots = slotRepository.sumCapacityByDriveId(drive.getId());
                long availableSlots = slotRepository.sumAvailableCapacityByDriveId(drive.getId());

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
                view.put("status", drive.getStatus());
                view.put("centerName", drive.getCenter() != null ? drive.getCenter().getName() : null);
                view.put("centerCity", drive.getCenter() != null ? drive.getCenter().getCity() : null);
                view.put("totalSlots", totalSlots);
                view.put("availableSlots", availableSlots);
                return view;
            })
            .toList();
        Map<String, Object> result = new HashMap<>();
        result.put("drives", driveViews);
        result.put("totalPages", 1);
        result.put("totalElements", driveViews.size());
        return result;
    }

    @Cacheable("public-summary")
    public SummaryResponse getSummary() {
        long totalCenters = centerRepository.count();
        long activeDrives = driveRepository.countByStatusIn(BOOKABLE_DRIVE_STATUSES);
        long totalSlots = slotRepository.sumAvailableCapacity();
        long totalBookings = bookingRepository.count();
        return new SummaryResponse(totalCenters, activeDrives, totalSlots, totalBookings);
    }

    public Optional<VaccinationCenter> getCenterDetail(Long id) {
        return centerRepository.findById(id);
    }

    public List<Slot> getDriveSlots(Long driveId) {
        return slotRepository.findByDriveIdOrderByStartTimeAsc(driveId);
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
