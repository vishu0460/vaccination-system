package com.vaccine.repository;

import com.vaccine.entity.VaccinationDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationDriveRepository extends JpaRepository<VaccinationDrive, Long> {
    
    List<VaccinationDrive> findByIsActiveTrue();
    
    @Query("SELECT d FROM VaccinationDrive d WHERE d.isActive = true AND d.startDate <= :endDate AND d.endDate >= :startDate")
    List<VaccinationDrive> findActiveDrivesBetweenDates(LocalDate startDate, LocalDate endDate);
    
    List<VaccinationDrive> findByCenterId(Long centerId);
}
