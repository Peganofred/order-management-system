# Order Management System

A backend for an e-commerce order management platform, built with **Java 17** and **Spring Boot 3** as a portfolio project targeting Java Backend Developer roles.

The project is being built in explicit, incremental phases — each phase is a real, working slice of a production-style e-commerce backend (auth → catalog → cart → orders → messaging → deployment), rather than one big upfront build.

> **Status:** Phase 2 of 12 complete. See [Roadmap](#roadmap) below.

<!-- Once GitHub Actions is added in a later phase:
![CI](https://github.com/<your-username>/order-management-system/actions/workflows/ci.yml/badge.svg)
-->
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Endpoints](#api-endpoints)
- [Design Decisions](#design-decisions)
- [Roadmap](#roadmap)
- [License](#license)

---

## Tech Stack

| Layer            | Technology                                  |
|-------------------|----------------------------------------------|
| Language           | Java 17                                       |
| Framework          | Spring Boot 3.3.4 (Web, Data JPA, Validation, Security) |
| Database           | PostgreSQL 16                                 |
| Schema migrations  | Flyway                                        |
| Auth               | Spring Security + JWT (JJWT 0.12.6)           |
| Build tool         | Maven                                         |
| Containerization   | Docker Compose (Postgres for local dev)       |

Planned for later phases: Redis (caching), Kafka (async events), full Docker Compose app stack, GitHub Actions CI/CD, AWS deployment.

## Architecture

The service follows a classic layered architecture:

```
┌─────────────────────────────────────────────┐
│                 Controller                   │  REST endpoints, request/response DTOs
│   AuthController · UserController · Admin    │
├─────────────────────────────────────────────┤
│                   Service                    │  Business logic, transactions
│         AuthService · UserDetailsService     │
├─────────────────────────────────────────────┤
│                  Repository                  │  Spring Data JPA
│                UserRepository                │
├─────────────────────────────────────────────┤
│                   Entity                     │  JPA-mapped domain model
│                    User                      │
└─────────────────────────────────────────────┘
                     │
                     ▼
              PostgreSQL (schema owned by Flyway)
```

Cutting across the layers:

- **`security/`** — `JwtService` (issues/validates tokens) and `JwtAuthenticationFilter` (runs once per request, before `UsernamePasswordAuthenticationFilter`, to populate the `SecurityContext` from the `Authorization` header).
- **`config/SecurityConfig`** — stateless session policy, public vs. authenticated routes, JSON error responses for 401/403, method-level `@PreAuthorize` enabled.
- **`dto/`** — request/response shapes are always separate from JPA entities; entities never cross the controller boundary.
- **`exception/`** — domain exceptions (`DuplicateEmailException`, `InvalidCredentialsException`) mapped to proper HTTP status codes.

Database schema is version-controlled with Flyway migrations under `src/main/resources/db/migration` — Hibernate is set to `ddl-auto: validate`, so the schema is never auto-generated at runtime; every change is an explicit, reviewable SQL migration.

## Getting Started

### Prerequisites

- JDK 17+
- Maven 3.9+
- Docker + Docker Compose (for Postgres)

### 1. Clone and start the database

```bash
git clone https://github.com/<your-username>/order-management-system.git
cd order-management-system
docker compose up -d
```

This starts a PostgreSQL 16 container (`orderdb` / `orderuser` / `orderpass`, exposed on `5432`) with a healthcheck.

### 2. Configure environment variables

Copy the example env file and adjust if needed (defaults work out of the box with the Docker Compose Postgres above):

```bash
cp .env.example .env
```

See [Environment Variables](#environment-variables) for what each value does.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

Flyway will run migrations automatically on startup. The API is available at `http://localhost:8080`.

### 4. Try it out

```bash
# Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"password123","fullName":"Jane Doe"}'

# Log in
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"password123"}'

# Call a protected endpoint with the returned token
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <token>"
```

## Environment Variables

The app reads sensitive/environment-specific config from environment variables, falling back to clearly-marked **dev-only** defaults so the project still runs out of the box locally. **Never rely on the defaults outside local development.**

| Variable            | Purpose                                   | Dev default (in `application.yml`)      |
|---------------------|--------------------------------------------|------------------------------------------|
| `JWT_SECRET`         | HMAC signing key for JWTs (HS256, ≥32 bytes) | `dev-only-secret-do-not-use-in-prod...` |
| `JWT_EXPIRATION_MS`  | Token lifetime in milliseconds             | `3600000` (1 hour)                        |
| `DB_URL`             | Postgres JDBC URL (see note below)         | `jdbc:postgresql://localhost:5432/orderdb`|
| `DB_USERNAME`        | Postgres username                          | `orderuser`                               |
| `DB_PASSWORD`        | Postgres password                          | `orderpass`                               |

> **Note:** the datasource block in `application.yml` currently has literal dev values rather than `${DB_URL:...}`-style placeholders. That's fine for a local-only Postgres container with throwaway credentials, but before deploying anywhere real, wire these three up the same way `JWT_SECRET` already is, and load them from `.env` via Docker Compose / your deployment platform's secrets manager — never commit real values.

See `.env.example` for the full list with comments.

## API Endpoints

All routes are prefixed with `/api/v1`.

| Method | Endpoint            | Auth required | Description                              |
|--------|----------------------|---------------|--------------------------------------------|
| POST   | `/auth/register`     | No            | Create a new account (default role: `CUSTOMER`) |
| POST   | `/auth/login`        | No            | Authenticate, returns a JWT                |
| GET    | `/users/me`          | Yes (any role)| Return the authenticated user's profile    |
| GET    | `/admin/ping`        | Yes (`ADMIN`) | Demo endpoint proving RBAC works end-to-end (403 for non-admins) |
| GET    | `/actuator/health`   | No            | Health check                               |
| GET    | `/actuator/info`     | No            | Build/app info                             |

Protected endpoints expect `Authorization: Bearer <jwt>`.

## Design Decisions

A few choices worth being able to explain in an interview:

- **Stateless JWT auth, not sessions.** No `HttpSession` is created (`SessionCreationPolicy.STATELESS`); every request authenticates itself via the token. This lets the API scale horizontally without sticky sessions, and is why CSRF protection is safely disabled — CSRF defends session cookies that are auto-attached by the browser, which doesn't apply to a bearer token in an `Authorization` header.
- **UUID primary keys**, generated in the JVM (`@UuidGenerator`) rather than via a Postgres extension like `pgcrypto`. Keeps ID generation visible in application code and avoids a DB-level dependency for something this simple.
- **Flyway-managed schema, `ddl-auto: validate`.** Hibernate is never allowed to auto-generate or silently alter the schema. Every schema change is an explicit, numbered, reviewable SQL file — the same discipline a real production team uses.
- **DTOs at every boundary.** Entities never leave the service layer; `RegisterRequest` intentionally has no `role` field so a client can never self-promote to `ADMIN` at signup.
- **Role checks via `@PreAuthorize` at the method level**, not a growing list of `.requestMatchers().hasRole()` in `SecurityConfig`. Keeps the authorization rule next to the code it protects.
- **Roles stored as plain `VARCHAR`, not a Postgres native `ENUM`.** Adding a new role later (e.g. `SUPPORT`) is a simple data change, not a database type migration.

## Roadmap

| Phase | Scope                                              | Status         |
|-------|-----------------------------------------------------|----------------|
| 1     | Project setup, PostgreSQL, Flyway baseline           | ✅ Done         |
| 2     | JWT auth, Spring Security, RBAC                      | ✅ Done         |
| 3     | Product & Inventory management                       | ⏳ Planned      |
| 4     | Shopping cart                                        | ⏳ Planned      |
| 5     | Order placement & lifecycle                          | ⏳ Planned      |
| 6     | Global exception handling, validation polish         | ⏳ Planned      |
| 7     | Redis caching                                        | ⏳ Planned      |
| 8     | Kafka event-driven notifications                     | ⏳ Planned      |
| 9+    | Full Docker Compose stack, GitHub Actions CI/CD, AWS deployment | ⏳ Planned |

Tracked in more detail via [GitHub Issues](../../issues) / [Projects](../../projects) as each phase starts.

## License

MIT — see [LICENSE](LICENSE).
