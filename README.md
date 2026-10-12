# Javora Commerce

Full-stack e-commerce platform built with **Spring Boot** and **React** in a single repository.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Flyway |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, TanStack Query |
| Database | PostgreSQL 17, Redis 7 |
| Storage | MinIO (S3 compatible) |
| Testing | JUnit 5, Testcontainers, Vitest, React Testing Library |

## Project Structure

```
javora-commerce/
├── backend/             # Spring Boot REST API
├── frontend/            # React single page application
├── docker-compose.yml   # Local infrastructure
└── PLAN.md              # Development plan and roadmap
```

## Prerequisites

- Java 21
- Node.js 22
- Docker Desktop

## Getting Started

1. Copy the environment template:
   ```bash
   cp .env.example .env
   ```
2. Start the infrastructure (PostgreSQL on port 5433, Redis, MinIO, Mailpit):
   ```bash
   docker compose up -d
   ```
3. Run the backend on http://localhost:8080:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
4. Run the frontend on http://localhost:5173 (API calls are proxied to the backend):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Useful URLs

| Service | URL |
|---|---|
| Frontend (dev) | http://localhost:5173 |
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| Mailpit inbox | http://localhost:8025 |
| MinIO console | http://localhost:9001 |

## Development Accounts

The `dev` profile creates an admin account on first start.

| Role | Email | Password |
|---|---|---|
| Admin | admin@javora.local | Admin12345 |

The `dev` profile also seeds a demo catalog (categories and products) when the database has no products. Product images are uploaded in the admin area and stored in MinIO.

Customers can register at http://localhost:5173/register. Verification and password reset emails appear in Mailpit.

In production set `JWT_SECRET` (at least 32 characters), `AUTH_COOKIE_SECURE=true`, and `APP_BOOTSTRAP_ADMIN_EMAIL` / `APP_BOOTSTRAP_ADMIN_PASSWORD` for the first admin.

## Common Commands

| Task | Command |
|---|---|
| Backend tests | `cd backend && ./mvnw verify` |
| Frontend tests | `cd frontend && npm run test` |
| Frontend lint and format check | `cd frontend && npm run lint && npm run format:check` |
| Generate API types from OpenAPI | `cd frontend && npm run gen:api` (backend must be running) |
| Build a single deployable jar | `cd backend && ./mvnw package -Pfrontend` |
| Run the packaged app | `java -jar backend/target/javora-commerce-0.0.1-SNAPSHOT.jar` |

## Production Build

The `frontend` Maven profile builds the React app and bundles it into the Spring Boot jar, so the SPA and the API are served from the same origin.
