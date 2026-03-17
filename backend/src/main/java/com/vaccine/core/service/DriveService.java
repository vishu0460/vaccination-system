package com.vaccine.core.service;

import com.vaccine.common.dto.DriveResponse;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriveService {

    private final VaccinationDriveRepository driveRepository;

    public DriveService(VaccinationDriveRepository driveRepository) {
        this.driveRepository = driveRepository;
    }

    public List<DriveResponse> getActiveDrives(String city, LocalDate fromDate, Integer age) {
        return driveRepository.findActiveDrives(city, fromDate, age)
                .stream()
                .map(DriveResponse::from)
                .collect(Collectors.toList());
    }

    public DriveResponse getDriveById(Long driveId) {
        return driveRepository.findById(driveId)
                .map(DriveResponse::from)
                .orElseThrow(() -> new AppException("Drive not found: " + driveId));
    }

    public List<DriveResponse> getAllActiveDrives() {
        return driveRepository.findByActiveTrue()
                .stream()
                .map(DriveResponse::from)
                .collect(Collectors.toList());
    }
}
