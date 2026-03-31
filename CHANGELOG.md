# 📜 Changelog

Version history based on DB migrations & features.

## [1.2.0] - Unreleased (Current)
- Refine docs (README, API, Setup, etc.)
- Test fixes (backend IntegrationTest)

## [1.1.0] - 2024-XX-XX
- **Drives & Subscriptions** (V18__drive_subscriptions)
- **Reliable Notifications** (V17__)
- **Search Logs** (V16__)
- **Soft Delete/Audit** (V19__)

## [1.0.0] - Initial Release
### Core Schema (V1-V10)
- Users, Bookings, Slots, Centers, Drives
- Contacts, Phone Verification

### Security/UX (V11-V15)
- Rate Limits (V12)
- Booking/Comm Status Normalize (V13-V14)
- Drive Status (V15)

### Advanced (V16-V27)
- Waitlists (V22), DOB/Profile Image (V25-V27)
- Admin Scopes/Roles (V21)
- OTP Hardening (V24)

## [0.1.0] - MVP
- Basic auth/booking/certs
- H2 local dev

**Migration Scripts**: backend/src/main/resources/db/migration/ (27 total)

Full features: [FEATURES.md](./FEATURES.md)

