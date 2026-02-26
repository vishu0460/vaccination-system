package com.vaccine.repository;

import com.vaccine.entity.VaccinationCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaccinationCenterRepository extends JpaRepository<VaccinationCenter, Long> {
    
    List<VaccinationCenter> findByIsActiveTrue();
    
    List<VaccinationCenter> findByCity(String city);
    
    @Query("SELECT DISTINCT c.city FROM VaccinationCenter c WHERE c.isActive = true ORDER BY c.city")
    List<String> findAllCities();
    
    Optional<VaccinationCenter> findByName(String name);
}
