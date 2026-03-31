# ✨ Features

Role-based capabilities with screenshots.

## 🏠 Public/User Features

| Feature | Description | Page/Endpoint |
|---------|-------------|---------------|
| **Smart Search** | City/drive/slot autocomplete & filters | HomePage, CentersPage |
| **Booking** | Select slot, confirm, waitlist | UserBookingsPage, /api/user/bookings |
| **My Profile** | Update details, change password, deactivate | ProfilePage |
| **Certificates** | Generate/verify/download PDF+QR | CertificatePage |
| **Notifications** | Realtime updates (WebSocket) | /api/user/notifications |
| **Reviews/Feedback** | Rate centers, submit inquiries | CenterDetailPage |
| **Recommendations** | Personalized slot suggestions | /api/user/recommendations/slots |

![User Booking Flow](docs/screenshots/user-bookings.png)

## 👨‍💼 Admin Features

| Feature | Description |
|---------|-------------|
| **Dashboard** | Stats, charts (bookings/centers), analytics | AdminDashboardPage |
| **Management** | CRUD bookings/centers/drives/slots/users | /admin/* endpoints |
| **Logs** | Audit/search/activity timeline | LogsTable |
| **Bulk Actions** | Complete/cancel bookings | AdminController |

![Admin Dashboard](docs/screenshots/admin-dashboard.png)

## 🦸‍♂️ Super Admin Features

| Feature | Description |
|---------|-------------|
| **Global CRUD** | Users/admins/centers/drives/slots | SuperAdminController |
| **Role Assignment** | Promote/demote users | /superadmin/admins |

## 🔧 Technical Features

- **Realtime**: STOMP WebSocket for slot/status updates
- **PWA**: Offline-capable, installable
- **SEO**: React Helmet, sitemap.xml, Open Graph
- **Analytics**: Booking/search trends (Admin only)
- **Exports**: CSV bookings, PDF certs
- **Multi-Channel Notify**: Email/SMS (Twilio)

**Screenshots Folder**: `docs/screenshots/` (add real captures)

Roadmap: [TODO.md](./TODO.md)

