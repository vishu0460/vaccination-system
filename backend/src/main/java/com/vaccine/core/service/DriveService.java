package com.vaccine.core.service;

import com.vaccine.common.dto.DriveResponse;
import com.vaccine.domain.Status;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriveService {
    private static final List<Status> BOOKABLE_DRIVE_STATUSES = List.of(Status.UPCOMING, Status.LIVE);

    private final VaccinationDriveRepository driveRepository;

    public DriveService(VaccinationDriveRepository driveRepository) {
        this.driveRepository = driveRepository;
    }

    public List<DriveResponse> getActiveDrives(String city, LocalDate fromDate, Integer age) {
        return driveRepository.findVisibleDrives(BOOKABLE_DRIVE_STATUSES, city, fromDate, age)
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
        return driveRepository.findByStatusIn(BOOKABLE_DRIVE_STATUSES)
                .stream()
                .map(DriveResponse::from)
                .collect(Collectors.toList());
    }
}
