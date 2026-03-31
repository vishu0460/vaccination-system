# 🔒 Security

Production-grade security model.

## 🛡️ Authentication

**JWT (JJWT 0.12.6)**:
- **Access Token**: 24h (configurable JWT_ACCESS_MINUTES)
- **Refresh Token**: 7 days (JWT_REFRESH_DAYS)
- **Claims**: sub (userId), role (USER/ADMIN/SUPER_ADMIN), iat/exp
- **Issuer**: vaccination-system (JWT_ISSUER)
- **Secret**: Env-only (JWT_SECRET ≥64 chars, rotate regularly)

Flow:
1. POST /api/auth/login → access + refresh
2. POST /api/auth/refresh → new access
3. Bearer token in `Authorization` header

## 🧑‍💼 Role-Based Access Control (RBAC)

| Role | Permissions (@PreAuthorize) |
|------|-----------------------------|
| USER | Bookings, profile, certs, reviews |
| ADMIN | Dashboard, manage centers/bookings/logs |
| SUPER_ADMIN | All + global users/admins |

## 🛑 Rate Limiting

- **Filter**: RateLimitFilter (60 req/min per IP, in-memory)
- Targets: Auth, OTP, public search, contacts
- Env: rate-limit.requests-per-minute

## 🔐 Sensitive Config (Env Only)

Never commit:
```
JWT_SECRET=your-base64-secret
DB_PASSWORD=...
MAIL_PASSWORD=...
TWILIO_AUTH_TOKEN=...
```

Rotate: `scripts/rotate-secrets.ps1`

## 🌐 Deployment Security

**Nginx** (nginx/default.conf):
- HTTPS required (`/certs/` mount)
- HSTS, CSP headers
- CORS allowlist (CORS_ALLOWED_ORIGINS)

**Profiles**:
- local: H2 console enabled (dev only)
- prod: Actuators/Swagger restricted

## 🧪 Best Practices

- **HTTPS Everywhere**: Prod reverse proxy
- **Secrets**: Vault/K8s Secrets (no .env)
- **Auditing**: All actions logged (audit_logs table)
- **Input Validation**: @Valid DTOs
- **Brute Force**: OTP lockout (5 attempts/15min)
- **2FA**: OTP support (TOTP ready)

**Vulns Mitigated**: From [TODO.md](./TODO.md)

Scan: `mvn spring-boot:run` + OWASP ZAP.

