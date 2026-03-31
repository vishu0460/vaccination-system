# 🏗️ Setup Guide

Step-by-step for local dev/prod.

## 📋 Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java | 17+ | [OpenJDK](https://adoptium.net) |
| Node.js | 18+ | [nodejs.org](https://nodejs.org) |
| Maven | 3.9+ | Bundled with Spring Boot |
| Docker | Latest | [docker.com](https://docker.com) (optional MySQL) |
| Git | Latest | [git-scm.com](https://git-scm.com) |

## 🔑 Environment (.env)

Copy `.env.example` → `.env` (root/backend/frontend):

**Backend**:
| Var | Example | Required |
|-----|---------|----------|
| DB_URL | `jdbc:mysql://localhost:3306/vaxdb` | MySQL |
| DB_USERNAME | `root` | DB |
| DB_PASSWORD | `pass` | DB |
| JWT_SECRET | `your-64char-secret` | Always |
| MAIL_USERNAME | `noreply@vaxzone.com` | Email |
| TWILIO_ACCOUNT_SID | `ACxxx` | SMS (optional) |

**Frontend**:
| Var | Example |
|-----|---------|
| VITE_API_BASE_URL | `http://localhost:8080/api` |

## 🚀 Backend Setup

1. **H2 Local (No DB needed)**:
```bash
cd backend
mvn spring-boot:run  # SPRING_PROFILES_ACTIVE=local
```
Port: **8080**. Swagger: http://localhost:8080/swagger-ui/index.html

2. **MySQL Local**:
```bash
docker compose up -d mysql
cd backend
SPRING_PROFILES_ACTIVE=local-fixed mvn spring-boot:run
```

3. **Prod Profile**:
```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

**Scripts**:
- `run-local.ps1/cmd`: Windows launch
- `migrate-db.ps1/sh`: H2 → MySQL

## ⚛️ Frontend Setup

```bash
cd frontend
npm install
npm run dev
```
Port: **5174**.

**Build**:
```bash
npm run build  # Output: dist/
npm run preview
```

## 🐳 Docker Full Stack

```bash
# Dev (MailHog)
docker compose --profile dev up -d

# Prod
docker compose up -d --build
```
Ports: Nginx 80/443, Backend 8080, Frontend N/A.

## 🔍 Verify Setup

- Backend health: `curl http://localhost:8080/api/v1/health`
- Swagger loads
- Frontend loads, login/register works
- Bookings flow end-to-end

## 🐛 Troubleshooting

| Issue | Fix |
|-------|-----|
| DB connection | Check DB_URL, docker logs mysql |
| CORS | Add localhost:5174 to CORS_ALLOWED_ORIGINS |
| JWT invalid | Regenerate JWT_SECRET (64+ chars) |
| Port conflict | Kill java/node processes |
| Tests fail | `mvn clean test`, `npm test` |
| Migrations | Flyway auto-runs on startup |

**Backup**: `./scripts/mysql-backup.sh`

Prod: Render (backend), Vercel (frontend). See README.

Full API: [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)

