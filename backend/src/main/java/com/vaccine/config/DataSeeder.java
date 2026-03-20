package com.vaccine.config;

import com.vaccine.domain.*;
import com.vaccine.domain.BookingStatus;
import com.vaccine.infrastructure.persistence.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@Transactional
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccinationDriveRepository driveRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired(required = false)
    private NewsRepository newsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:vaxzone.vaccine@gmail.com}")
    private String defaultAdminEmail;

    @Value("${app.admin.password:Vaccine@#6030}")
    private String defaultAdminPassword;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Starting DataSeeder...");

        seedRoles();

        if (!seedEnabled) {
            ensureDefaultSuperAdmin();
            log.info("Data seeding disabled via app.seed.enabled=false");
            return;
        }

        seedUsers();
        ensureDefaultSuperAdmin();
        seedCenters();
        seedDrives();
        seedSlots();
        seedBookings();
        seedCertificates();
        seedReviews();
        seedNews();

        log.info("✅ Data seeding completed successfully!");
    }

    private void seedRoles() {
        if (roleRepository.count() == 0) {
            RoleName[] roleNames = {RoleName.USER, RoleName.ADMIN, RoleName.CENTER_ADMIN, RoleName.SUPER_ADMIN};
            for (RoleName roleName : roleNames) {
                Role role = Role.builder()
                        .name(roleName)
                        .build();
                roleRepository.save(role);
            }
            log.info("Seeded {} roles", roleNames.length);
        }
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            List<User> users = Arrays.asList(
                    // Regular users (ROLE_USER)
                    User.builder().email("john.doe@example.com").fullName("John Doe").password(passwordEncoder.encode("password123")).age(30).phoneNumber("+91-9876543210").userCity("Delhi").emailVerified(true).build(),
                    User.builder().email("jane.smith@example.com").fullName("Jane Smith").password(passwordEncoder.encode("password123")).age(28).phoneNumber("+91-9876543211").userCity("Mumbai").emailVerified(true).build(),
                    User.builder().email("raj.kumar@example.com").fullName("Raj Kumar").password(passwordEncoder.encode("password123")).age(35).phoneNumber("+91-9876543212").userCity("Bangalore").emailVerified(true).build(),
                    User.builder().email("priya.sharma@example.com").fullName("Priya Sharma").password(passwordEncoder.encode("password123")).age(25).phoneNumber("+91-9876543213").userCity("Delhi").emailVerified(true).build(),
                    User.builder().email("vikas.singh@example.com").fullName("Vikas Singh").password(passwordEncoder.encode("password123")).age(40).phoneNumber("+91-9876543214").userCity("Mumbai").emailVerified(true).build(),
                    User.builder().email("anita.patel@example.com").fullName("Anita Patel").password(passwordEncoder.encode("password123")).age(32).phoneNumber("+91-9876543215").userCity("Bangalore").emailVerified(true).build(),
                    User.builder().email("rohit.mehta@example.com").fullName("Rohit Mehta").password(passwordEncoder.encode("password123")).age(29).phoneNumber("+91-9876543216").userCity("Delhi").emailVerified(true).build(),
                    User.builder().email("neha.gupta@example.com").fullName("Neha Gupta").password(passwordEncoder.encode("password123")).age(27).phoneNumber("+91-9876543217").userCity("Mumbai").emailVerified(true).build(),
                    // Admins
                    User.builder().email("admin@test.com").fullName("System Admin").password(passwordEncoder.encode("admin123")).age(35).isAdmin(true).phoneNumber("+91-9000000001").userCity("Delhi").emailVerified(true).build(),
                    User.builder().email("centeradmin@test.com").fullName("Center Admin").password(passwordEncoder.encode("admin123")).age(38).phoneNumber("+91-9000000002").userCity("Mumbai").emailVerified(true).build(),
                    User.builder().email("superadmin@test.com").fullName("Super Admin").password(passwordEncoder.encode("super123")).age(45).isSuperAdmin(true).phoneNumber("+91-9000000003").userCity("Delhi").emailVerified(true).build()
            );

            // Assign roles
            Role userRole = roleRepository.findByName(RoleName.USER).get();
            Role adminRole = roleRepository.findByName(RoleName.ADMIN).get();
            Role centerAdminRole = roleRepository.findByName(RoleName.CENTER_ADMIN).get();
            Role superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN).get();

            users.get(0).setRoles(new HashSet<>(Set.of(userRole)));
            users.get(1).setRoles(new HashSet<>(Set.of(userRole)));
            users.get(2).setRoles(new HashSet<>(Set.of(userRole)));
            users.get(3).setRoles(new HashSet<>(Set.of(userRole)));
            users.get(4).setRoles(new HashSet<>(Set.of(userRole)));
            users.get(5).setRoles(new HashSet<>(Set.of(userRole)));
            users.get(6).setRoles(new HashSet<>(Set.of(userRole)));
            users.get(7).setRoles(new HashSet<>(Set.of(userRole)));
            users.get(8).setRoles(new HashSet<>(Set.of(adminRole)));
            users.get(9).setRoles(new HashSet<>(Set.of(centerAdminRole)));
            users.get(10).setRoles(new HashSet<>(Set.of(superAdminRole)));

            userRepository.saveAll(users);
            log.info("Seeded {} users", users.size());
        }
    }

    private void ensureDefaultSuperAdmin() {
        Role superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.SUPER_ADMIN).build()));

        User superAdmin = userRepository.findByEmail(defaultAdminEmail.toLowerCase())
                .orElseGet(() -> User.builder()
                        .email(defaultAdminEmail.toLowerCase())
                        .fullName("Super Administrator")
                        .age(35)
                        .phoneNumber("+91-9000006030")
                        .createdAt(LocalDateTime.now())
                        .build());

        superAdmin.setFullName("Super Administrator");
        superAdmin.setPassword(passwordEncoder.encode(defaultAdminPassword));
        superAdmin.setEnabled(true);
        superAdmin.setEmailVerified(true);
        superAdmin.setPhoneVerified(true);
        superAdmin.setIsAdmin(true);
        superAdmin.setIsSuperAdmin(true);
        superAdmin.setRoles(new HashSet<>(Set.of(superAdminRole)));

        userRepository.save(superAdmin);
        log.info("Ensured default super admin exists for {}", superAdmin.getEmail());
    }

    private void seedCenters() {
        if (centerRepository.count() == 0) {
            List<VaccinationCenter> centers = Arrays.asList(
                    VaccinationCenter.builder().name("Apollo Hospital Delhi").address("Sarita Vihar, Delhi").city("Delhi").state("Delhi").pincode("110076").phone("011-26925858").email("delhi@apollohospital.com").dailyCapacity(200).build(),
                    VaccinationCenter.builder().name("Fortis Hospital Mumbai").address("Mulund West, Mumbai").city("Mumbai").state("Maharashtra").pincode("400080").phone("022-49255555").email("mumbai@fortishealthcare.com").dailyCapacity(150).build(),
                    VaccinationCenter.builder().name("Manipal Hospital Bangalore").address("Whitefield, Bangalore").city("Bangalore").state("Karnataka").pincode("560066").phone("080-25024444").email("bangalore@manipalhospitals.com").dailyCapacity(180).build(),
                    VaccinationCenter.builder().name("AIIMS Delhi").address("Ansari Nagar, Delhi").city("Delhi").state("Delhi").pincode("110029").phone("011-26588500").email("info@aiims.edu").dailyCapacity(300).build(),
                    VaccinationCenter.builder().name("Lilavati Hospital Mumbai").address("Bandra West, Mumbai").city("Mumbai").state("Maharashtra").pincode("400050").phone("022-26568000").email("info@lilavatihospital.com").dailyCapacity(120).build(),
                    VaccinationCenter.builder().name("Narayana Health Bangalore").address("Bommasandra, Bangalore").city("Bangalore").state("Karnataka").pincode("560099").phone("080-71222222").email("info@narayanahealth.org").dailyCapacity(250).build()
            );
            centerRepository.saveAll(centers);
            log.info("Seeded {} centers", centers.size());
        }
    }

    private void seedDrives() {
        if (driveRepository.count() == 0) {
            List<VaccinationCenter> centers = new ArrayList<>(centerRepository.findAll());
            List<VaccinationDrive> drives = new ArrayList<>();

            String[] vaccines = {"Covishield", "Covaxin", "Sputnik V"};
            LocalDate today = LocalDate.now();
            LocalDate driveDate1 = today.plusDays(1);
            LocalDate driveDate2 = today.plusDays(3);

            for (int i = 0; i < centers.size(); i++) {
                VaccinationCenter center = centers.get(i);
                drives.add(VaccinationDrive.builder()
                        .center(center)
                        .title("COVID-19 Vaccination Drive - " + vaccines[i % 3])
                        .vaccineType(vaccines[i % 3])
                        .driveDate(driveDate1)
                        .minAge(18)
                        .maxAge(60)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(17, 0))
                        .totalSlots(100)
                        .active(true)
                        .build());
                drives.add(VaccinationDrive.builder()
                        .center(center)
                        .title("Booster Dose Drive - " + vaccines[(i + 1) % 3])
                        .vaccineType(vaccines[(i + 1) % 3])
                        .driveDate(driveDate2)
                        .minAge(18)
                        .maxAge(65)
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(16, 0))
                        .totalSlots(80)
                        .active(true)
                        .build());
            }

            driveRepository.saveAll(drives);
            log.info("Seeded {} drives", drives.size());
        }
    }

    private void seedSlots() {
        if (slotRepository.count() == 0) {
            List<VaccinationDrive> drives = new ArrayList<>(driveRepository.findAll());
            List<Slot> slots = new ArrayList<>();

            for (VaccinationDrive drive : drives) {
                LocalDateTime slotDateTime = drive.getDriveDate().atTime(drive.getStartTime());
                for (int i = 0; i < 3; i++) {
                    slots.add(Slot.builder()
                            .drive(drive)
                            .dateTime(slotDateTime.plusHours(i * 2))
                            .capacity(30)
                            .startTime(slotDateTime.toLocalTime())
                            .endTime(slotDateTime.plusHours(2).toLocalTime())
                            .build());
                }
            }

            slotRepository.saveAll(slots);
            log.info("Seeded {} slots", slots.size());
        }
    }

    private void seedBookings() {
        if (bookingRepository.count() == 0) {
            List<User> users = new ArrayList<>(userRepository.findAll());
            List<Slot> slots = new ArrayList<>(slotRepository.findAll());
            List<Booking> bookings = new ArrayList<>();
            Random random = new Random();

            BookingStatus[] statuses = {BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CANCELLED};

            for (int i = 0; i < 20; i++) {
                User user = users.get(i % users.size());
                Slot slot = slots.get(i % slots.size());
                bookings.add(Booking.builder()
                        .user(user)
                        .slot(slot)
                        .status(statuses[random.nextInt(statuses.length)])
                        .notes("Dose 1 - " + slot.getDrive().getVaccineType())
                        .build());
            }

            bookingRepository.saveAll(bookings);
            log.info("Seeded {} bookings", bookings.size());
        }
    }

    private void seedCertificates() {
        log.info("Skipping certificates seeding to avoid schema error");
    }

    private void seedReviews() {
        if (reviewRepository.count() == 0) {
            List<User> users = new ArrayList<>(userRepository.findAll());
            List<VaccinationCenter> centers = new ArrayList<>(centerRepository.findAll());
            List<Review> reviews = new ArrayList<>();
            Random random = new Random();
            String[] comments = {
                    "Great service, quick vaccination!",
                    "Staff was helpful and friendly.",
                    "Good facilities, recommended.",
                    "Bit crowded but efficient.",
                    "Excellent experience overall."
            };

            for (int i = 0; i < 10; i++) {
                reviews.add(Review.builder()
                        .user(users.get(i % users.size()))
                        .center(centers.get(i % centers.size()))
                        .rating(random.nextInt(3) + 3)
                        .comment(comments[i % comments.length])
                        .build());
            }

            reviewRepository.saveAll(reviews);
            log.info("Seeded {} reviews", reviews.size());
        }
    }

    private void seedNews() {
        if (newsRepository != null && newsRepository.count() == 0) {
            try {
                // Seed sample news if repo exists
                log.info("Seeding news...");
            } catch (Exception e) {
                log.warn("NewsRepository not available, skipping news seeding");
            }
        } else {
            log.info("News seeding skipped");
        }
    }
}
