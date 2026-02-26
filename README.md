# Vaccination Scheduling System

A comprehensive Smart Scheduling and Notification System for Vaccination Drives. This full-stack application allows users to book vaccination slots, manage drives, and administrators to manage the entire system.

## Features

- **User Registration & Authentication** - Secure JWT-based authentication
- **Vaccination Drive Management** - View and manage vaccination drives
- **Slot Booking** - Book vaccination slots at convenient times
- **Center Management** - Manage vaccination centers
- **Admin Dashboard** - Admin panel for system management
- **Booking Management** - View, approve, reject, and complete bookings

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.0
- Spring Security with JWT
- Spring Data JPA
- H2 Database (for development) / MySQL (for production)
- Maven

### Frontend
- React 18
- Vite
- React Router
- Axios
- Bootstrap 5

## Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- Maven 3.8+
- MySQL 8.0+ (optional, for production)

## Quick Start (Windows)

### Option 1: Using Batch Scripts

1. **Start Backend:**
   
```
cmd
   double-click start-backend.bat
   
```
   The backend will start on http://localhost:8080 with H2 database.

2. **Start Frontend:**
   
```
cmd
   double-click start-frontend.bat
   
```
   The frontend will start on http://localhost:5173

### Option 2: Manual Start

**Backend:**
```
cmd
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=h2"
```

**Frontend:**
```
cmd
cd frontend
npm install
npm run dev
```

## Accessing the Application

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080/api
- **H2 Console:** http://localhost:8080/h2-console (dev mode only)

## Default Credentials

After starting the application, you can login with:

- **Admin Account:**
  - Email: admin@vaccinesystem.com
  - Password: Admin@123

## Environment Variables

### Backend (.env)

Create a `backend/.env` file:

```env
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/vaccination_db
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your_secret_key
JWT_EXPIRATION=86400000

# Mail Configuration
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=h2
```

### Frontend (.env)

Create a `frontend/.env` file:

```
env
VITE_API_URL=http://localhost:8080/api
```

## Database Setup

### MySQL Setup (Production)

1. Create the database:
```
sql
CREATE DATABASE vaccination_db;
```

2. Run the schema file:
```
cmd
mysql -u root -p vaccination_db < backend/src/main/resources/schema-mysql.sql
```

3. Switch profile to MySQL:
```
cmd
set SPRING_PROFILES_ACTIVE=dev
```

## API Endpoints

### Public Endpoints
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/public/drives` - Get all drives
- `GET /api/public/centers` - Get all centers

### Protected Endpoints (Require JWT)
- `GET /api/auth/me` - Get current user
- `POST /api/auth/refresh` - Refresh token
- `GET /api/bookings/my` - Get user's bookings
- `POST /api/bookings` - Create booking

### Admin Endpoints
- `GET /api/admin/dashboard` - Dashboard stats
- `GET /api/admin/users` - Manage users
- `POST /api/admin/drives` - Create drive
- `PUT /api/admin/centers/:id` - Update center

## Security Features

- JWT Token Authentication
- Password Hashing (BCrypt)
- CORS Configuration
- CSRF Protection
- Rate Limiting
- Input Validation
- SQL Injection Protection (via JPA)
- Security Headers (HSTS, Frame Options)

## Project Structure

```
vaccination-system/
├── backend/
│   ├── src/main/java/com/vaccine/
│   │   ├── config/         # Security, CORS configs
│   │   ├── controller/    # REST controllers
│   │   ├── dto/           # Data Transfer Objects
│   │   ├── entity/        # JPA entities
│   │   ├── exception/     # Exception handlers
│   │   ├── repository/    # Data repositories
│   │   ├── security/     # JWT, filters
│   │   ├── service/      # Business logic
│   │   └── util/         # Utilities
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── schema-mysql.sql
│   │   └── import.sql
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/          # API calls
│   │   ├── components/   # React components
│   │   ├── pages/       # Page components
│   │   ├── routes/      # Route definitions
│   │   └── styles/      # CSS styles
│   ├── package.json
│   └── vite.config.js
├── start-backend.bat
├── start-frontend.bat
└── README.md
```

## Troubleshooting

### Backend won't start
- Check if port 8080 is available
- Ensure Java 17+ is installed
- Check logs in `backend/logs/vaccination-system.log`

### Frontend won't start
- Delete `node_modules` and reinstall
- Ensure Node.js 18+ is installed

### Database issues
- For H2: Database is in-memory, data resets on restart
- For MySQL: Ensure database exists and credentials are correct

## License

This project is for educational purposes.
