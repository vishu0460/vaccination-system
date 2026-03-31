# 🔌 API Documentation

OpenAPI/Swagger: http://localhost:8080/swagger-ui/index.html (local)

Full endpoints (~100+) from controllers. Key examples below.

## 📋 Endpoints Summary

### Auth (Public)
| Method | Path | Description | Request Body | Response Example |
|--------|------|-------------|--------------|------------------|
| POST | /api/auth/login | JWT login | `{email, password}` | `{accessToken, refreshToken, user}` |
| POST | /api/auth/register | User signup | `{name, email, phone, password, dob}` | `{message: \"Verify email\"}` |
| POST | /api/auth/refresh | Refresh JWT | `{refreshToken}` | `{accessToken, refreshToken}` |
| POST | /api/auth/verify-email | Email OTP | `{email, otp}` | `{message: \"Verified\"}` |
| POST | /api/auth/forgot-password | Reset init | `{email}` | `{message: \"OTP sent\"}` |

### Public/Catalog (No Auth)
| Method | Path | Description | Query Params | Response |
|--------|------|-------------|--------------|----------|
| GET | /api/public/centers | Search centers | `city, lat, lng, page` | `[{id, name, address, rating}]` |
| GET | /api/public/drives | Active drives | `city, status` | `[{id, centerId, dates, slots}]` |
| GET | /api/public/slots | Available slots | `driveId, date` | `[{id, time, capacity, status}]` |

### User (Role: USER)
| Method | Path | Description | Body | Response |
|--------|------|-------------|------|----------|
| POST | /api/user/bookings | Book slot | `{slotId}` | `{booking: {id, status}}` |
| GET | /api/user/bookings | My bookings | - | `[{id, slot, status, certificate}]` |
| GET | /api/user/recommendations/slots | Slot recs | - | `[{slotId, score}]` |
| GET | /api/user/notifications | Notifications | - | `[{id, message, status}]` |
| POST | /api/certificates | Generate cert | `{bookingId}` | `{certificateNumber, pdfUrl, qr}` |
| GET | /api/certificates/my-certificates | My certs | - | `[{id, number, date}]` |
| POST | /api/reviews | Submit review | `{centerId, rating, comment}` | `{reviewId}` |

### Admin (Role: ADMIN/SUPER_ADMIN)
| Method | Path | Description | Query | Response |
|--------|------|-------------|-------|----------|
| GET | /api/admin/dashboard/stats | Stats/charts | `period` | `{bookingsToday: 50, centers: 20}` |
| GET | /api/admin/bookings | All bookings | `page, status` | `[{id, user, slot}]` |
| PUT | /api/admin/bookings/{id}/complete | Complete booking | - | `{status: \"COMPLETED\"}` |
| POST | /api/admin/centers | Create center | `{name, address, city}` | `{centerId}` |
| GET | /api/admin/logs | Audit logs | `level, actor` | `[{timestamp, event, resource}]` |
| GET | /api/admin/search-analytics | Search insights | - | `{topCities: [\"Delhi\", \"Mumbai\"]}` |

### Super Admin
| Method | Path | Description | Body |
|--------|------|-------------|------|
| POST | /api/superadmin/admins | Create admin | `{email, role}` |
| PUT | /api/superadmin/users/{id} | Update user | `{role, active}` |
| DELETE | /api/superadmin/centers/{id} | Delete center | - |

### Certificate Verification (Public)
| Method | Path | Description | Path Param | Response |
|--------|------|-------------|------------|----------|
| GET | /api/certificates/verify/{number} | Verify cert | `certificateNumber` | `{valid: true, details}` |

## 📦 Request/Response Standards

**Success**:
```json
{
  \"success\": true,
  \"data\": {...},
  \"message\": \"Optional\"
}
```

**Error**:
```json
{
  \"success\": false,
  \"error\": {
    \"code\": \"VALIDATION_ERROR\",
    \"message\": \"Field required\",
    \"details\": [...]
  }
}
```

**Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`

## 🧪 Testing

```bash
curl -X POST http://localhost:8080/api/auth/login \\
  -H \"Content-Type: application/json\" \\
  -d '{\"email\":\"user@example.com\",\"password\":\"pass123\"}'
```

Rate limited (60/min). CORS: localhost:5174 etc.

**Full Spec**: Run backend → Swagger UI.

