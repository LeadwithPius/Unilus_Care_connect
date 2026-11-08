# UNILUS CareConnect

CareConnect is a student wellbeing Progressive Web App for the University of Lusaka. The repository contains a Spring Boot API in `backend/` and the React + Vite web application in `frontend/`. The older static frontend material is retained in `Frontend_Material/` for reference during the migration.

## Technology

- Backend: Java 21, Spring Boot, Spring Security, JWT, PostgreSQL and Flyway.
- Frontend: React, Vite, React Router, Axios, TanStack Query, Motion and Lucide.
- Authentication: JWT bearer tokens; role-based access for students, counsellors and administrators.

## Prerequisites

- JDK 21
- Node.js 22 or a compatible supported Node.js release
- PostgreSQL
- npm

## Run locally

### 1. Configure PostgreSQL and the backend

Create the `careconnect` database and `careconnect_user` role if they do not already exist. Then open a terminal:

```bash
cd backend

export DB_PASSWORD='your-postgres-password'
export CARECONNECT_JWT_SECRET="$(openssl rand -hex 32)"

# Optional: only for local development without verification email delivery.
# Production should leave this unset so verification remains required.
export CARECONNECT_AUTH_EMAIL_VERIFICATION_REQUIRED=false

./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. Flyway applies database migrations automatically. Hibernate validates the resulting schema.

For email delivery, configure `CARECONNECT_MAIL_USERNAME` and `CARECONNECT_MAIL_PASSWORD`. Email verification is required by default; the local override above is intended only for development environments without SMTP configured.

For initial administrator provisioning, configure `CARECONNECT_ADMIN_BOOTSTRAP_ENABLED=true`, `CARECONNECT_ADMIN_EMAIL` and `CARECONNECT_ADMIN_PASSWORD` in the environment. Keep bootstrap disabled after the administrator account has been created.

### 2. Run the React frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

By default, the frontend API client uses `http://localhost:8080/api` when running locally. Set `VITE_API_BASE_URL` before starting Vite when the API is hosted elsewhere.

### 3. Build and lint the frontend

```bash
cd frontend
npm run lint
npm run build
```

The production build is generated in `frontend/dist/`. Do not commit `node_modules/` or `dist/`.

## Application routes

- `/` — public landing page
- `/login` — student, counsellor and administrator sign-in
- `/register` — UNILUS student registration
- `/forgot-password` — password reset request
- `/reset-password` — password reset token handler
- `/verify-email` — email verification token handler
- `/student` — student portal
- `/counsellor` — counsellor portal
- `/admin` — administrator portal

The portal routes validate the current session against `GET /api/auth/me` and enforce the expected account role. Portal functionality is being migrated incrementally from the legacy frontend.

## Configuration and security

Do not commit database passwords, JWT secrets, SMTP credentials, administrator bootstrap credentials, `.env` files or real user credentials.

Important environment variables:

| Variable | Purpose |
| --- | --- |
| `DB_PASSWORD` | PostgreSQL password |
| `CARECONNECT_JWT_SECRET` | JWT signing secret |
| `CARECONNECT_JWT_ISSUER` | JWT issuer (defaults to `careconnect-api`) |
| `CARECONNECT_MAIL_USERNAME` | SMTP sender username |
| `CARECONNECT_MAIL_PASSWORD` | SMTP app password |
| `CARECONNECT_AUTH_EMAIL_VERIFICATION_REQUIRED` | Require verified email for login; defaults to `true` |
| `CARECONNECT_ADMIN_BOOTSTRAP_ENABLED` | Enable one-time administrator provisioning |
| `CARECONNECT_ADMIN_EMAIL` | Initial administrator email |
| `CARECONNECT_ADMIN_PASSWORD` | Initial administrator password |
| `VITE_API_BASE_URL` | Frontend API base URL |

JWTs are stored in browser session storage and sent to the API as bearer tokens. Keep production secrets outside source control and use HTTPS in deployed environments.

## Project structure

```text
backend/             Spring Boot API, tests and Flyway migrations
frontend/            React + Vite application
Frontend_Material/   Legacy static frontend assets retained during migration
```
