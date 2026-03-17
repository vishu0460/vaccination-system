# Vaccination System Full Repair TODO
Status: [IN PROGRESS] 

## Plan Breakdown (Approved by user)

### Phase 1: Compilation Fixes (High Priority)
- [x] 1.1 Fix all import errors in controllers/services (domain vs core.model)
- [ ] 1.2 Add missing Lombok annotations (@RequiredArgsConstructor, @Slf4j, @Transactional)
- [ ] 1.3 Implement stubbed methods in services (CenterService, SlotService, etc.)
- [ ] 1.4 `mvn clean compile` → SUCCESS

### Phase 2: Backend Core Fixes
- [ ] 2.1 Complete service implementations (queries, business logic)
- [ ] 2.2 Fix SecurityConfig: permit authenticated paths
- [ ] 2.3 Add missing controllers (ProfileController, NotificationController)
- [ ] 2.4 Enhance GlobalExceptionHandler
- [ ] 2.5 Update DataSeeder (add ADMIN role, test data)
- [ ] 2.6 `mvn clean test` → All tests pass

### Phase 3: Frontend Integration
- [ ] 3.1 Fix api/client.js paths
- [ ] 3.2 Update components/pages for API responses
- [ ] 3.3 `npm ci && npm run build` → SUCCESS

### Phase 4: Database & Data
- [ ] 4.1 Align Flyway migrations with entities
- [ ] 4.2 Seed centers/drives/slots/news data
- [ ] 4.3 Test public endpoints return data

### Phase 5: Security & Prod
- [ ] 5.1 Verify admin login (vaxzone.vaccine@gmail.com / Vaccine@#6030)
- [ ] 5.2 Secure actuator endpoints
- [ ] 5.3 Docker optimizations
- [ ] 5.4 Update README with run instructions

### Phase 6: Verification
- [ ] 6.1 Full docker-compose up → No errors
- [ ] 6.2 Test key flows: register/login/book/ admin dashboard
- [ ] 6.3 No console errors, data visible in UI

**Next Step: 1.2 Add Lombok annotations → Mark complete & run mvn clean compile**

**Commands to run after each phase:**
```
backend: mvn clean compile test
frontend: npm ci && npm run build
full: docker-compose up --build
```


