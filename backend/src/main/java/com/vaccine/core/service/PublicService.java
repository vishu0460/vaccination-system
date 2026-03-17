package com.vaccine.core.service;

import com.vaccine.dto.SummaryResponse;
import com.vaccine.core.model.VaccinationCenter;
import com.vaccine.core.model.VaccinationDrive;
import com.vaccine.core.model.Slot;
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

    @Cacheable(value = "public-drives", key = "T(java.lang.String).valueOf(#city) + ':' + #page + ':' + #size")
    public Map<String, Object> getDrives(String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VaccinationDrive> drives = driveRepository.findActiveDrivesPaged(
            normalize(city), null, null, pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("drives", drives.getContent());
        result.put("totalPages", drives.getTotalPages());
        result.put("totalElements", drives.getTotalElements());
        return result;
    }

    @Cacheable(value = "public-drives", key = "T(java.lang.String).valueOf(#city) + ':' + T(java.lang.String).valueOf(#fromDate) + ':' + T(java.lang.String).valueOf(#age) + ':' + #page + ':' + #size")
    public Map<String, Object> getDrives(String city, java.time.LocalDate fromDate, Integer age, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VaccinationDrive> drives = driveRepository.findActiveDrivesPaged(
            normalize(city), fromDate, age, pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("drives", drives.getContent());
        result.put("totalPages", drives.getTotalPages());
        result.put("totalElements", drives.getTotalElements());
        return result;
    }

    @Cacheable("public-summary")
    public SummaryResponse getSummary() {
        long totalCenters = centerRepository.count();
        long activeDrives = driveRepository.countByActiveTrue();
        long availableSlots = slotRepository.countAvailableSlots();
        return new SummaryResponse(totalCenters, activeDrives, availableSlots);
    }

    public Optional<VaccinationCenter> getCenterDetail(Long id) {
        return centerRepository.findById(id);
    }

    public List<Slot> getDriveSlots(Long driveId) {
        return slotRepository.findAvailableByDriveId(driveId);
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
