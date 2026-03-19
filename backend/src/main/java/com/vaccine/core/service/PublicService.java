package com.vaccine.core.service;

import com.vaccine.common.dto.SummaryResponse;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.domain.Slot;
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

    private final VaccinationCenterRepository centerRepository;
    private final VaccinationDriveRepository driveRepository;
    private final SlotRepository slotRepository;

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
        List<VaccinationDrive> drives = driveRepository.findActiveDrives(city, fromDate, age);
        Map<String, Object> result = new HashMap<>();
        result.put("drives", drives);
        result.put("totalPages", 1);
        result.put("totalElements", drives.size());
        return result;
    }

@Cacheable("public-summary")
    public SummaryResponse getSummary() {
        long totalCenters = centerRepository.count();
        long activeDrives = driveRepository.countByActiveTrue();
        long totalSlots = slotRepository.count();
        long totalBookings = 0L;
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
