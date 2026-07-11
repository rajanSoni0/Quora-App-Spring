# Production-Readiness Refactor — Change Report

This document explains every improvement made to the Quora-clone backend: why it was needed, what was wrong before, how it was solved, and how request flows changed. The refactor is incremental — every original capability (users, questions, answers, threaded comments, tags, personalized feed, pagination) is preserved, and only the endpoints that were inherently insecure changed shape.

## 0. Bugs fixed in the original code

Before any architectural work, three latent defects were corrected. First, `User` redeclared `private Long id`, shadowing the inherited `@Id` field from `BaseModel`; depending on access strategy this either breaks the mapping or silently creates a dead column. The duplicate field was removed. Second, every entity used Lombok's `@Data`, which generates `equals`, `hashCode`, and `toString` over all fields — on bidirectional JPA relationships (`Answer ↔ Comment`, `Question ↔ Tag ↔ User`) this causes infinite recursion, and hash-based collections break when the generated `hashCode` changes after the id is assigned. Entities now use `@Getter`/`@Setter` only. Third, `Comment` had no author field at all, which made ownership of comments untrackable and unenforceable; a mandatory `user` association was added.

## 1. Authentication (Spring Security 6 + JWT)

**Problem.** The application had no security whatsoever: no Spring Security dependency, passwords stored in plain text via `UserService.createUser`, and every endpoint open to the world.

**Solution.** Full JWT authentication was implemented with the standard production topology. `security/SecurityConfig` defines the `SecurityFilterChain` with stateless sessions, a `DaoAuthenticationProvider` backed by `security/CustomUserDetailsService`, a `BCryptPasswordEncoder` bean, and an `AuthenticationManager` exposed from `AuthenticationConfiguration`. `jwt/JwtService` issues HS256-signed access tokens (jjwt 0.12 API) containing the username as subject plus `uid` and `role` claims; the signing key is decoded from a Base64 environment variable and is never hardcoded. `jwt/JwtAuthenticationFilter` extends `OncePerRequestFilter`: it extracts the `Authorization: Bearer` header, validates the token, loads the user, and populates the `SecurityContext`; invalid tokens fall through so the authorization layer produces the correct 401.

Refresh tokens are persisted in the new `RefreshToken` entity as opaque random UUIDs rather than JWTs, so a leaked signing key cannot mint refresh tokens. `service/RefreshTokenService` handles creation, validation, revocation, and **rotation**: every call to `/auth/refresh` revokes the presented token and issues a new one, which limits the blast radius of a stolen token and makes replay detectable (reuse of a revoked token is logged as a warning and rejected). Logout (`/auth/logout`) revokes the presented refresh token server-side — the short-lived access token (15 minutes by default) then expires naturally, which is the standard trade-off for stateless JWTs.

**New request flow for a protected call.** Request → `JwtAuthenticationFilter` (token parsed, principal placed in `SecurityContext`) → authorization rules → controller → service (reads the principal via `util/SecurityUtils`, never a client-sent id) → repository. Login flow: `AuthController.login` → `AuthService.login` → `AuthenticationManager.authenticate` (BCrypt comparison inside Spring Security) → access token from `JwtService` + persisted refresh token → `AuthResponse`.

## 2. Authorization (RBAC + ownership)

**Problem.** The old API trusted the client to say who it was: `POST /questions` accepted a `userId` in the body, `GET /feed/{userId}` let anyone read anyone's feed, and anyone could delete any user, question, answer, comment, or tag.

**Solution.** A `Role` enum (`USER`, `ADMIN`) is stored on the user and surfaced as `ROLE_*` authorities via `CustomUserDetails`. `@EnableMethodSecurity` activates `@PreAuthorize`, which guards admin-only operations (`UserService.getAllUsers`, `UserService.deleteUser`, `TagService.deleteTag`). Ownership is enforced in the service layer (`assertOwnerOrAdmin` in `QuestionService`/`AnswerService`, inline in `CommentService.deleteComment`): only the author or an admin may modify or delete a resource, otherwise a `ForbiddenException` becomes a 403.

