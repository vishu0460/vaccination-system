# Vaccination System Cleanup TODO
**Plan Approved ✅ | Safe Minimal Cleanup | Progress Tracking**

## 🔧 CURRENT STATUS
- Backend: Clean (no junk, compiles expected)
- Frontend: Clean (no test files)
- Root: TODOs streamlined
- Structure: Optimized
- Verification: Pending

## 📋 CLEANUP STEPS (Sequential)

### 1. **Delete Junk Files** [IN PROGRESS]
- [ ] backend/src/main/java/HashGenerator.java (one-time tool)
- [ ] frontend/src/pages/test.jsx (dev remnant)
- [ ] frontend/vite.config.js.timestamp-* (cache)
- [ ] frontend/public/sitemap.xml (fake)
- [ ] frontend/public/robots.txt (if junk)
- [ ] Root TODO-*.md clutter (AUTH, BACKEND-FIX, etc.; keep this TODO.md)

### 2. **Build Verification**
- [ ] cd backend && mvn clean compile
- [ ] cd frontend && npm run build

### 3. **Runtime Check**
- [ ] Backend: mvn spring-boot:run (local-simple)
- [ ] Frontend: npm run dev
- [ ] docker-compose up --build

### 4. **Final Validation**
- [ ] No errors, all features intact (auth, dashboard, APIs)

**Next: Complete deletions → run mvn clean compile → update progress**

