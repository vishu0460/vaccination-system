# VaxZone Vaccination System

VaxZone is a full-stack vaccination booking platform built with Spring Boot, React, and MySQL. It supports citizen registration, slot booking, certificate verification, admin operations, notifications, and deployment-ready configuration for modern hosting platforms.

## Overview

- Citizens can browse centers and drives, book slots, manage bookings, and verify certificates.
- Admins can manage centers, drives, slots, bookings, feedback, contacts, and analytics.
- The platform includes JWT security, rate limiting, responsive UI, SEO metadata, and PWA support.

## Features

- JWT-based authentication with refresh flow
- User registration, login, profile management, and booking history
- Vaccination drive and slot discovery with filtering
- Booking, cancellation, rescheduling, and certificate generation
- Admin dashboard with charts, booking insights, and management tools
- Personalized in-app/email/SMS-style notification messages
- Feedback, contact, news, and review workflows
- React Helmet SEO metadata, sitemap, robots rules, and Open Graph tags
- PWA manifest and service worker registration

## Tech Stack

- Backend: Spring Boot 3, Spring Security, Spring Data JPA, Flyway
- Frontend: React 18, Vite, React Router, React Bootstrap, Chart.js
- Database: MySQL
- Dev/Test: H2, JUnit, Mockito, Vitest, Playwright

## Project Structure

### Backend

- `backend/src/main/java/com/vaccine/web/controller` API endpoints
- `backend/src/main/java/com/vaccine/core/service` business logic
- `backend/src/main/java/com/vaccine/infrastructure/persistence/repository` persistence layer
- `backend/src/main/java/com/vaccine/common/dto` request and response DTOs
- `backend/src/main/java/com/vaccine/domain` entities and enums
- `backend/src/main/java/com/vaccine/config` app, cache, CORS, and rate-limit config
- `backend/src/main/java/com/vaccine/security` JWT and auth filters
- `backend/src/main/java/com/vaccine/util` certificate and export utilities

### Frontend

- `frontend/src/components` shared UI pieces
- `frontend/src/pages` route-level pages
- `frontend/src/api` API client layer
- `frontend/src/hooks` reusable hooks
- `frontend/src/utils` auth and helpers
- `frontend/src/styles` global styling

## Local Setup

### Prerequisites

- Java 17+
- Node.js 18+
- MySQL 8+ for production profile
- Docker Desktop optional

### Backend

Backend startup now requires environment variables for database access and JWT signing. No credentials are embedded in source-controlled config.

```bash
cp .env.example .env
cd backend
mvn spring-boot:run
```

Backend default URL: `http://localhost:8080`

Use `SPRING_PROFILES_ACTIVE=local` for local H2-backed development, `SPRING_PROFILES_ACTIVE=local-fixed` for MySQL-backed local development, and `SPRING_PROFILES_ACTIVE=dev` only when you explicitly want development-only sample data.

For MySQL-backed local development:

```bash
docker compose up -d mysql
SPRING_PROFILES_ACTIVE=local-fixed mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Frontend

```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```

Frontend default URL: `http://localhost:5174`

## Environment Variables

Copy `.env.example` to `.env` and set the values you need.

### Backend

