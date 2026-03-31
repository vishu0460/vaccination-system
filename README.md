# VaxZone 🩺 Vaccination Management System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org)
[![Tests](https://img.shields.io/badge/Tests-Passing-brightgreen.svg)](https://github.com/example/vaxzone/actions)

**VaxZone** is a production-ready, full-stack vaccination booking platform for citizens, admins, and super admins. Built with Spring Boot backend + React/Vite frontend, it handles slot booking, certificates, real-time notifications, analytics, and role-based management.

![VaxZone Dashboard](docs/screenshots/dashboard.png) *(Add real screenshots)*

## 🚀 Key Features

| Role | Capabilities |
|------|--------------|
| **Citizens/Users** | Browse/search centers & drives, book/cancel/reschedule slots, waitlists, profile/certificates/notifications/recommendations/reviews/feedback/contacts |
| **Admins** | Dashboard charts/stats/analytics/logs, CRUD centers/drives/slots/bookings/users/news/feedback/contacts/admins |
| **Super Admins** | All admin + global users/centers/drives/slots management |
| **Public** | Verify certificates, legal pages, SEO/PWA optimized |

- **Security**: JWT auth/refresh, role-based access (USER/ADMIN/SUPER_ADMIN), rate limiting
- **Realtime**: WebSocket notifications (STOMP/SockJS)
- **Integrations**: Email/SMS (Twilio), PDF/QR certificates
- **DevOps**: Docker Compose, Flyway migrations (MySQL/H2), OpenAPI/Swagger

## 🛠 Tech Stack

**Backend**: Spring Boot 3.3.5, Spring Security/Data JPA/WebSocket/Mail/Cache, Flyway, JJWT, ZXing (QR), OpenPDF, Twilio SMS, MySQL/H2  
**Frontend**: React 18, Vite 5, React Router, Bootstrap 5, Chart.js/Recharts, React Helmet (SEO), PWA/Service Worker  
**Tools**: Maven, npm/Vitest/Playwright, Docker, Nginx reverse proxy

## 📁 Quick Structure

```
e:/vaccination-system/
├── backend/          # Spring Boot API (Java 17)
│   ├── src/main/java/com/vaccine/web/controller/  # REST controllers
│   ├── src/main/resources/db/migration/           # 27 Flyway scripts
│   └── pom.xml
├── frontend/         # React/Vite SPA
│   ├── src/pages/    # Home/Drives/Centers/AdminDashboard etc.
│   ├── src/api/      # Axios client
│   └── package.json
├── docker-compose.yml # MySQL + Backend + Frontend + Nginx
├── README.md         # You're here!
└── docs/             # Full structure/API/Setup etc.
```

Detailed: [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md)

## ⚡ Quickstart (Local Dev)

### Prerequisites
- Java 17+, Node 18+, Docker (optional for MySQL)
- Copy `.env.example` → `.env` & set secrets (JWT_SECRET, DB creds)

### Backend (http://localhost:8080)
```bash
cd backend
mvn spring-boot:run  # H2 local profile (SPRING_PROFILES_ACTIVE=local)
# Or MySQL: docker compose up -d mysql && SPRING_PROFILES_ACTIVE=local-fixed mvn spring-boot:run
```
Swagger: http://localhost:8080/swagger-ui/index.html

### Frontend (http://localhost:5174)
```bash
cd frontend
npm install
npm run dev
```

**Full Setup**: [SETUP_GUIDE.md](./SETUP_GUIDE.md)

## 🌐 API Overview

See [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) for full spec.

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | /api/auth/login | Public | JWT login |
| GET | /api/public/centers | Public | Search centers |
| POST | /api/user/bookings | USER | Create booking |
| GET | /api/admin/dashboard/stats | ADMIN | Dashboard analytics |
| POST | /api/certificates | USER/ADMIN | Generate certificate |

## 🚀 Deployment

- **Docker**: `docker compose up -d --build`
- **Backend**: Render/Heroku (`mvn package && java -jar app.jar`)
- **Frontend**: Vercel/Netlify (`npm run build`)
- **Prod Checklist**: HTTPS, env secrets, Flyway migrations

## 📖 Documentation

- [API Docs](./API_DOCUMENTATION.md) | [Setup](./SETUP_GUIDE.md) | [Features](./FEATURES.md)
- [Security](./SECURITY.md) | [Architecture](./ARCHITECTURE.md)
- [Changelog](./CHANGELOG.md) | [Contributing](./CONTRIBUTING.md)

## 🛠 Help Wanted

See [TODO.md](./TODO.md) for priorities. Contributions welcome!

## 📄 License & Author

MIT License. Built by [Your Name/Team]. Questions? Open an issue.

---

⭐ **Star on GitHub if useful!**  
![Footer](docs/screenshots/footer.png)

