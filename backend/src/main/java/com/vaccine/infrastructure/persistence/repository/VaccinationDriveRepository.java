package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.VaccinationDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationDriveRepository extends JpaRepository<VaccinationDrive, Long> {
    List<VaccinationDrive> findActiveDrives(String city, LocalDate fromDate, Integer age);
    List<VaccinationDrive> findByActiveTrue();
}
