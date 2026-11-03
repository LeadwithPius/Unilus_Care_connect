# UNILUS CareConnect

CareConnect is a Progressive Web App for UNILUS student wellbeing, backed by the Spring Boot API in `backend/`.

## Default frontend

The repository root is the frontend web root:

- `index.html` — landing page
- `login.html` — student, counsellor and administrator sign-in
- `verify-email.html` — email-verification link handler
- `reset-password.html` — password-reset link handler
- `Frontend_Material/` — student, counsellor and administrator portals, shared styles and scripts
- `manifest.json`, `sw.js`, `icon-192.png`, `icon-512.png` — PWA installation and offline shell

The former nested copy of this frontend has been moved to the repository root so there is one default frontend.

## Run locally

Start PostgreSQL and the backend first:

```bash
cd backend
export DB_PASSWORD='your-postgres-password'
export CARECONNECT_JWT_SECRET="$(openssl rand -hex 32)"
./mvnw spring-boot:run
```

In a second terminal, from the repository root:

```bash
npx --yes http-server . -p 5173 -c-1
```

Open `http://localhost:5173`.

The frontend API client automatically targets `http://localhost:8080/api` when served from localhost. For a different deployment, set `window.CARECONNECT_API_BASE_URL` before loading `Frontend_Material/Shared/api.js`, or configure a same-origin reverse proxy for `/api`.

## Backend-connected flows in this integration

- Login uses `POST /api/auth/login` and stores the returned JWT in session storage.
- Protected portal pages validate the JWT with `GET /api/auth/me` and enforce the selected STUDENT, COUNSELLOR or ADMIN role.
- Student registration uses `POST /api/students/register` with full name, student email, student number and password.
- Forgot password, reset password and email verification use the existing backend endpoints. The email links resolve to `reset-password.html` and `verify-email.html`.
- Administrator counsellor creation uses `POST /api/admin/counsellors`.

Student appointment workflows, counsellor scheduling/triage, emergency-session booking, education content loading and anonymised reports still need their individual screens connected to implemented backend endpoints. Some existing screens remain prototype UI and must not be treated as persisted backend records.

## Security notes

- No demo passwords or browser-side account records are used for authentication.
- JWTs are held in `sessionStorage`, sent as Bearer tokens and checked by the backend on protected-page loads.
- Do not commit database passwords, JWT secrets, SMTP credentials or real user credentials.
