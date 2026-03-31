# 🗂️ Project Structure

Detailed breakdown of VaxZone monorepo organization.

## 📁 Root Level

```
e:/vaccination-system/
├── README.md                 # Main project overview
├── TODO.md                   # Priorities & blockers
├── docker-compose.yml        # Local/prod stack (MySQL/Backend/Frontend/Nginx)
├── .env.example              # Env vars template (JWT/DB secrets)
├── .gitignore                # Standard ignores
├── nginx/                    # Prod reverse proxy configs
├── scripts/                  # Backup/migrate utils (mysql-backup.sh, migrate-db.ps1)
├── Essentials/               # Project notes (tech stack, workflow)
└── Logs/                     # Runtime logs
```

## 🔧 Backend (Spring Boot)

**Path**: `backend/`  
**Main**: `VaccinationApplication.java` (port 8080)

```
backend/
├── pom.xml                       # Maven deps (Spring Boot 3.3.5, JPA/Security/etc.)
├── src/main/java/com/vaccine/
│   ├── VaccinationApplication.java  # Entry point (@EnableScheduling)
│   ├── web/controller/             # REST APIs (~15 controllers)
│   │   ├── AdminController.java
│   │   ├── UserController.java
│   │   ├── CertificateController.java
│   │   └── SuperAdminController.java
│   ├── core/service/               # Business logic
│   ├── infrastructure/persistence/repository/ # JPA repos
│   ├── domain/                     # Entities (@Entity: User, Booking, Slot, Drive, Center)
│   ├── security/                   # JWT filters/utils
│   ├── config/                     # App/Cache/CORS/RateLimit/SecurityConfig
│   └── common/dto/                 # Request/Response DTOs
├── src/main/resources/
│   ├── application.yml             # Env import, ports/profiles (local/H2 vs prod/MySQL)
│   ├── db/migration/               # 27 Flyway SQL scripts (V1__Initial..V27__)
│   └── data.sql                    # Seed data (dev profile)
└── target/                         # Maven build output
```

**Key Packages**:
- `web/controller`: @RestController endpoints (auth/user/admin)
- `core`: Services (BookingService, NotificationService)
- `exception`: GlobalExceptionHandler

## ⚛️ Frontend (React/Vite)

**Path**: `frontend/`  
**Ports**: 5174 (dev), dist/ for prod.

```
frontend/
├── package.json                    # React 18, Vite 5, Bootstrap/Charts etc.
├── vite.config.js                  # Build config
├── src/
│   ├── App.jsx                     # Root + AppRoutes
│   ├── main.jsx                    # Router/Helmet/PWA
│   ├── pages/                      # ~25 pages
│   │   ├── HomePage.jsx
│   │   ├── AdminDashboardPage.jsx
│   │   ├── CentersPage.jsx
│   │   └── UserBookingsPage.jsx
│   ├── components/                 # UI (~30: Navbar, Modal, Charts, Search)
│   │   ├── admin/
│   │   └── auth/
│   ├── api/client.js               # Axios public/auth API client
│   ├── hooks/                      # useDebounce, useCurrentTime
│   ├── utils/                      # auth.js, realtimeStatus.js, notificationSocket.js
│   └── context/                    # ThemeContext, PublicCatalogContext
├── public/                         # PWA manifest, sitemap.xml, sw.js
├── tests/                          # Playwright E2E
└── dist/                           # npm run build output
```

**Key Folders**:
- `pages/`: Route components (ProtectedRoute roles)
- `components/admin/`: AdminManagement, LogsTable etc.
- `utils/`: Realtime WebSocket, dataSync

## 🐳 Deployment & Ops

```
docker-compose.yml          # Services: mysql(3306), backend(8080), frontend(80), nginx
nginx/default.conf          # HTTPS/CORS/cache
scripts/mysql-backup.sh     # Automated DB dumps
migrate-db.ps1/.sh          # H2 → MySQL migration
```

## 🔄 Dependencies Flow

```
Frontend (Axios) → Backend API (/api/v1/*)
                 ↓
Spring Controllers → Services → JPA Repos → MySQL/H2
                 ↗
WebSocket (STOMP) ← Realtime updates (slots/notifications)
```

Generated from actual repo scan. Questions? [CONTRIBUTING.md](./CONTRIBUTING.md)

