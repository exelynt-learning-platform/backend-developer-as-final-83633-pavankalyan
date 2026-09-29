# Resource Booking System

A secure RESTful Resource Booking System built with **Java 17, Spring Boot 3.5.16, Spring Security, JWT, PostgreSQL, JPA/Hibernate, and Docker**.

The system supports authentication, role-based authorization, resource management, reservations, conflict detection, filtering, pagination, sorting, validation, centralized error handling, Swagger/OpenAPI documentation, integration tests, and Docker Compose.

---

## Features

- JWT authentication with BCrypt password hashing
- USER / ADMIN role-based authorization
- Resource CRUD for ADMIN
- Read-only resource access for authenticated users
- USER reservation creation
- USER ownership-based reservation access
- ADMIN reservation management
- `PENDING`, `CONFIRMED`, and `CANCELLED` reservation statuses
- Reservation overlap detection
- Cancelled reservations do not block future bookings
- Status and price-range filtering
- Pagination and sorting
- Request validation
- Centralized exception handling
- PostgreSQL with JPA/Hibernate
- Swagger/OpenAPI documentation
- Integration and security tests
- Docker Compose with persistent PostgreSQL storage
- Environment-based configuration
- Stateless JWT-based security

---

## Architecture

```text
Client
  |
  v
Spring Security + JWT
  |
  v
Controller
  |
  v
Service
  |
  v
Repository
  |
  v
PostgreSQL
```

The application follows a layered architecture:

```text
src/main/java/com/example/bookingsystem
│
├── config
├── controller
├── dto
├── entity
├── exception
├── mapper
├── repository
├── security
└── service
```

### Layer Responsibilities

- **Controller** - Handles HTTP requests and responses
- **Service** - Contains business logic and validation
- **Repository** - Handles database access
- **Entity** - Represents persistent database models
- **DTO** - Defines API request and response models
- **Mapper** - Converts entities to DTOs and vice versa
- **Security** - Handles JWT authentication and authorization
- **Exception** - Provides centralized API error handling
- **Config** - Application and security configuration

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 | Programming language |
| Spring Boot 3.5.16 | Backend framework |
| Spring Security | Authentication and authorization |
| JJWT 0.12.6 | JWT token generation and validation |
| Spring Data JPA | Persistence abstraction |
| Hibernate | ORM |
| PostgreSQL 16 | Production database |
| H2 | Test database |
| SpringDoc OpenAPI 2.9.1 | Swagger/OpenAPI documentation |
| Maven | Build tool |
| Docker | Containerization |
| Docker Compose | Multi-container application setup |

---

## Roles & Access

| Operation | USER | ADMIN |
|---|:---:|:---:|
| Login | ✓ | ✓ |
| View resources | ✓ | ✓ |
| Create resources | — | ✓ |
| Update resources | — | ✓ |
| Delete resources | — | ✓ |
| Create reservation | ✓ | — |
| Create reservation for a user | — | ✓ |
| View own reservations | ✓ | — |
| View own reservation by ID | ✓ | — |
| View all reservations | — | ✓ |
| View reservation by ID | — | ✓ |
| Update reservation | — | ✓ |
| Update reservation status | — | ✓ |
| Delete reservations | — | ✓ |

---

## API Endpoints

### Authentication

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/auth/login` | Public | Authenticate user and receive JWT |

### Resources

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/resources` | ADMIN | Create a resource |
| GET | `/resources` | Authenticated | Get paginated resources |
| GET | `/resources/{id}` | Authenticated | Get resource by ID |
| PUT | `/resources/{id}` | ADMIN | Update a resource |
| DELETE | `/resources/{id}` | ADMIN | Delete a resource |

### Reservations

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/reservations` | USER | Create a reservation |
| POST | `/reservations/admin` | ADMIN | Create a reservation for a specific user |
| GET | `/reservations/my` | USER | Get the authenticated user's reservations |
| GET | `/reservations/my/{id}` | USER | Get one of the authenticated user's reservations |
| GET | `/reservations` | ADMIN | Get all reservations |
| GET | `/reservations/{id}` | ADMIN | Get any reservation by ID |
| PUT | `/reservations/{id}` | ADMIN | Update a reservation |
| PATCH | `/reservations/{id}/status` | ADMIN | Update reservation status |
| DELETE | `/reservations/{id}` | ADMIN | Delete a reservation |

---

## Reservation Rules

The following business rules are enforced:

- `startAt` must be before `endAt`.
- Reservation times must be in the future.
- The selected resource must exist.
- The selected resource must be available.
- Overlapping `PENDING` or `CONFIRMED` reservations for the same resource are rejected.
- Adjacent bookings are allowed.
- `CANCELLED` reservations do not participate in conflict detection.
- Reservation price is captured from the resource price when the reservation is created.
- USER identity is taken from the authenticated JWT.
- The client cannot specify another user's identity when creating a reservation.
- Reservation ownership is verified when USER endpoints are accessed.

### Reservation Status Transitions

```text
PENDING
   ├──> CONFIRMED
   └──> CANCELLED

