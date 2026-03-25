package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.VaccinationCenter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaccinationCenterRepository extends JpaRepository<VaccinationCenter, Long> {
Optional<VaccinationCenter> findByNameIgnoreCase(String name);
    
    List<VaccinationCenter> findByCityIgnoreCase(String city);
    List<VaccinationCenter> findByAdminId(Long adminId);
    
    Page<VaccinationCenter> findByCityContainingIgnoreCase(String city, Pageable pageable);

    @Query("""
        SELECT c
        FROM VaccinationCenter c
        WHERE :keyword IS NULL
           OR LOWER(c.city) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(c.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    Page<VaccinationCenter> searchPublicCenters(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT DISTINCT c.city
        FROM VaccinationCenter c
        WHERE c.city IS NOT NULL AND TRIM(c.city) <> ''
        ORDER BY c.city ASC
        """)
    List<String> findDistinctCities();
}
