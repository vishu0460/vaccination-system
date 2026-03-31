# 🏗️ Architecture

High-level design of VaxZone.

## 📊 System Overview

```
┌─────────────────┐     ┌──────────────────┐
│   React/Vite    │◄──► │   Nginx (Prod)   │
│  (Frontend)     │     │ (HTTPS/CORS)     │
└─────────┬───────┘     └────────┬─────────┘
          │ HTTP/WS             │ Port 80/443
          ▼                     │
┌─────────▼──────┐             │
│ Backend API     │◄── Docker ───┘
│ (Spring Boot)   │
│ Port 8080       │
├─────────────────┤
│ Controllers     │
├─────────────────┤
│ Services        │  ┌─────────────┐
├─────────────────┤  │   MySQL     │ Flyway
│ Repositories    │◄─┤  (Prod)     │ Migrations
└─────────────────┘  │   H2 (Dev)  │ (27 scripts)
                     └─────────────┘
                           ↑
                    ┌─────────────┐
                    │ Audit Logs  │
                    │ Notifications│
                    └─────────────┘
```

## 🧩 Layers

1. **Presentation**: React pages/components → Axios client → API calls
2. **API Layer**: @RestController → DTO validation
3. **Business Logic**: Services (BookingService, NotificationService) + @Scheduled
4. **Persistence**: Spring Data JPA Repos → Entities (soft-delete)
5. **Realtime**: WebSocket/STOMP → Slot/booking updates

## 🔄 Data Flow Example: Booking

```
User clicks Book → POST /api/user/bookings
  ↓
Controller → BookingService.book()
  ↓ (Transaction)
Repo.save(Booking) + Slot.capacity--
  ↓
NotificationService → WebSocket + Email/SMS
  ↓
Admin dashboard realtime update
```

## 📈 Key Design Decisions

- **Monorepo**: Backend + Frontend co-located
- **Env-Driven**: No hardcoded secrets/DB (profiles: local/prod)
- **Stateless**: JWT (session-less scaling)
- **Migrations**: Flyway (evolutionary schema)
- **Caching**: Spring Cache (drives/slots)
- **PWA**: Service worker for offline catalog

## 🚀 Deployment Topology

```
Internet ──► Load Balancer/Nginx ──► Backend Pods (K8s/Render)
                              │
                       MySQL RDS/Cluster
                              │
                       Redis (cache/jobs future)
```

Scales horizontally. Observability: Actuator /health + logs.

Details: [PROJECT_STRUCTURE.md](./PROJECT_STRUCTURE.md)

