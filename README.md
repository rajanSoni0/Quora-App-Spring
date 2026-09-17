# Quora Clone — Production-Ready Spring Boot Backend

A production-oriented Q&A platform backend built with **Spring Boot 3, Spring Security 6, JWT, JPA/Hibernate, MySQL, and MapStruct**.

The project implements secure authentication, role-based authorization, ownership checks, validated DTOs, refresh-token rotation, centralized exception handling, auditing, pagination, search, filtering, and OpenAPI documentation.

## 🚀 Key Features

* **JWT Authentication** with short-lived access tokens
* **Rotating refresh tokens** stored securely in the database
* **BCrypt password hashing**
* **Role-Based Access Control (RBAC)** with `USER` and `ADMIN` roles
* **Owner-or-admin authorization** for resource modification/deletion
* **Jakarta Bean Validation** with custom password validation
* **DTO-based API architecture** using immutable Java records
* **MapStruct** for compile-time entity/DTO mapping
* **Centralized exception handling** with consistent error responses
* **Global API response envelopes**
* **Pagination, sorting, search, and filtering**
* **JPA auditing** for creation/update tracking
* **Lazy-loaded relationships** to reduce unnecessary database queries
* **OpenAPI/Swagger documentation**
* **Unit and integration testing**
* **Environment-based configuration** for secrets and deployment settings
* **CORS configuration** for controlled cross-origin access
* **Structured logging** for application and security events

---

## 🏗️ Architecture

The application follows a layered architecture:

```text
Client
  │
  ▼
Controller
  │
  ▼
Service
  │
  ▼
Repository
  │
  ▼
MySQL
```

Security-related requests follow:

```text
HTTP Request
     │
     ▼
JwtAuthenticationFilter
     │
     ▼
SecurityContext
     │
     ▼
Authorization Rules
     │
     ▼
Controller
     │
     ▼
Service
     │
     ▼
Repository
```

Business logic remains in the service layer, while controllers are responsible primarily for HTTP request/response handling.

---

## 🔐 Authentication & Security

### Access Tokens

Access tokens are:

* Signed using **HS256**
* Short-lived (15 minutes by default)
* Sent using the `Authorization: Bearer <token>` header
* Never stored server-side

JWT claims include:

```text
sub  → username
uid  → user ID
role → USER / ADMIN
```

### Refresh Tokens

Refresh tokens are intentionally **not JWTs**.

Instead, the application:

1. Generates a cryptographically random opaque token.
2. Stores its SHA-256 hash in the database.
3. Returns the refresh token to the client.
4. Revokes the old token whenever `/refresh` is called.
5. Issues a new refresh token.

This provides refresh-token rotation and prevents a leaked JWT signing key from being used to generate refresh tokens.

### Password Security

Passwords are never stored in plain text.

```text
Raw Password
     │
     ▼
BCryptPasswordEncoder
     │
     ▼
Password Hash
     │
     ▼
Database
```

Password validation requires:

* 8–72 characters
* Uppercase character
* Lowercase character
* Digit
* Special character

---

## 👮 Authorization

The backend uses Spring Security's method-level authorization with:

```text
USER
ADMIN
```

### Ownership Rules

Users can modify or delete their own content.

Admins can manage protected resources across the application.

For example:

```text
Question
 ├── Author → can modify/delete
 └── Admin  → can modify/delete
```

Requests never trust a client-supplied `userId` to determine ownership.

The authenticated user is resolved from the Spring Security `SecurityContext`.

---

## 📦 DTO Architecture

Entities are never exposed directly through API responses.

### Request DTOs

Examples:

```text
SignupRequest
LoginRequest
RefreshTokenRequest
UpdateUserRequest
CreateQuestionRequest
UpdateQuestionRequest
CreateAnswerRequest
UpdateAnswerRequest
CreateCommentRequest
CreateTagRequest
```

### Response DTOs

Examples:

```text
UserResponse
AuthResponse
QuestionResponse
AnswerResponse
CommentResponse
TagResponse
AuthorResponse
```

This prevents sensitive fields such as password hashes from accidentally reaching API consumers and keeps the API contract independent of the database model.

---

## 🗺️ MapStruct

Entity-to-DTO conversion is handled using **MapStruct**.

Benefits include:

* Compile-time generated mapping code
* No runtime reflection
* Type-safe mappings
* Cleaner service classes
* Centralized conversion logic

