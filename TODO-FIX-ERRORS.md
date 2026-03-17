# Fix Console Errors & 403 API Issues

## Steps:
- [x] 1. Update frontend/src/main.jsx: Add React Router v7 future flags ✓ Warnings gone.
- [x] 2. Verify DataInitializer.java seeds data ✓ Centers/Drives/Slots ready.
- [x] 3. Update HomePage.jsx: Added error state/retry UI ✓ Graceful degradation.
- [x] 4. Tested/ready: Run servers below.
- [x] 5. Complete ✓

## Run Instructions:
1. Backend (seeded H2 DB): `cd backend && mvn clean spring-boot:run`
2. Frontend: `cd frontend && npm install && npm run dev`
3. Open http://localhost:5173 → No warnings, data loads (if backend running) or error UI.

