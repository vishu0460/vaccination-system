package com.vaccine.config;

import com.vaccine.entity.*;
import com.vaccine.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private VaccinationCenterRepository centerRepository;
    
    @Autowired
    private VaccinationDriveRepository driveRepository;
    
    @Autowired
    private SlotRepository slotRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // Create roles if they don't exist
        createRoleIfNotExists("SUPER_ADMIN", "Super Administrator with full access");
        createRoleIfNotExists("ADMIN", "Administrator with limited access");
        createRoleIfNotExists("USER", "Regular user");
        
        // Create admin user if not exists
        if (!userRepository.existsByEmail("admin@vaccine.com")) {
            User admin = new User();
            admin.setEmail("admin@vaccine.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setPhone("9876543210");
            admin.setAadharNumber("123456789012");
            admin.setDateOfBirth(LocalDate.of(1980, 1, 1));
            admin.setGender(User.Gender.MALE);
            admin.setIsActive(true);
            admin.setIsVerified(true);
            
            Set<Role> adminRoles = new HashSet<>();
            roleRepository.findByName("ADMIN").ifPresent(adminRoles::add);
            admin.setRoles(adminRoles);
            
            userRepository.save(admin);
            System.out.println("Admin user created: admin@vaccine.com / admin123");
        }
        
        // Create regular user if not exists
        if (!userRepository.existsByEmail("user@vaccine.com")) {
            User user = new User();
            user.setEmail("user@vaccine.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setPhone("9876543211");
            user.setAadharNumber("123456789013");
            user.setDateOfBirth(LocalDate.of(1990, 5, 15));
            user.setGender(User.Gender.MALE);
            user.setIsActive(true);
            user.setIsVerified(true);
            
            Set<Role> userRoles = new HashSet<>();
            roleRepository.findByName("USER").ifPresent(userRoles::add);
            user.setRoles(userRoles);
            
            userRepository.save(user);
            System.out.println("Regular user created: user@vaccine.com / user123");
        }
        
        // Initialize vaccination centers if not exist
        if (centerRepository.count() == 0) {
            initializeCentersAndDrives();
        } else if (slotRepository.count() == 0) {
            // Centers exist but slots don't - create slots for existing drives
            initializeSlotsForExistingDrives();
        }
    }
    
    private void initializeSlotsForExistingDrives() {
        List<VaccinationDrive> drives = driveRepository.findAll();
        for (VaccinationDrive drive : drives) {
            createSlotsForDrive(drive);
        }
        System.out.println("Created slots for " + drives.size() + " existing drives");
    }
    
    private void createSlotsForDrive(VaccinationDrive drive) {
        LocalDate startDate = drive.getStartDate();
        LocalDate endDate = drive.getEndDate();
        LocalTime startTime = drive.getStartTime();
        LocalTime endTime = drive.getEndTime();
        
        int slotsPerSession = 20;
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            int dayOfWeek = currentDate.getDayOfWeek().getValue();
            if (dayOfWeek < 6) {
                LocalTime currentTime = startTime;
                while (currentTime.isBefore(endTime)) {
                    Slot slot = new Slot();
                    slot.setDrive(drive);
                    slot.setSlotDate(currentDate);
                    slot.setSlotTime(currentTime);
                    slot.setTotalCapacity(slotsPerSession);
                    slot.setAvailableCapacity(slotsPerSession);
                    slotRepository.save(slot);
                    
                    currentTime = currentTime.plusHours(1);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        
        System.out.println("Created slots for drive: " + drive.getName());
    }
    
    private void initializeCentersAndDrives() {
        // Create vaccination centers
        VaccinationCenter center1 = new VaccinationCenter();
        center1.setName("City General Hospital");
        center1.setAddress("123 Main Road, Civil Lines");
        center1.setCity("Delhi");
        center1.setState("Delhi");
        center1.setPincode("110001");
        center1.setLatitude(28.6139);
        center1.setLongitude(77.2090);
        center1.setPhone("011-12345678");
        center1.setEmail("cgh@vaccinesystem.com");
        center1.setCapacityPerDay(500);
        center1.setIsActive(true);
        center1 = centerRepository.save(center1);
        
        VaccinationCenter center2 = new VaccinationCenter();
        center2.setName("Metro Medical Center");
        center2.setAddress("456 Metro Station Road");
        center2.setCity("Mumbai");
        center2.setState("Maharashtra");
        center2.setPincode("400001");
        center2.setLatitude(19.0760);
        center2.setLongitude(72.8777);
        center2.setPhone("022-23456789");
        center2.setEmail("mmc@vaccinesystem.com");
        center2.setCapacityPerDay(400);
        center2.setIsActive(true);
        center2 = centerRepository.save(center2);
        
        VaccinationCenter center3 = new VaccinationCenter();
        center3.setName("Tech City Clinic");
        center3.setAddress("789 Tech Park, Electronic City");
        center3.setCity("Bangalore");
        center3.setState("Karnataka");
        center3.setPincode("560100");
        center3.setLatitude(12.9180);
        center3.setLongitude(77.6640);
        center3.setPhone("080-34567890");
        center3.setEmail("tcc@vaccinesystem.com");
        center3.setCapacityPerDay(300);
        center3.setIsActive(true);
        center3 = centerRepository.save(center3);
        
        VaccinationCenter center4 = new VaccinationCenter();
        center4.setName("Green Valley Hospital");
        center4.setAddress("321 Green Avenue");
        center4.setCity("Chennai");
        center4.setState("Tamil Nadu");
        center4.setPincode("600001");
        center4.setLatitude(13.0827);
        center4.setLongitude(80.2707);
        center4.setPhone("044-45678901");
        center4.setEmail("gvh@vaccinesystem.com");
        center4.setCapacityPerDay(350);
        center4.setIsActive(true);
        center4 = centerRepository.save(center4);
        
        VaccinationCenter center5 = new VaccinationCenter();
        center5.setName("Lake View Center");
        center5.setAddress("654 Lake Shore Road");
        center5.setCity("Hyderabad");
        center5.setState("Telangana");
        center5.setPincode("500001");
        center5.setLatitude(17.3850);
        center5.setLongitude(78.4867);
        center5.setPhone("040-56789012");
        center5.setEmail("lvc@vaccinesystem.com");
        center5.setCapacityPerDay(450);
        center5.setIsActive(true);
        center5 = centerRepository.save(center5);
        
        System.out.println("Created 5 vaccination centers");
        
        // Create vaccination drives
        createDrive(center1, "COVID-19 Vaccination Drive 2026", "Free COVID-19 vaccination for all eligible citizens",
                "Covishield", "Oxford/AstraZeneca", 18, 60, 2, 84,
                LocalDate.now(), LocalDate.now().plusMonths(10),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 5000);
        
        createDrive(center2, "Flu Vaccination Campaign", "Annual flu vaccination campaign",
                "Influvac", "Abbott", 5, 100, 1, 0,
                LocalDate.now(), LocalDate.now().plusMonths(10),
                LocalTime.of(8, 0), LocalTime.of(16, 0), 4000);
        
        createDrive(center3, "Pediatric Immunization Program", "Routine immunization for children",
                "Pentavac", "Serum Institute", 0, 12, 3, 30,
                LocalDate.now(), LocalDate.now().plusMonths(10),
                LocalTime.of(9, 0), LocalTime.of(15, 0), 3000);
        
        createDrive(center4, "Hepatitis B Adult Drive", "Hepatitis B vaccination for adults",
                "Hepavax", "Johnson and Johnson", 18, 100, 3, 30,
                LocalDate.now(), LocalDate.now().plusMonths(10),
                LocalTime.of(10, 0), LocalTime.of(18, 0), 3500);
        
        createDrive(center5, "Typhoid Awareness Drive", "Typhoid vaccination awareness",
                "Typbar-TCV", "Bharat Biotech", 2, 100, 1, 0,
                LocalDate.now(), LocalDate.now().plusMonths(10),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 4500);
        
        System.out.println("Created 5 vaccination drives");
    }
    
    private void createDrive(VaccinationCenter center, String name, String description,
            String vaccineName, String manufacturer, int minAge, int maxAge,
            int dosesRequired, int doseGapDays, LocalDate startDate, LocalDate endDate,
            LocalTime startTime, LocalTime endTime, int totalSlots) {
        
        VaccinationDrive drive = new VaccinationDrive();
        drive.setName(name);
        drive.setDescription(description);
        drive.setVaccineName(vaccineName);
        drive.setVaccineManufacturer(manufacturer);
        drive.setMinAge(minAge);
        drive.setMaxAge(maxAge);
        drive.setDosesRequired(dosesRequired);
        drive.setDoseGapDays(doseGapDays);
        drive.setCenter(center);
        drive.setStartDate(startDate);
        drive.setEndDate(endDate);
        drive.setStartTime(startTime);
        drive.setEndTime(endTime);
        drive.setTotalSlots(totalSlots);
        drive.setAvailableSlots(totalSlots);
        drive.setIsActive(true);
        
        drive = driveRepository.save(drive);
        
        // Create slots for the drive
        int slotsPerSession = 20; // 20 people per hour slot
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // Skip weekends (Saturday = 6, Sunday = 7)
            int dayOfWeek = currentDate.getDayOfWeek().getValue();
            if (dayOfWeek < 6) {
                LocalTime currentTime = startTime;
                while (currentTime.isBefore(endTime)) {
                    Slot slot = new Slot();
                    slot.setDrive(drive);
                    slot.setSlotDate(currentDate);
                    slot.setSlotTime(currentTime);
                    slot.setTotalCapacity(slotsPerSession);
                    slot.setAvailableCapacity(slotsPerSession);
                    slotRepository.save(slot);
                    
                    currentTime = currentTime.plusHours(1);
                }
            }
            currentDate = currentDate.plusDays(1);
        }
        
        System.out.println("Created drive: " + name + " with slots");
    }
    
    private void createRoleIfNotExists(String name, String description) {
        Optional<Role> existingRole = roleRepository.findByName(name);
        if (existingRole.isEmpty()) {
            Role role = new Role(name, description);
            roleRepository.save(role);
            System.out.println("Role created: " + name);
        }
    }
}