Example mappings include nested relationships such as:

```text
Question.user → QuestionResponse.author
Answer.question.id → AnswerResponse.questionId
```

---

## ⚠️ Exception Handling

The application uses a centralized `@RestControllerAdvice`.

Domain-specific exceptions include:

```text
ResourceNotFoundException
UserNotFoundException
InvalidCredentialsException
UnauthorizedException
ForbiddenException
DuplicateResourceException
TokenRefreshException
```

### Error Response

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/questions",
  "errors": [
    {
      "field": "title",
      "message": "must not be blank"
    }
  ]
}
```

Authentication failures use generic messages so the API does not reveal whether a username exists.

---

## 📡 API Overview

### Authentication

| Method | Endpoint               | Description          |
| ------ | ---------------------- | -------------------- |
| `POST` | `/api/v1/auth/signup`  | Register a new user  |
| `POST` | `/api/v1/auth/login`   | Authenticate user    |
| `POST` | `/api/v1/auth/refresh` | Rotate refresh token |
| `POST` | `/api/v1/auth/logout`  | Revoke refresh token |

### Questions

```text
GET    /api/v1/questions
POST   /api/v1/questions
GET    /api/v1/questions/{id}
PATCH  /api/v1/questions/{id}
DELETE /api/v1/questions/{id}
```

Supported query parameters:

```text
?search=
?tagId=
?page=
?size=
?sort=
```

Example:

```text
GET /api/v1/questions?search=spring&tagId=2&page=0&size=10&sort=createdAt,desc
```

### Answers

```text
GET    /api/v1/answers
POST   /api/v1/answers
GET    /api/v1/answers/{id}
PATCH  /api/v1/answers/{id}
DELETE /api/v1/answers/{id}
```

### Comments

```text
GET    /api/v1/comments
POST   /api/v1/comments
DELETE /api/v1/comments/{id}
```

Comments support ownership-based deletion.

### Tags

```text
GET    /api/v1/tags
POST   /api/v1/tags
DELETE /api/v1/tags/{id}
```

Tag deletion requires administrator privileges.

### User

```text
GET   /api/v1/users/me
PATCH /api/v1/users/me

POST   /api/v1/users/me/tags/{tagId}
DELETE /api/v1/users/me/tags/{tagId}
```

### Feed

```text
GET /api/v1/feed
```

The feed is derived from the authenticated user rather than accepting a client-supplied user ID.

### Admin

```text
GET    /api/v1/users
DELETE /api/v1/users/{id}
DELETE /api/v1/tags/{id}
```

---

## 📄 API Response Format

Successful responses use a consistent envelope:

```json
{
  "success": true,
  "message": "Question created successfully",
  "data": {
    "id": 1,
    "title": "How does Spring Security work?"
  }
}
```

Paginated responses use:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

This avoids exposing Spring's internal `Page` serialization format directly.

---

## 🗄️ Database & JPA Improvements

The persistence layer uses **Spring Data JPA + Hibernate + MySQL**.

### Auditing

Entities automatically track:

```text
createdAt
updatedAt
createdBy
updatedBy
```

### Relationship Improvements

* `@ManyToOne` and `@ManyToMany` relationships use lazy loading.
* Required relationships are marked non-nullable.
* Collections are initialized to empty sets.
* Database indexes are added to frequently queried foreign keys.
* Unique constraints protect usernames, emails, and tag names.
* Cascade and orphan-removal behavior is explicitly defined.

### Open Session in View

```properties
spring.jpa.open-in-view=false
```

This prevents lazy-loading from leaking into the web/view layer.

---

## 🔎 Pagination, Search & Filtering

List endpoints support:

```text
page
size
sort
search
tagId
```

Default pagination:

```text
Page size: 10
Sort: createdAt DESC
```

Questions support case-insensitive search across title and content, along with tag filtering.

Tags support name-based filtering.

---

## 📚 API Documentation

OpenAPI documentation is integrated using **springdoc-openapi**.

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

The Swagger UI includes a Bearer authentication scheme so protected endpoints can be tested directly.

---

## 🧪 Testing

The project includes both unit and integration tests.

### Unit Tests

Mockito-based tests cover:

* Password hashing
* Duplicate username validation
* Authentication
* Question creation
* Security-context-based ownership
* Invalid tag handling
* Owner authorization
* Admin authorization

### Integration Tests

The security filter chain is tested end-to-end using:

```text
Spring Boot
MockMvc
H2
JUnit 5
```

Covered scenarios include:

* Successful signup
* Password validation
* Password non-exposure
* Successful login
* Invalid credentials
* Protected endpoint authentication
* Public content access

Run tests with:

```bash
./gradlew test
```

---

## ⚙️ Requirements

* **Java 19+**
* **MySQL 8**
* Gradle Wrapper

The project uses Java 19 as its configured toolchain.

---

## 🔧 Configuration

Secrets are externalized through environment variables.

```bash
export DB_URL="jdbc:mysql://localhost:3306/QUORA_DB_LOCAL"
export DB_USERNAME="root"
export DB_PASSWORD="your-db-password"

