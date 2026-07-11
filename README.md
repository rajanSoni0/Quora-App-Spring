# Quora Clone — Spring Boot 3 / Spring Security 6 Backend

A production-grade Q&A backend: JWT authentication with rotating refresh tokens, role-based access control, validated DTOs, centralized error handling, auditing, pagination/search, and OpenAPI docs.

See `IMPROVEMENTS.md` for the full change report explaining every refactor decision.

## Requirements

Java 19+ (toolchain pinned to 19), MySQL 8.

## Configuration

All secrets come from environment variables. The application will not start without `DB_PASSWORD` and `JWT_SECRET`.

```bash
export DB_URL="jdbc:mysql://localhost:3306/QUORA_DB_LOCAL"   # optional, this is the default
export DB_USERNAME="root"                                     # optional, default root
export DB_PASSWORD="your-db-password"                         # required
export JWT_SECRET="$(openssl rand -base64 48)"                # required, Base64, >= 256 bits
export JWT_ACCESS_EXPIRATION_MS=900000                        # optional, 15 min default
export JWT_REFRESH_EXPIRATION_MS=604800000                    # optional, 7 days default
export CORS_ALLOWED_ORIGINS="http://localhost:3000"           # optional, comma-separated
```

## Run

```bash
./gradlew bootRun
```

Swagger UI: `http://localhost:8080/swagger-ui.html` — use the Authorize button with the access token from `/api/v1/auth/login`.

## Tests

```bash
./gradlew test
```

Unit tests (Mockito) cover the auth and question services; integration tests run the full security filter chain against in-memory H2.

## API overview

Auth: `POST /api/v1/auth/signup`, `/login`, `/refresh` (rotates the refresh token), `/logout` (revokes it).

Content (reads are public, writes require a Bearer token; modify/delete is owner-or-admin):
`/api/v1/questions` (supports `?search=`, `?tagId=`, `?page=`, `?size=`, `?sort=`), `/api/v1/answers`, `/api/v1/comments`, `/api/v1/tags`.

Personal: `GET /api/v1/users/me`, `PATCH /api/v1/users/me`, `POST|DELETE /api/v1/users/me/tags/{tagId}`, `GET /api/v1/feed`.

Admin-only: `GET /api/v1/users`, `DELETE /api/v1/users/{id}`, `DELETE /api/v1/tags/{id}`.

All responses use the envelope `{ "success", "message", "data" }`; errors return `{ "timestamp", "status", "error", "message", "path", "errors" }`.

## Promoting a user to ADMIN

Roles are stored in the `users.role` column. Bootstrap the first admin directly:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'your-admin';
```