- `SPRING_PROFILES_ACTIVE`
- `DB_URL`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ISSUER`
- `JWT_ACCESS_MINUTES`
- `JWT_REFRESH_DAYS`
- `CORS_ALLOWED_ORIGINS`
- `APP_BASE_URL`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `SMS_ENABLED`
- `TWILIO_ACCOUNT_SID`
- `TWILIO_AUTH_TOKEN`
- `TWILIO_PHONE_NUMBER`

### Frontend

- `VITE_API_BASE_URL`
- `VITE_API_URL`
- `VITE_APP_ENV`
- `VITE_API_PORT_CANDIDATES`
- `E2E_USER_EMAIL`
- `E2E_USER_PASSWORD`
- `E2E_ADMIN_EMAIL`
- `E2E_ADMIN_PASSWORD`

## Database

- Runtime database connection details come from environment variables only.
- Flyway migrations live in `backend/src/main/resources/db/migration`.
- The `dev` and `test` profiles are intentionally non-persistent and should not be used for data you need to keep.
- Slot-notification subscriptions, notifications, search logs, bookings, users, and drives are stored in the database and survive application restarts when running with MySQL.

### Backup

- A repeatable MySQL backup script is available at `scripts/mysql-backup.sh`.
- Example: `DB_PASSWORD=your-runtime-password ./scripts/mysql-backup.sh`
- The script creates compressed dumps in `./backups/mysql` and prunes backups older than `BACKUP_RETENTION_DAYS` (default `14`).

## Deployment

### Production Checklist

- Set strong values for `JWT_SECRET`, `DB_ROOT_PASSWORD`, `DB_USERNAME`, and `DB_PASSWORD`
- Keep `APP_SEED_ENABLED=false` in production. Development sample seeding runs only with the `dev` profile.
- Set `CORS_ALLOWED_ORIGINS`, `APP_BASE_URL`, and `FRONTEND_URL` to your real HTTPS domains
- Run Flyway migrations before exposing traffic
- Replace MailHog with a real SMTP provider in production
- Provision or rotate administrator access outside source control before going live
- Use `scripts/rotate-secrets.ps1` locally to generate a fresh JWT secret and BCrypt password hash during incident response or credential rotation
- Enable HTTPS at the reverse proxy and mount valid certificates in `nginx/certs`

### Backend on Render

- Build command: `cd backend && mvn clean package`
- Start command: `java -jar backend/target/app.jar`
- Set database, JWT, mail, and CORS environment variables in Render
- Point `APP_BASE_URL` to the frontend production URL

### Docker Compose Production

```bash
docker compose up -d --build
```

- The compose stack now fails fast when required database or JWT environment variables are missing
- MySQL is only exposed to the internal Docker network by default
- MailHog is available only through the optional `dev` profile:

```bash
docker compose --profile dev up -d
```

### Frontend on Vercel

- Root directory: `frontend`
- Build command: `npm run build`
- Output directory: `dist`
- Set `VITE_API_BASE_URL` to your deployed backend URL, for example `https://your-api.onrender.com/api`

## API Documentation

- Swagger/OpenAPI UI: `/swagger-ui/index.html`
- Health endpoint: `/api/v1/health`
- Public endpoints: `/api/public/*`
- Auth endpoints: `/api/auth/*`
- User endpoints: `/api/user/*`, `/api/profile`
- Admin endpoints: `/api/admin/*`

## Testing

### Backend

```bash
cd backend
mvn test
```

### Frontend

```bash
cd frontend
npm test -- --run
npm run build
```

Authenticated Playwright scenarios use environment-provided credentials only. Set `E2E_USER_EMAIL`, `E2E_USER_PASSWORD`, `E2E_ADMIN_EMAIL`, and `E2E_ADMIN_PASSWORD` in your local `.env` or CI secret store before running them.

## Verified Checks

- Login and register flows pass
- Booking and cancellation flows pass
- Admin dashboard loads with stats and charts
- Backend tests pass
- Frontend tests pass
- Frontend production build passes
- PWA manifest and service worker are wired

## Screenshots

- `docs/screenshots/home.png`
- `docs/screenshots/drives.png`
- `docs/screenshots/bookings.png`
- `docs/screenshots/admin-dashboard.png`
- `docs/screenshots/certificate.png`

## Notes

- Replace placeholder screenshot files with real captures before sharing publicly.
- Update `frontend/public/sitemap.xml` and `frontend/public/robots.txt` with your final production domain.
- Review `.env` values before deployment so secrets are never committed.
- Do not commit `.env`, `application-local*.yml`, or ad hoc secret files.
