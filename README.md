# VaxZone Vaccination Management System

## 🚀 Quick Start (Local Development)

### Prerequisites
- Java 17+
- Node.js 18+
- Docker Desktop (optional for full stack)

### 1. Backend + Database (Terminal 1)
```bash
cd backend
run-local.cmd
```
- Runs on http://localhost:8080
- H2 in-memory DB with seed data (5 centers, 8 drives, slots, news)
- Swagger: http://localhost:8080/swagger-ui.html

Alternative:
```bash
cd backend
powershell -ExecutionPolicy Bypass -File .\run-local.ps1
```

### 2. Frontend (Terminal 2)
```bash
cd frontend
npm install
npm run dev
```
- Runs on http://localhost:5173
- Auto-connects to backend API

### Super Admin Login
```
Email: vaxzone.vaccine@gmail.com
Password: 
```

## 🔍 Verification Checklist
- [x] Home shows **5 Centers, 8 Drives**, live stats
- [x] **News Page**: 5 articles visible
- [x] **Admin Login** works → Dashboard accessible
- [x] **Vite Proxy**: /api → localhost:8080 (no CORS issues)
- [x] **H2 Seeding**: data-complete.sql loads automatically

## 🐳 Docker (Production)
```bash
docker-compose up --build
```
Frontend: http://localhost:80 | Backend: http://localhost:8080

## 🗄️ MySQL Production Setup
1. Create DB: `vaccination_db`
2. User: `vaxzone_user` (all privileges)
3. Copy `.env.example` → `.env`, update DB creds
4. `SPRING_PROFILES_ACTIVE=prod`
5. Flyway auto-migrates V1 schema

## 📋 Features
- User registration/login/2FA
- Drive & slot booking
- Admin dashboard & management
- Certificate generation (QR/PDF)
- News & notifications
- Reviews & feedback

## 🛠 Troubleshooting
**Backend not starting?** Check Maven logs for compilation errors  
**Port 8080 already in use?** Run `backend\run-local.cmd` to stop the previous listener and restart safely  
**Frontend blank?** Check browser console for CORS/API errors  
**No data?** Data auto-seeds on H2 startup - refresh Home page

## 🔧 Environment
Backend: Spring Boot 3.3.8 + JPA/H2  
Frontend: React 18 + Vite + Bootstrap 5
