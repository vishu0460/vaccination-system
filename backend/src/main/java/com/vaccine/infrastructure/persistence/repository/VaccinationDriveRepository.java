package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.VaccinationDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationDriveRepository extends JpaRepository<VaccinationDrive, Long> {
    List<VaccinationDrive> findByActiveTrue();
    long countByActiveTrue();
    
    @Query("SELECT d FROM VaccinationDrive d WHERE d.active = true " +
           "AND (:city IS NULL OR d.center.city = :city) " +
           "AND (:fromDate IS NULL OR d.startDate >= :fromDate) " +
           "AND (:age IS NULL OR d.minAge <= :age)")
    List<VaccinationDrive> findActiveDrives(@Param("city") String city, 
                                            @Param("fromDate") LocalDate fromDate, 
                                            @Param("age") Integer age);
}