CONFIRMED
   └──> CANCELLED

CANCELLED
   └──> No further transitions
```

A reservation status update is idempotent when the requested status matches the current status; the existing reservation is returned unchanged.
Invalid status transitions are rejected.

---

## Filtering, Pagination & Sorting

Reservations support filtering by:

- Status
- Minimum price
- Maximum price

### Examples

#### Filtering

```http
GET /reservations?status=PENDING
```

```http
GET /reservations?minPrice=100&maxPrice=1000
```

```http
GET /reservations?status=CONFIRMED&minPrice=100&maxPrice=1000
```

#### Pagination


```http
GET /reservations?page=0&size=10
```

#### Sorting

```http
GET /resources?page=0&size=10&sort=name,asc
```

Invalid sort properties return:

```text
400 Bad Request
```

---

## Authentication

Authentication is handled through JWT tokens.

### Login

```http
POST /auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "user@example.com",
  "password": "User@12345"
}
```

Successful response:

```json
{
  "accessToken": "<JWT_TOKEN>",
  "tokenType": "Bearer"
}
```

Use the returned token when calling protected endpoints:

```http
Authorization: Bearer <JWT_TOKEN>
```

The application uses stateless authentication. The JWT contains the authenticated user's identity and role.

---

## Configuration

Application configuration is environment-based.

A sample configuration is provided in:

```text
.env.example
```

Example:

```env
DB_URL=jdbc:postgresql://localhost:5432/booking_system
DB_USERNAME=postgres
DB_PASSWORD=change-me

SERVER_PORT=8080

JWT_SECRET=change-this-to-a-secure-random-secret-key
JWT_EXPIRATION=900000

SEED_ENABLED=false

SEED_ADMIN_EMAIL=admin@example.com
SEED_ADMIN_PASSWORD=change-me

SEED_USER_EMAIL=user@example.com
SEED_USER_PASSWORD=change-me
```

### Configuration Properties

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `SERVER_PORT` | Application port |
| `JWT_SECRET` | Secret used for signing JWT tokens |
| `JWT_EXPIRATION` | JWT expiration time in milliseconds |
| `SEED_ADMIN_EMAIL` | Initial ADMIN email |
| `SEED_ADMIN_PASSWORD` | Initial ADMIN password |
| `SEED_USER_EMAIL` | Initial USER email |
| `SEED_USER_PASSWORD` | Initial USER password |

Do not commit real passwords, database credentials, or JWT secrets.

---

## Run Locally

### Prerequisites

- Java 17+
- PostgreSQL 16+
- Git

### 1. Clone the Repository

```bash
git clone <repository-url>
cd backend-developer-as-final-83633-pavankalyan
```

### 2. Create the Database

Create a PostgreSQL database:

```sql
CREATE DATABASE booking_system;
```

### 3. Configure Environment Variables

Set the required environment variables.

For example:

```env
DB_URL=jdbc:postgresql://localhost:5432/booking_system
DB_USERNAME=postgres
DB_PASSWORD=your-password
JWT_SECRET=your-secure-jwt-secret
```

The JWT secret should be a strong random value.

### 4. Run Tests

```bash
./mvnw clean test
```

### 5. Start the Application

```bash
./mvnw spring-boot:run
```

The application will start on:

```text
http://localhost:8080
```

---

## Run with Docker

Docker Compose runs both the Spring Boot application and PostgreSQL.

```bash
docker compose up --build
```

Or run in detached mode:

```bash
docker compose up --build -d
```

Docker Compose starts:

```text
booking-system-app
booking-system-postgres
```

The application will be available at:

```text
http://localhost:8080
```

### Stop the Containers

To stop the application without deleting PostgreSQL data:

```bash
docker compose down
```

### Start Again

```bash
docker compose up -d
```

The PostgreSQL data is stored in the Docker volume:

```text
postgres_data
```

The volume allows database data to persist when containers are stopped or recreated.

### Remove the Database Volume

To remove the PostgreSQL volume and all stored database data:

```bash
docker compose down -v
```

> **Warning:** `docker compose down -v` permanently removes the PostgreSQL volume and its stored data.

---

## Swagger / OpenAPI

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

Swagger supports JWT authentication through the **Authorize** button.

Enter:

```text
Bearer <JWT_TOKEN>
```

Then protected endpoints can be tested directly from Swagger UI.

---

## Error Handling

The application uses centralized exception handling and returns a consistent error response.

Example:

```json
{
  "timestamp": "2026-09-27T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/resources",
  "fieldErrors": []
}
```

### HTTP Status Codes

| Status | Meaning |
|---|---|
| 400 | Invalid request, validation error, malformed request, or invalid parameter |
| 401 | Missing or invalid authentication |
| 403 | Insufficient permissions |
| 404 | Resource, reservation, or user not found |
| 409 | Reservation conflict |
| 500 | Unexpected server error |

---

## Validation

Request validation is implemented using Jakarta Bean Validation.

Examples include:

- Required resource fields
- Resource name length validation
- Resource description length validation
- Non-negative resource price
- Required resource availability
- Required reservation resource ID
- Required reservation start and end times
- Future reservation times
- Reservation status validation
- Price-range validation

Business validations are handled in the service layer.

---

## Testing

The project includes an integration test suite using **H2** as the test database.

The tests cover:

- Authentication
- Invalid credentials
- Unauthorized access
- USER / ADMIN authorization
- Resource access control
- Resource management
- Reservation creation
- Reservation ownership
- Reservation conflict detection
- Adjacent reservations
- Invalid reservation times
- Reservation status transitions
- Reservation update validation
- Reservation deletion
- Status filtering
- Price-range filtering
- Invalid status transitions
- Invalid sorting
- Malformed JSON
- Missing request body
- Pagination
- Sorting
- Large page-size handling

Run the complete test suite:

```bash
./mvnw clean test
```

Expected result:

```text
Tests run: 62, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Security