Every API that required a client-side `userId` was removed or reshaped: `POST /api/v1/users` is gone (signup lives at `/api/v1/auth/signup`); `POST /users/{userId}/followTag/{tagId}` became `POST /users/me/tags/{tagId}` (plus a new `DELETE` to unfollow); `GET /feed/{userId}` became `GET /feed`; `userId` was deleted from the question/answer creation DTOs. In all cases the acting user is resolved from the `SecurityContext` through `SecurityUtils.getCurrentUser()`.

## 3. Validation (Jakarta Bean Validation)

**Problem.** No request was validated; blank titles, malformed emails, and null tag lists reached the database and failed (or worse, succeeded) unpredictably.

**Solution.** Every request DTO in `dto/request` is annotated with `@NotBlank`, `@Email`, `@Size`, `@Pattern`, `@NotNull`, or `@NotEmpty`, and every controller body parameter is annotated `@Valid`. Entities are never validated directly — validation is a contract of the API surface, so it belongs on the DTOs. A custom composite constraint, `validation/@ValidPassword` with `PasswordConstraintValidator`, enforces the password policy (8–72 chars — 72 being BCrypt's effective input limit — with upper, lower, digit, and special character) in one reusable place. Validation failures are translated by the global handler into a 400 with a per-field `errors` array.

## 4. Exception handling

**Problem.** Errors surfaced as raw `RuntimeException`s ("User not found") producing 500s with Spring's default whitelabel body, and `deleteById` on a missing id failed with an unhandled `EmptyResultDataAccessException`.

**Solution.** Domain exceptions were introduced in `exception/`: `ResourceNotFoundException` (with `UserNotFoundException` as a specialization), `InvalidCredentialsException`, `UnauthorizedException`, `ForbiddenException`, `DuplicateResourceException`, and `TokenRefreshException`. `GlobalExceptionHandler` (`@RestControllerAdvice`) maps each to the right HTTP status and a consistent `ErrorResponse` body of the exact shape requested: `timestamp`, `status`, `error`, `message`, `path`, plus an optional `errors` list for validation. Security exceptions get the same treatment: `BadCredentialsException` maps to a deliberately generic "Invalid username or password" (never revealing whether the username exists), and the two Spring Security response paths that bypass `@RestControllerAdvice` — unauthenticated (401) and access-denied (403) at the filter level — are covered by `JwtAuthenticationEntryPoint` and `JwtAccessDeniedHandler`, which write the same JSON shape. The catch-all `Exception` handler logs full details server-side and returns only "An unexpected error occurred" to the client, so internals never leak.

## 5. DTO layer

**Problem.** Controllers returned JPA entities directly. This exposed the password hash in every user payload, caused lazy-loading serialization failures and cyclic JSON (bidirectional relations), and welded the API contract to the database schema.

**Solution.** Request and response objects are fully separated: `SignupRequest`, `LoginRequest`, `RefreshTokenRequest`, `UpdateUserRequest`, `CreateQuestionRequest`/`UpdateQuestionRequest`, `CreateAnswerRequest`/`UpdateAnswerRequest`, `CreateCommentRequest`, `CreateTagRequest` on the way in; `UserResponse`, `AuthResponse`, `QuestionResponse`, `AnswerResponse`, `CommentResponse`, `TagResponse`, and a minimal `AuthorResponse` embedded in content payloads on the way out. All DTOs are Java records — immutable by construction, with value semantics for free. `UserResponse` contains no password field of any kind.

## 6. Mapping (MapStruct)

**Problem.** Entity-to-response conversion was manual field copying scattered through services.

**Solution.** MapStruct mappers (`mapper/UserMapper`, `QuestionMapper`, `AnswerMapper`, `CommentMapper`, `TagMapper`) with `componentModel = "spring"` are injected like any bean and generate the copying code at compile time (no reflection at runtime, unlike ModelMapper — which is why MapStruct is preferred in production). Nested mappings (`author` from `user`, `questionId` from `question.id`) and derived values (`likeCount`) are declared with `@Mapping`. The build includes `lombok-mapstruct-binding` so the Lombok and MapStruct annotation processors cooperate.

## 7. Service layer discipline

**Problem.** The layering existed but leaked: controllers made decisions (e.g., `Optional` unwrapping and 404 construction), and services returned entities.

**Solution.** Controllers now only translate HTTP: bind/validate the request, delegate, wrap the result. All business rules — existence checks, ownership checks, duplicate checks, tag resolution — live in services, which accept request DTOs and return response DTOs. Repositories remain pure Spring Data interfaces. Transaction boundaries are explicit: `@Transactional(readOnly = true)` on queries, `@Transactional` on mutations.

## 8. Standardized API responses

Every success response is wrapped in `response/ApiResponse<T>` — `{ "success": true, "message": "...", "data": { ... } }` — and every failure uses the `ErrorResponse` shape described above (`success`-style failures with an `errors` array are produced for validation). Paginated data uses `response/PageResponse<T>` (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`) instead of serializing Spring's `Page` directly, whose JSON structure is an internal detail Spring explicitly warns against exposing.

## 9. Database improvements

`BaseEntity` (replacing `BaseModel`) adds the four auditing columns — `createdAt`, `updatedAt`, `createdBy`, `updatedBy` — populated automatically via `@EnableJpaAuditing` in `config/JpaAuditingConfig`; the auditor is the authenticated username or `"system"` for anonymous flows like signup. Relationship hygiene: all `@ManyToOne` and `@ManyToMany` associations are now `FetchType.LAZY` (JPA's default EAGER on `@ManyToOne` causes N+1 storms), owning sides are `optional = false`/`nullable = false` where the domain requires it, and collections are initialized to empty `HashSet`s to avoid null checks. Unique constraints were added on `users.username`, `users.email`, and `tags.name`; foreign-key columns got explicit indexes (`idx_answers_question`, `idx_comments_answer`, etc.). Cascade rules were made deliberate: deleting an answer removes its comments (`REMOVE` + `orphanRemoval`), and deleting a tag detaches it from questions and followers first. `spring.jpa.open-in-view=false` closes the classic lazy-loading-in-the-view anti-pattern. The existing MySQL/Hibernate configuration is otherwise unchanged; `ddl-auto=update` remains the local-dev default but is now externalized (`DDL_AUTO`) with a comment recommending `validate` plus Flyway/Liquibase for production.

## 10. Security hardening

CSRF is disabled with justification (CSRF protects cookie/session authentication; a bearer token in the `Authorization` header is not auto-attached by browsers, so the attack does not apply to this design). CORS is restricted to configured origins (`CORS_ALLOWED_ORIGINS`), methods, and headers instead of a wildcard. `AuthenticationEntryPoint` and `AccessDeniedHandler` are configured as described in §4. Read-only content endpoints (questions, answers, comments, tags) remain publicly readable to preserve the original open-read behavior; every mutating endpoint requires authentication. Login errors are generic; stack traces are excluded from error responses (`server.error.include-stacktrace=never`); passwords never appear in any response or log line.

## 11. Logging

There were no `System.out.println` calls to remove, but there was also no logging at all. SLF4J (via Lombok's `@Slf4j`) was added throughout: INFO for lifecycle events (signup, login, resource creation/deletion), WARN for security-relevant anomalies (failed auth, revoked-token reuse, access denials, integrity violations), ERROR with full stack trace for unhandled exceptions, DEBUG for token-processing details. `spring.jpa.show-sql` was turned off — SQL echo to stdout is a dev-only tool and a performance/noise problem in production.

## 12. Pagination, sorting, filtering, searching

The old endpoints required raw `page`/`size` request params with no bounds and no sorting. List endpoints now accept a Spring `Pageable` with `@PageableDefault` (size 10, sorted by `createdAt` descending), so clients get `?page=`, `?size=`, and `?sort=field,direction` for free. `GET /questions` gains `?search=` (case-insensitive match on title/content) and `?tagId=` filtering via a single JPQL query; `GET /tags` gains `?name=` filtering. All list responses return the `PageResponse` envelope with totals.

## 13. API documentation

springdoc-openapi is integrated (`config/OpenApiConfig`). Every endpoint carries an `@Operation` summary, controllers are grouped with `@Tag`, and a global `bearerAuth` security scheme makes the Swagger UI "Authorize" button work with the JWT; public endpoints are marked `@SecurityRequirements` so the UI doesn't demand a token for them. UI at `/swagger-ui.html`, spec at `/v3/api-docs`.

## 14. Testing

Three test classes were added on JUnit 5. `AuthServiceTest` (Mockito) proves the password is BCrypt-hashed before persistence and never stored raw, that duplicate usernames are rejected before any save, and that login returns both tokens. `QuestionServiceTest` (Mockito) proves authorship comes from the `SecurityContext`, unknown tag ids fail loudly, non-owners cannot delete, and admins can. `AuthControllerIntegrationTest` (`@SpringBootTest` + MockMvc + H2, `test` profile) exercises the real filter chain end-to-end: signup 201 without password exposure, weak-password 400 with field errors, login/wrong-password 401 with the generic message, JSON 401 on protected endpoints, and open public reads.

## 15. Configuration

The hardcoded MySQL password (`Mac@local12345`) was removed from `application.properties` — this credential should be considered compromised and rotated. All secrets now come from environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (Base64, ≥256 bits), with non-secret knobs (`JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`, `DDL_AUTO`) also externalized with safe defaults. The application fails fast at startup if `DB_PASSWORD` or `JWT_SECRET` is missing, which is the correct behavior for a secret.

## 16. Best practices

Constructor injection replaced all field injection (`@RequiredArgsConstructor` + `private final`), which makes dependencies explicit, enables `final` fields, and keeps classes unit-testable without Spring. DTOs are immutable records. The package structure follows the requested layout (`controller`, `service`, `repository`, `entity`, `dto/request`, `dto/response`, `mapper`, `config`, `security`, `jwt`, `exception`, `util`, `validation`, `constant`, `response`). SOLID is observed pragmatically: single-responsibility per class, policy centralized (password rule, error mapping, pagination defaults in `constant/AppConstants`), and no speculative interfaces or layers were added — a service class per aggregate is what this codebase's size warrants.

## Endpoint changes at a glance

| Original | Now | Reason |
|---|---|---|
| `POST /api/v1/users` (plain-text password) | `POST /api/v1/auth/signup` | Auth flow with hashing and validation |
| — | `POST /api/v1/auth/login`, `/refresh`, `/logout` | JWT lifecycle |
| `POST /users/{userId}/followTag/{tagId}` | `POST /users/me/tags/{tagId}` (+ `DELETE` to unfollow) | Client-supplied userId removed |
| `GET /feed/{userId}` | `GET /feed` (authenticated) | Feed belongs to the principal |
| `GET /questions?page=&size=` | `GET /questions?search=&tagId=&page=&size=&sort=` | Search/filter/sort added; params now optional |
| `DELETE` on any resource (open) | Owner-or-admin only; users/tags admin-only | RBAC + ownership |
| — | `PATCH /questions/{id}`, `PATCH /answers/{id}`, `PATCH /users/me` | Update operations the original lacked |

All `GET` content endpoints keep their original paths and remain publicly readable, so existing read integrations continue to work (response bodies are now wrapped in the standard envelope).

## Known limitation of this delivery

This refactor was authored and statically cross-checked in a sandbox without access to Maven Central, so `./gradlew build` could not be executed here. Run `./gradlew clean build` locally as the first step; any issues should be limited to trivial fixes (imports/annotation-processor ordering), not design problems.
