package com.vaccine.core.service;

import com.vaccine.common.dto.CenterRequest;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.VaccinationCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CenterService {
    private final VaccinationCenterRepository centerRepository;

    @Transactional
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

    public List<VaccinationCenter> getCenters(String city) {
        if (!StringUtils.hasText(city)) {
            return centerRepository.findAll();
        }
        return centerRepository.findByCityIgnoreCase(city.trim());
    }

    @Cacheable(value = "centers", key = "#city")
    public Page<VaccinationCenter> getCenters(String city, Pageable pageable) {
        if (!StringUtils.hasText(city)) {
            return centerRepository.findAll(pageable);
        }
        List<VaccinationCenter> centers = centerRepository.findByCityIgnoreCase(city.trim());
        long total = centers.size();
        List<VaccinationCenter> pageContent = centers.stream()
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .collect(Collectors.toList());
        return new PageImpl<>(pageContent, pageable, total);
    }

    public Page<VaccinationCenter> getAllCenters(Pageable pageable) {
        return centerRepository.findAll(pageable);
    }

    public VaccinationCenter getCenterById(Long id) {
        return centerRepository.findById(id).orElseThrow(() -> new AppException("Center not found"));
    }

    public long countCenters() {
        return centerRepository.count();
    }
}

