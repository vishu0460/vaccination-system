package com.vaccine.config;

import com.vaccine.domain.Role;
import com.vaccine.domain.RoleName;
import com.vaccine.domain.Slot;
import com.vaccine.domain.Status;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.infrastructure.persistence.repository.RoleRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationCenterRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
@Transactional
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final VaccinationCenterRepository centerRepository;
    private final VaccinationDriveRepository driveRepository;
    private final SlotRepository slotRepository;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        seedRoles();

        if (!seedEnabled) {
            log.info("Development sample seeding skipped because APP_SEED_ENABLED is not true.");
            return;
        }

        seedCenters();
        seedDrives();
        seedSlots();
        log.info("Development sample data seeded successfully.");
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
        }
    }

    private void seedCenters() {
        if (centerRepository.count() > 0) {
            return;
        }

        List<VaccinationCenter> centers = List.of(
            VaccinationCenter.builder()
                .name("Sample Community Health Center")
                .address("100 Demo Street")
                .city("Bengaluru")
                .state("Karnataka")
                .pincode("560001")
                .phone("0800000000")
                .email("contact@example.invalid")
                .dailyCapacity(120)
                .build(),
            VaccinationCenter.builder()
                .name("Sample District Hospital")
                .address("200 Example Avenue")
                .city("Delhi")
                .state("Delhi")
                .pincode("110001")
                .phone("0110000000")
                .email("support@example.invalid")
                .dailyCapacity(160)
                .build()
        );

        centerRepository.saveAll(centers);
    }

    private void seedDrives() {
        if (driveRepository.count() > 0) {
            return;
        }

        List<VaccinationDrive> drives = new ArrayList<>();
        LocalDate firstDate = LocalDate.now().plusDays(1);
        List<VaccinationCenter> centers = centerRepository.findAll();

        for (int index = 0; index < centers.size(); index++) {
            VaccinationCenter center = centers.get(index);
            drives.add(VaccinationDrive.builder()
                .center(center)
                .title("Sample Immunization Drive " + (index + 1))
                .description("Non-production sample drive for development only.")
                .vaccineType(index % 2 == 0 ? "Covishield" : "Covaxin")
                .driveDate(firstDate.plusDays(index))
                .minAge(18)
                .maxAge(60)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .totalSlots(90)
                .status(Status.UPCOMING)
                .active(true)
                .build());
        }

        driveRepository.saveAll(drives);
    }

    private void seedSlots() {
        if (slotRepository.count() > 0) {
            return;
        }

        List<Slot> slots = new ArrayList<>();
        for (VaccinationDrive drive : driveRepository.findAll()) {
            LocalDateTime firstSlot = drive.getDriveDate().atTime(drive.getStartTime());
            slots.add(Slot.builder()
                .drive(drive)
                .adminId(drive.getAdminId())
                .dateTime(firstSlot)
                .startTime(firstSlot.toLocalTime())
                .endTime(firstSlot.plusHours(2).toLocalTime())
                .capacity(30)
                .bookedCount(0)
                .build());
            slots.add(Slot.builder()
                .drive(drive)
                .adminId(drive.getAdminId())
                .dateTime(firstSlot.plusHours(3))
                .startTime(firstSlot.plusHours(3).toLocalTime())
                .endTime(firstSlot.plusHours(5).toLocalTime())
                .capacity(30)
                .bookedCount(0)
                .build());
        }

        slotRepository.saveAll(slots);
    }
}