The application implements several security measures:

- JWT-based stateless authentication
- BCrypt password hashing
- Role-based authorization
- Method-level authorization
- JWT signature validation
- JWT expiration validation
- Authenticated user identity from JWT
- USER reservation ownership checks
- ADMIN-only resource management
- ADMIN-only reservation management
- Environment-based secrets
- `.env` excluded from Git
- Centralized exception handling
- Request validation
- CSRF disabled for the stateless REST API
- Authentication and authorization failures return JSON responses

### Production Security Recommendations

For production deployment:

- Use strong randomly generated JWT secrets.
- Never use the development seed passwords.
- Use HTTPS/TLS.
- Store secrets in a dedicated secret manager.
- Use secure database credentials.
- Enable appropriate database backups and monitoring.
- Use a controlled database migration strategy instead of relying on automatic schema updates.
- Review and restrict exposed management and documentation endpoints as appropriate for the deployment environment.

---

## Default Development Users

The application creates development users when they do not already exist.

### ADMIN

```text
Email: admin@example.com
Password: Admin@12345
Role: ADMIN
```

### USER

```text
Email: user@example.com
Password: User@12345
Role: USER
```

These values can be overridden using:

```text
SEED_ADMIN_EMAIL
SEED_ADMIN_PASSWORD
SEED_USER_EMAIL
SEED_USER_PASSWORD
```

> **Important:** These are development credentials only. Do not use the default passwords in production.

---

## Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.example.bookingsystem
│   │       ├── config
│   │       ├── controller
│   │       ├── dto
│   │       │   ├── auth
│   │       │   ├── reservation
│   │       │   └── resource
│   │       ├── entity
│   │       ├── exception
│   │       ├── mapper
│   │       ├── repository
│   │       ├── security
│   │       └── service
│   │
│   └── resources
│       └── application.yaml
│
├── test
│   └── java
│       └── com.example.bookingsystem
│           ├── AuthenticationIntegrationTest.java
│           ├── BaseIntegrationTest.java
│           ├── ErrorHandlingIntegrationTest.java
│           ├── ReservationAccessIntegrationTest.java
│           ├── ReservationCreationIntegrationTest.java
│           ├── ReservationFilteringIntegrationTest.java
│           ├── ReservationManagementIntegrationTest.java
│           ├── ReservationPaginationAndSortingIntegrationTest.java
│           ├── ResourceIntegrationTest.java
│           └── ResourcePaginationAndSortingIntegrationTest.java
│
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .env.example
├── pom.xml
└── README.md
```

---

## Database

The application uses PostgreSQL for persistent storage.

Main entities:

```text
User 1 ─────────< N Reservation N >──────── 1 Resource
```

### User

Stores:

- ID
- Email
- BCrypt password
- Role

### Resource

Stores:

- ID
- Name
- Description
- Price
- Availability

### Reservation

Stores:

- ID
- User
- Resource
- Start time
- End time
- Status
- Reservation price

The reservation price is stored independently so that a reservation retains the resource price that existed when the booking was created.

---

## Database Persistence with Docker

PostgreSQL uses a named Docker volume:

```text
postgres_data
```

Running:

```bash
docker compose down
```

does **not** remove the volume.

Running:

```bash
docker compose down -v
```

removes the volume and its database data.

This allows the application and PostgreSQL containers to be recreated while preserving existing data.

---

## Development Workflow

A typical development workflow is:

```bash
# Run tests
./mvnw clean test

# Start with Docker
docker compose up --build
```

For local development without Docker:

```bash
./mvnw spring-boot:run
```

---

## License

This project was developed as a backend engineering assignment demonstrating REST API development, authentication, authorization, persistence, validation, testing, API documentation, and containerization.
