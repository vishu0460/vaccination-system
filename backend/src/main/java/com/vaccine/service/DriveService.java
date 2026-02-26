package com.vaccine.service;

import com.vaccine.dto.DriveDTO;
import com.vaccine.entity.VaccinationCenter;
import com.vaccine.entity.VaccinationDrive;
import com.vaccine.repository.VaccinationCenterRepository;
import com.vaccine.repository.VaccinationDriveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriveService {
    
    @Autowired
    private VaccinationDriveRepository driveRepository;
    
    @Autowired
    private VaccinationCenterRepository centerRepository;
    
    public List<DriveDTO> getAllDrives() {
        return driveRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<DriveDTO> getActiveDrives() {
        return driveRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<DriveDTO> getAllActiveDrives() {
        return driveRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<DriveDTO> getActiveDrivesBetweenDates(LocalDate start, LocalDate end) {
        return driveRepository.findActiveDrivesBetweenDates(start, end).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public DriveDTO getDriveById(Long id) {
        VaccinationDrive drive = driveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drive not found"));
        return mapToDTO(drive);
    }
    
    public List<DriveDTO> getDrivesByCenter(Long centerId) {
        return driveRepository.findByCenterId(centerId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<DriveDTO> getDrivesByDateRange(LocalDate startDate, LocalDate endDate) {
        return driveRepository.findActiveDrivesBetweenDates(startDate, endDate).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public DriveDTO createDrive(DriveDTO dto) {
        VaccinationCenter center = centerRepository.findById(dto.getCenterId())
                .orElseThrow(() -> new RuntimeException("Center not found"));
        
        VaccinationDrive drive = new VaccinationDrive();
        drive.setName(dto.getName());
        drive.setDescription(dto.getDescription());
        drive.setVaccineName(dto.getVaccineName());
        drive.setVaccineManufacturer(dto.getVaccineManufacturer());
        drive.setMinAge(dto.getMinAge());
        drive.setMaxAge(dto.getMaxAge());
        drive.setDosesRequired(dto.getDosesRequired());
        drive.setDoseGapDays(dto.getDoseGapDays());
        drive.setCenter(center);
        drive.setStartDate(dto.getStartDate());
        drive.setEndDate(dto.getEndDate());
        drive.setStartTime(dto.getStartTime());
        drive.setEndTime(dto.getEndTime());
        drive.setTotalSlots(dto.getTotalSlots());
        drive.setAvailableSlots(dto.getAvailableSlots());
        drive.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        drive = driveRepository.save(drive);
        return mapToDTO(drive);
    }
    
    @Transactional
    public DriveDTO updateDrive(Long id, DriveDTO dto) {
        VaccinationDrive drive = driveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drive not found"));
        
        drive.setName(dto.getName());
        drive.setDescription(dto.getDescription());
        drive.setVaccineName(dto.getVaccineName());
        drive.setVaccineManufacturer(dto.getVaccineManufacturer());
        drive.setMinAge(dto.getMinAge());
        drive.setMaxAge(dto.getMaxAge());
        drive.setDosesRequired(dto.getDosesRequired());
        drive.setDoseGapDays(dto.getDoseGapDays());
        
        if (dto.getCenterId() != null) {
            VaccinationCenter center = centerRepository.findById(dto.getCenterId())
                    .orElseThrow(() -> new RuntimeException("Center not found"));
            drive.setCenter(center);
        }
        
        drive.setStartDate(dto.getStartDate());
        drive.setEndDate(dto.getEndDate());
        drive.setStartTime(dto.getStartTime());
        drive.setEndTime(dto.getEndTime());
        
        if (dto.getIsActive() != null) {
            drive.setIsActive(dto.getIsActive());
        }
        
        drive = driveRepository.save(drive);
        return mapToDTO(drive);
    }
    
    @Transactional
    public void deleteDrive(Long id) {
        VaccinationDrive drive = driveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drive not found"));
        drive.setIsActive(false);
        driveRepository.save(drive);
    }
    
    private DriveDTO mapToDTO(VaccinationDrive drive) {
        DriveDTO dto = new DriveDTO();
        dto.setId(drive.getId());
        dto.setName(drive.getName());
        dto.setDescription(drive.getDescription());
        dto.setVaccineName(drive.getVaccineName());
        dto.setVaccineManufacturer(drive.getVaccineManufacturer());
        dto.setMinAge(drive.getMinAge());
        dto.setMaxAge(drive.getMaxAge());
        dto.setDosesRequired(drive.getDosesRequired());
        dto.setDoseGapDays(drive.getDoseGapDays());
        dto.setCenterId(drive.getCenter().getId());
        dto.setCenterName(drive.getCenter().getName());
        dto.setStartDate(drive.getStartDate());
        dto.setEndDate(drive.getEndDate());
        dto.setStartTime(drive.getStartTime());
        dto.setEndTime(drive.getEndTime());
        dto.setTotalSlots(drive.getTotalSlots());
        dto.setAvailableSlots(drive.getAvailableSlots());
        dto.setIsActive(drive.getIsActive());
        return dto;
    }
}
