# CareConnect deployment

## Production architecture

- Frontend: Cloudflare Pages, built from `frontend/` using `npm run build`, output `dist`.
- Backend: Render Docker web service, Dockerfile at `backend/Dockerfile`.
- Database: Neon PostgreSQL.

## Render service

Create a Docker Web Service connected to this repository:

- Branch: `main`
- Root directory: leave blank (repository root)
- Dockerfile path: `backend/Dockerfile`
- Docker build context: repository root (the Dockerfile copies from `backend/`)
- Health check path: `/api/health`

Set the following environment variables in Render:

- `SPRING_DATASOURCE_URL`: Neon pooled JDBC URL (hostname contains `-pooler`, prefixed with `jdbc:`).
- `SPRING_FLYWAY_URL`: Neon direct JDBC URL (hostname without `-pooler`, prefixed with `jdbc:`).
- `DB_USERNAME`: Neon database role.
- `DB_PASSWORD`: Neon role password.
- `CARECONNECT_JWT_SECRET`: unique random secret (at least 32 bytes).
- `CARECONNECT_CORS_ALLOWED_ORIGINS`: exact Cloudflare Pages origin, e.g. `https://your-project.pages.dev`.
- `CARECONNECT_FRONTEND_VERIFICATION_URL`: `https://your-project.pages.dev/verify-email`.
- `CARECONNECT_AUTH_EMAIL_VERIFICATION_REQUIRED`: `true`.
- `CARECONNECT_ADMIN_BOOTSTRAP_ENABLED`: `false` unless temporarily provisioning the first administrator.
- `CARECONNECT_MAIL_USERNAME` and `CARECONNECT_MAIL_PASSWORD`: credentials for the configured mail provider.

Both Neon URLs must use TLS (`sslmode=require`). Keep the direct URL for Flyway migrations and the pooled URL for application traffic. Do not commit connection strings, passwords, or JWT secrets.

## Cloudflare Pages

Connect the GitHub repository and configure:

- Production branch: `main`
- Root directory: `frontend`
- Build command: `npm run build`
- Build output directory: `dist`
- Environment variable: `VITE_API_BASE_URL=https://YOUR-RENDER-SERVICE.onrender.com/api`

Set the same `VITE_API_BASE_URL` for the production environment and redeploy after changing it.

## Important production notes

- Render free web services sleep after 15 minutes without inbound traffic and can take about a minute to wake. A free instance is suitable for an initial deployment test, not a production service for a student counselling platform.
- Render free services cannot send outbound SMTP on ports 465 or 587. Use a mail provider with an HTTPS API integration or a hosting plan/provider that supports the required mail transport before relying on email verification and password recovery.
- Confirm Neon backups/retention and the selected plan before storing real student data.
- Keep administrator bootstrap disabled after the first admin account has been created.
- Test registration, verification, password reset, login, role access, appointment booking, counsellor availability, and chat before announcing the production URL.
