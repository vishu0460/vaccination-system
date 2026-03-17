package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.VaccinationCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VaccinationCenterRepository extends JpaRepository<VaccinationCenter, Long> {
    Optional<VaccinationCenter> findByNameIgnoreCase(String name);
}
