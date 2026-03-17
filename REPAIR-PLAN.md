# Vaccination System Backend Repair Plan

## Information Gathered
✅ **pom.xml**: Complete with Spring Boot 3.3.5, Java 17, JPA, Security, Flyway, JWT, Lombok
✅ **Controllers**: 14 @RestController files in `com.vaccine.web.controller.*`
✅ **Services**: 18 @Service files in `com.vaccine.core.service.*`
✅ **Repositories**: 12 @Repository interfaces extending JpaRepository in `com.vaccine.infrastructure.persistence.repository.*`
✅ **Entities**: 16 @Entity classes in `com.vaccine.domain.*`
✅ **Configuration**: SecurityConfig, CacheConfig, DataSeeder exist
✅ **Package structure**: 107 Java files with `com.vaccine.*` packages (inconsistent legacy imports found)
✅ **Flyway migrations**: V1-V12 complete
✅ **DTOs**: Most exist in `com.vaccine.common.dto.*`, 4 missing DTOs created

**Current Status**: `mvn clean compile` shows 1 critical syntax error in AuthService.java (missing closing brace)

## Plan
### Phase 1: Fix Critical Syntax (Immediate)
1. **AuthService.java**: Add missing `}` to close class
2. **CertificateService.java**: Fix import from `core.model.*` to `domain.*`
3. **DriveService.java**: Remove custom repository methods, use standard JpaRepository.findAll()

### Phase 2: Package Import Cleanup (5 min)
Replace legacy imports:
- `com.vaccine.dto.*` → `com.vaccine.common.dto.*`
- `com.vaccine.entity.*` → `com.vaccine.domain.*`
- `com.vaccine.repository.*` → `com.vaccine.infrastructure.persistence.repository.*`

### Phase 3: Entity/Repository Alignment (10 min)
Add missing @Query methods to repositories:
- CertificateRepository: `existsByBookingId`, `findByBookingId`, `findByCertificateNumber`, `findByBookingUserIdOrderByIssuedAtDesc`
- BookingRepository: `countByStatus`
- VaccinationDriveRepository: `findByActiveTrue`
- SlotRepository: `findByDriveIdOrderByStartTimeAsc`, `countAvailableSlots`

### Phase 4: DTO Constructor Fixes (5 min)
Update test constructors to match record definitions.

### Phase 5: Missing Methods (10 min)
- VaccinationDrive: add `title()` to builder
- DriveResponse: add static `from(VaccinationDrive)` factory method
- AdminDashboardStatsResponse: verify all getter methods exist

### Dependent Files to Edit
**Priority 1 (Syntax)**:
- `backend/src/main/java/com/vaccine/core/service/AuthService.java`
- `backend/src/main/java/com/vaccine/core/service/CertificateService.java`

**Priority 2 (Imports)**:
- All test files (`src/test/java/com/vaccine/service/*Test.java`)
- `backend/src/main/java/com/vaccine/util/CsvExportUtil.java`
- `backend/src/main/java/com/vaccine/core/service/DriveService.java`

**Priority 3 (Repository Methods)**:
- `backend/src/main/java/com/vaccine/infrastructure/persistence/repository/CertificateRepository.java`
- `backend/src/main/java/com/vaccine/infrastructure/persistence/repository/BookingRepository.java`
- `backend/src/main/java/com/vaccine/infrastructure/persistence/repository/VaccinationDriveRepository.java`
- `backend/src/main/java/com/vaccine/infrastructure/persistence/repository/SlotRepository.java`

## Followup Steps
1. `cd backend && mvn clean compile` after each phase
2. `cd backend && mvn spring-boot:run` after full compilation
3. Test key endpoints: `/auth/register`, `/auth/login`, `/health`
4. Verify database migrations run
5. Check Swagger UI at `/swagger-ui.html`

**Ready to proceed? Confirm and I'll start with Phase 1 (AuthService syntax fix).**
