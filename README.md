# Finance Data Processing and Access Control Backend

A production-grade Spring Boot backend for a finance dashboard system featuring JWT authentication, role-based access control, financial records management, and real-time analytics.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Default Credentials](#default-credentials)
- [Role Model & Access Control](#role-model--access-control)
- [API Reference](#api-reference)
- [Design Decisions](#design-decisions)
- [Assumptions](#assumptions)

---

## Tech Stack

| Layer         | Technology                        |
|---------------|-----------------------------------|
| Language      | Java 17                           |
| Framework     | Spring Boot 3.2                   |
| Security      | Spring Security + JWT (jjwt 0.11) |
| Persistence   | Spring Data JPA + H2 (file-based) |
| Validation    | Jakarta Bean Validation           |
| Build Tool    | Maven                             |
| Testing       | JUnit 5 + MockMvc                 |
| Boilerplate   | Lombok                            |

---

## Project Structure

```
src/main/java/com/financeapp/
├── config/
│   ├── SecurityConfig.java          # Spring Security + JWT filter chain
│   └── DataInitializer.java         # Seeds default users + sample transactions
├── controller/
│   ├── AuthController.java          # POST /api/auth/register, /login
│   ├── UserController.java          # CRUD for users (ADMIN only)
│   ├── TransactionController.java   # CRUD + filtering for transactions
│   └── DashboardController.java     # GET /api/dashboard/summary
├── dto/
│   ├── request/                     # Validated inbound payloads
│   └── response/                    # Outbound response shapes
├── entity/
│   ├── User.java
│   └── Transaction.java             # Includes soft-delete flag
├── enums/
│   ├── Role.java                    # VIEWER | ANALYST | ADMIN
│   ├── TransactionType.java         # INCOME | EXPENSE
│   └── UserStatus.java              # ACTIVE | INACTIVE
├── exception/
│   ├── GlobalExceptionHandler.java  # Unified error responses
│   ├── ResourceNotFoundException.java
│   └── BadRequestException.java
├── repository/
│   ├── UserRepository.java
│   └── TransactionRepository.java   # Custom JPQL for filters + aggregations
├── security/
│   ├── JwtUtils.java                # Token generation + validation
│   ├── JwtAuthFilter.java           # OncePerRequestFilter
│   └── UserDetailsServiceImpl.java
└── service/
    ├── AuthService.java / impl/
    ├── UserService.java / impl/
    ├── TransactionService.java / impl/
    └── DashboardService.java / impl/ # Aggregation logic
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Run the application

```bash
# Clone the repo
git clone <your-repo-url>
cd finance-backend

# Start the server
./mvnw spring-boot:run
```

The server starts at **http://localhost:8080**

The H2 database persists to `./data/financedb` — data survives restarts.

### H2 Console (optional debugging)

Visit **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:file:./data/financedb`
- Username: `sa`
- Password: `password`

### Run tests

```bash
./mvnw test
```

Tests use an in-memory H2 database and are isolated from your development data.

---

## Default Credentials

Three seed users are created automatically on first startup:

| Role    | Email                  | Password    |
|---------|------------------------|-------------|
| ADMIN   | admin@finance.com      | admin123    |
| ANALYST | analyst@finance.com    | analyst123  |
| VIEWER  | viewer@finance.com     | viewer123   |

Sample transactions are also seeded so the dashboard returns meaningful data immediately.

---

## Role Model & Access Control

| Endpoint                         | VIEWER | ANALYST | ADMIN |
|----------------------------------|:------:|:-------:|:-----:|
| POST /api/auth/register          | ✅     | ✅      | ✅    |
| POST /api/auth/login             | ✅     | ✅      | ✅    |
| GET  /api/transactions           | ✅     | ✅      | ✅    |
| GET  /api/transactions/{id}      | ✅     | ✅      | ✅    |
| GET  /api/transactions/categories| ✅     | ✅      | ✅    |
| GET  /api/dashboard/summary      | ❌     | ✅      | ✅    |
| POST /api/transactions           | ❌     | ❌      | ✅    |
| PUT  /api/transactions/{id}      | ❌     | ❌      | ✅    |
| DELETE /api/transactions/{id}    | ❌     | ❌      | ✅    |
| GET  /api/users                  | ❌     | ❌      | ✅    |
| POST /api/users                  | ❌     | ❌      | ✅    |
| PATCH /api/users/{id}/role       | ❌     | ❌      | ✅    |
| PATCH /api/users/{id}/status     | ❌     | ❌      | ✅    |
| DELETE /api/users/{id}           | ❌     | ❌      | ✅    |

Access control is enforced at two levels:
1. **URL-level** — in `SecurityConfig.java` via `authorizeHttpRequests`
2. **Method-level** — via `@PreAuthorize` annotations on controllers

---

## API Reference

All responses follow a standard envelope:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2026-04-05T10:00:00"
}
```

Error responses:
```json
{
  "success": false,
  "message": "Descriptive error message",
  "timestamp": "2026-04-05T10:00:00"
}
```

Validation errors include a `data` map of field → message.

---

### Auth

#### Register
```
POST /api/auth/register
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "secret123"
}
```
New accounts receive `VIEWER` role by default. An ADMIN can elevate via `PATCH /api/users/{id}/role`.

#### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@finance.com",
  "password": "admin123"
}
```
Response includes a `token`. Pass it as `Authorization: Bearer <token>` on subsequent requests.

---

### Transactions

#### List with filters + pagination
```
GET /api/transactions?type=INCOME&category=Salary&from=2026-01-01&to=2026-04-05&page=0&size=20
Authorization: Bearer <token>
```

| Parameter  | Type           | Description                    |
|------------|----------------|--------------------------------|
| type       | INCOME/EXPENSE | Filter by transaction type     |
| category   | string         | Filter by category (exact)     |
| from       | YYYY-MM-DD     | Start date (inclusive)         |
| to         | YYYY-MM-DD     | End date (inclusive)           |
| page       | int (default 0)| Page number (0-indexed)        |
| size       | int (default 20)| Page size (max 100)           |

#### Get one
```
GET /api/transactions/{id}
Authorization: Bearer <token>
```

#### Get categories (for filter dropdowns)
```
GET /api/transactions/categories
Authorization: Bearer <token>
```

#### Create (ADMIN)
```
POST /api/transactions
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2026-04-01",
  "notes": "Monthly salary"
}
```

#### Update (ADMIN)
```
PUT /api/transactions/{id}
Authorization: Bearer <token>
Content-Type: application/json

{ ...same fields as create... }
```

#### Delete (ADMIN — soft delete)
```
DELETE /api/transactions/{id}
Authorization: Bearer <token>
```
Records are marked `deleted=true` and hidden from all queries. The data is preserved in the database for audit purposes.

---

### Dashboard

#### Summary (ANALYST / ADMIN)
```
GET /api/dashboard/summary
Authorization: Bearer <token>
```

Response shape:
```json
{
  "data": {
    "totalIncome": 10800.00,
    "totalExpenses": 2315.00,
    "netBalance": 8485.00,
    "categoryTotals": {
      "Salary": 10000.00,
      "Freelance": 800.00,
      "Rent": 1200.00
    },
    "recentActivity": [ ... ],
    "monthlyTrends": [
      {
        "year": 2026,
        "month": 3,
        "income": 5800.00,
        "expenses": 2315.00,
        "net": 3485.00
      }
    ]
  }
}
```

---

### Users (ADMIN only)

```
GET    /api/users               → list all
GET    /api/users/{id}          → get one
POST   /api/users               → create with any role
PATCH  /api/users/{id}/role     → { "role": "ANALYST" }
PATCH  /api/users/{id}/status   → { "status": "INACTIVE" }
DELETE /api/users/{id}          → hard delete
```

---

## Design Decisions

### Soft Deletes on Transactions
Transactions are never physically removed. A `deleted` flag hides them from all queries. This preserves the audit trail and matches real-world finance system expectations.

### Dual-layer Access Control
Security rules are declared in both `SecurityConfig` (URL-level) and `@PreAuthorize` (method-level). The URL rules act as the first gate; method-level rules allow fine-grained control and serve as documentation of intent.

### Generic `ApiResponse<T>` Wrapper
Every endpoint returns the same envelope shape (`success`, `message`, `data`, `timestamp`). This makes frontend integration predictable and errors easy to parse.

### Layered Architecture
The project follows a strict Controller → Service → Repository pattern. Business logic lives exclusively in service classes. Controllers only handle HTTP concerns. Repositories only handle data access.

### Interface + Implementation for Services
Each service has a separate interface and implementation. This decouples the contract from the implementation, makes mocking in tests straightforward, and signals design intent clearly.

### File-based H2 Database
H2 is used in file mode (`jdbc:h2:file:./data/financedb`) so data persists across application restarts during development. The test profile overrides this with an in-memory database for isolation.

---

## Assumptions

1. **Self-registration** always produces a `VIEWER` account. Role elevation is an admin operation, not self-service.
2. **Transaction ownership** is recorded (`created_by`) but not used to restrict reads — all authenticated users can read all non-deleted transactions.
3. **Soft delete** applies only to transactions. Users are hard-deleted since they have no financial audit significance of their own.
4. **Monthly trends** cover the trailing 12 months from the current date.
5. **Page size** is capped at 100 to prevent accidental large result sets.
6. **JWT expiry** is set to 24 hours. Token refresh is not implemented (out of scope for this assignment).
