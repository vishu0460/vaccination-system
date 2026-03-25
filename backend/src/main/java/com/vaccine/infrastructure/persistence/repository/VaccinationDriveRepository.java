package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Status;
import com.vaccine.domain.VaccinationDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VaccinationDriveRepository extends JpaRepository<VaccinationDrive, Long>, JpaSpecificationExecutor<VaccinationDrive> {
    List<VaccinationDrive> findByStatusIn(List<Status> statuses);
    List<VaccinationDrive> findByCenterId(Long centerId);
    long countByStatusIn(List<Status> statuses);

    @Query("""
        select distinct d
        from VaccinationDrive d
        left join d.center c
        where d.status in :statuses
          and d.active = true
          and (:city is null or lower(c.city) like lower(concat('%', :city, '%')))
          and (:fromDate is null or d.driveDate >= :fromDate)
          and (:age is null or (d.minAge <= :age and d.maxAge >= :age))
        order by d.driveDate asc, d.startTime asc
        """)
    List<VaccinationDrive> findVisibleDrives(
        @Param("statuses") List<Status> statuses,
        @Param("city") String city,
        @Param("fromDate") LocalDate fromDate,
        @Param("age") Integer age
    );
}