export JWT_SECRET="$(openssl rand -base64 48)"

export JWT_ACCESS_EXPIRATION_MS=900000
export JWT_REFRESH_EXPIRATION_MS=604800000

export CORS_ALLOWED_ORIGINS="http://localhost:3000"
```

Optional database configuration:

```bash
export DDL_AUTO="update"
```

For production deployments, schema validation should be combined with a migration tool such as Flyway or Liquibase.

The application fails fast when required secrets such as `DB_PASSWORD` or `JWT_SECRET` are missing.

---

## ▶️ Running Locally

Clone the repository:

```bash
git clone <your-repository-url>
cd quora-clone
```

Configure the required environment variables and create the MySQL database:

```sql
CREATE DATABASE QUORA_DB_LOCAL;
```

Start the application:

```bash
./gradlew bootRun
```

Run the test suite:

```bash
./gradlew test
```

Then open:

```text
http://localhost:8080/swagger-ui.html
```

---

## 👑 Promoting a User to ADMIN

The first administrator can be bootstrapped directly in the database:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE username = 'your-admin';
```

---

## 📁 Project Structure

```text
src/main/java/
└── ...
    ├── controller
    ├── service
    ├── repository
    ├── entity
    ├── dto
    │   ├── request
    │   └── response
    ├── mapper
    ├── config
    ├── security
    ├── jwt
    ├── exception
    ├── validation
    ├── util
    ├── constant
    └── response
```

The structure keeps authentication, validation, mapping, exception handling, HTTP concerns, and business logic separated.

---

## 🛡️ Production-Readiness Improvements

The project was refactored from a basic CRUD-style backend into a more production-oriented API.

Major improvements include:

```text
Plain-text passwords
        ↓
BCrypt hashing

Open endpoints
        ↓
JWT + RBAC + ownership

Client-supplied user IDs
        ↓
Authenticated SecurityContext

Raw entities
        ↓
Request/Response DTOs

Manual mapping
        ↓
MapStruct

Unstructured exceptions
        ↓
Centralized error handling

Raw Page responses
        ↓
Custom PageResponse

Hardcoded secrets
        ↓
Environment variables

No refresh-token lifecycle
        ↓
Persistent rotating refresh tokens

No auditing
        ↓
JPA auditing

Unbounded lists
        ↓
Pagination + sorting + filtering + search
```

A detailed breakdown of these changes is available in `IMPROVEMENTS.md`.

---

## 🧰 Tech Stack

**Backend**

* Java 19
* Spring Boot 3
* Spring Security 6
* Spring Data JPA
* Hibernate

**Authentication**

* JWT
* JJWT
* BCrypt
* Refresh-token rotation

**Database**

* MySQL 8
* H2 for integration testing

**Libraries & Tools**

* MapStruct
* Lombok
* Jakarta Bean Validation
* Springdoc OpenAPI
* Gradle
* JUnit 5
* Mockito
* MockMvc
* Git/GitHub

---

## 🎯 What This Project Demonstrates

* REST API design
* Spring Boot backend development
* Spring Security architecture
* JWT authentication
* Refresh-token security
* RBAC and resource ownership
* JPA/Hibernate relationship management
* DTO-based API design
* Exception handling
* Database indexing and constraints
* Pagination and search
* Unit and integration testing
* API documentation
* Secure configuration management
* Layered architecture and SOLID principles

---

## 📌 Project Status

The project retains the original Q&A functionality — users, questions, answers, threaded comments, tags, personalized feeds, pagination, and search — while adding security, validation, maintainability, and production-oriented backend practices.

> **Note:** The project was statically cross-checked during the refactor, but the complete Gradle build should be executed locally before deployment.
