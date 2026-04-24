# Social Login — GitHub Issues

**Spec:** `docs/specs/2026-04-20-001-social-login-spec.md`
**Total:** 10 issues, ~25–30 files, ~900–1200 LOC including tests

---

## Issue 1: Schema + user/auth domain skeleton

**Labels:** `backend`, `auth`, `database`
**Size:** M
**Depends on:** —

### Business context

StablePay currently has no concept of a "user" — wallets and remittances use arbitrary string IDs passed from the client. Before we can authenticate anyone, we need a `users` table as the identity anchor and FK target for wallets and remittances.

### Description

Create the Flyway migration `V8__users_and_auth.sql` and the full `com.stablepay.domain.auth` skeleton (models, ports, exceptions). Also create `com.stablepay.infrastructure.db.user` with JPA entities, Spring Data repos, and repository adapters.

**Migration (`V8__users_and_auth.sql`):**
- `users(id UUID PK, email, created_at, updated_at)`
- `social_identities(id UUID PK, user_id FK, provider, subject, email, email_verified, created_at)` with `UNIQUE(provider, subject)`
- `refresh_tokens(id UUID PK, user_id FK, token_hash, issued_at, expires_at, revoked_at)` with unique hash and partial index on non-revoked tokens
- Migrate `wallets.user_id` from `VARCHAR(255)` to `UUID NOT NULL REFERENCES users(id)` — uses `TRUNCATE wallets CASCADE` (acceptable for devnet, also empties remittances/claim_tokens/funding_orders via FK chain)
- Add `UNIQUE(user_id)` constraint on `wallets` — one wallet per user, prevents duplicates from dev-gated `POST /api/wallets`
- Migrate `remittances.sender_id` from `VARCHAR(255)` to `UUID NOT NULL REFERENCES users(id)` — same truncate approach
- Add indexes: `idx_wallet_user` (unique), `idx_remittance_sender`, `idx_refresh_user`, `idx_refresh_hash`

**Domain models (`com.stablepay.domain.auth.model/`):**
- `AppUser(UUID id, String email, Instant createdAt)` — Java record + `@Builder(toBuilder = true)`
- `SocialIdentity(String provider, String subject, String email, boolean emailVerified)` — record
- `AuthSession(String accessToken, String refreshToken, Instant accessExpiresAt)` — record
- `RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant revokedAt)` — record

**Domain ports (`com.stablepay.domain.auth.port/`):**
- `UserRepository` — `findById(UUID)`, `save(AppUser)`
- `SocialIdentityRepository` — `findByProviderAndSubject(String, String)`, `save(SocialIdentity, UUID userId)`
- `RefreshTokenRepository` — `findByHash(String)`, `save(RefreshToken)`, `revokeByUserId(UUID)`

**Domain exceptions (`com.stablepay.domain.auth.exception/`):**
- `InvalidIdTokenException` (SP-0032)
- `EmailNotVerifiedException` (SP-0033)
- `UnsupportedAuthProviderException` (SP-0034)
- `InvalidRefreshTokenException` (SP-0035)
- `RefreshTokenExpiredException` (SP-0036)
- All use static factory methods per coding standards

**Infrastructure (`com.stablepay.infrastructure.db.user/`):**
- `UserEntity`, `SocialIdentityEntity`, `RefreshTokenEntity` — JPA entities with Lombok
- `UserJpaRepository`, `SocialIdentityJpaRepository`, `RefreshTokenJpaRepository` — Spring Data
- `UserRepositoryAdapter`, `SocialIdentityRepositoryAdapter`, `RefreshTokenRepositoryAdapter` — implement domain ports
- `UserMapper` — MapStruct `@Mapper(componentModel = "spring")` for entity ↔ domain mapping

### Acceptance criteria

- [ ] `./gradlew build` passes — migration applies cleanly against TestContainers PostgreSQL
- [ ] All 5 domain models are Java records with `@Builder(toBuilder = true)`, zero Spring imports (ADR-004, ADR-020)
- [ ] All 5 exception classes use `SP-XXXX` error codes and static factory methods (e.g., `InvalidIdTokenException.of(String detail)`)
- [ ] Domain ports are plain interfaces in `domain/auth/port/` — no Spring annotations
- [ ] Infrastructure entities use `@Entity`, `@Table`, `@Builder(toBuilder = true)`, `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PRIVATE)`, `@Getter`
- [ ] Infrastructure entities, JPA repos, adapters, and MapStruct mappers live under `infrastructure/db/user/` (per-subdomain DB pattern)
- [ ] Entity ↔ domain mapping uses MapStruct `@Mapper(componentModel = "spring")` with standard method names (`toDomain`, `toEntity`) — no manual field copying (ADR-017)
- [ ] `@RequiredArgsConstructor` + `private final` for DI — no `@Autowired` (ADR-020)
- [ ] ArchUnit tests pass — domain imports no infrastructure or application code (ADR-002)
- [ ] Unit tests for repository adapters: `// given // when // then` markers, golden rule (build expected object + single `usingRecursiveComparison`), BDDMockito `given()`/`then()`, no generic matchers (`any()`, `anyString()` forbidden), `@Spy` for MapStruct mappers
- [ ] Existing tests still pass after migration (existing data is truncated, test fixtures use TestContainers with fresh schema each run)

### References

- Spec: `docs/specs/2026-04-20-001-social-login-spec.md` § Flyway migration, Domain layout, Error codes
- [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) § 1 (Hexagonal), § 2 (Domain Layer), § 4.1 (JPA), § 5 (MapStruct)
- [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) § Golden Rule, § 3 (Given/When/Then), § 4 (Mocking)
- ADR-002 (Hexagonal), ADR-004 (Immutable Records), ADR-017 (MapStruct), ADR-020 (Conventions), ADR-022 (Auth Strategy)

---

## Issue 2: JWT machinery + Spring Security dependency

**Labels:** `backend`, `auth`, `security`
**Size:** M
**Depends on:** #1

### Business context

StablePay needs to issue its own short-lived JWTs after verifying a Google login, and validate those JWTs on every authenticated API call. This issue adds the token infrastructure — signing, issuing, and verifying — without wiring it into the filter chain yet.

### Description

Add `spring-boot-starter-oauth2-resource-server` dependency (transitively pulls Spring Security + nimbus-jose-jwt). Implement the JWT issuance and refresh-token generation adapters.

**Dependency (`build.gradle.kts`):**
```kotlin
implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
```

**App config (`com.stablepay.application.config/`):**
- `JwtConfig` — two `JwtDecoder` beans:
  1. `appJwtDecoder`: `NimbusJwtDecoder.withSecretKey(HMAC)` for verifying app-issued JWTs (wired into filter chain in issue #6)
  2. `googleJwtDecoder`: `NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")` + issuer + audience validators (used manually by `SocialLoginHandler`, NOT in filter chain)
- `GoogleAuthProps` — `@ConfigurationProperties("stablepay.auth.google")` with `List<String> clientIds`
- `AppUserConverter` — `Converter<Jwt, AbstractAuthenticationToken>` that produces `AppUser(UUID.fromString(jwt.getSubject()))`

**Domain port (`com.stablepay.domain.auth.port/`):**
- `AuthTokenIssuer` — `issue(UUID userId) → AuthSession`
- `SocialIdentityVerifier` — `verify(String provider, String idToken) → SocialIdentity` (interface only — implementation in issue #3)

**Infrastructure (`com.stablepay.infrastructure.auth/jwt/`):**
- `JwtTokenIssuerAdapter` implements `AuthTokenIssuer` — signs HS256 JWT with `sub=userId`, `iat`, `exp` (15 min TTL). Generates refresh token via `RefreshTokenGenerator`.
- `RefreshTokenGenerator` — generates 32-byte random opaque token, prefixed `r1_`, SHA-256 hashes for storage

**Config (`application.yml`):**
```yaml
stablepay:
  jwt:
    secret: ${JWT_SECRET}
    access-ttl: PT15M
    refresh-ttl: P30D
  auth:
    google:
      client-ids:
        - ${GOOGLE_ANDROID_CLIENT_ID}
```

**`AppUser` principal:**
- `record AppUser(UUID id)` in `com.stablepay.domain.auth.model/`
- Produced by `AppUserConverter` from JWT `sub` claim

### Acceptance criteria

- [ ] `./gradlew build` passes — Spring Security autoconfiguration does not break existing tests (may need `@AutoConfigureMockMvc` or temporary `SecurityConfig` with `permitAll()`)
- [ ] `JwtTokenIssuerAdapter` issues a valid HS256 JWT that `appJwtDecoder` can verify
- [ ] JWT contains exactly `sub` (user UUID), `iat`, `exp` — no email, name, roles, or wallet claims
- [ ] `RefreshTokenGenerator` produces URL-safe base64 tokens prefixed `r1_`, SHA-256 hash is 64 hex chars
- [ ] `googleJwtDecoder` validates issuer (`accounts.google.com`) and audience (from config)
- [ ] `AppUserConverter` returns `JwtAuthenticationToken` (extends `AbstractAuthenticationToken`) with `AppUser(UUID)` as principal — controller extracts via `@AuthenticationPrincipal AppUser`
- [ ] Config binds via `@ConfigurationProperties` — no hardcoded values
- [ ] Unit tests: `JwtTokenIssuerAdapterTest` (round-trip sign → decode), `RefreshTokenGeneratorTest` (uniqueness, hash consistency), `AppUserConverterTest` — all with `// given // when // then` markers, golden rule (build expected → single `usingRecursiveComparison`), BDDMockito `given()`/`then()`, no generic matchers (`any()`, `anyString()` forbidden)
- [ ] 80% line / 70% branch coverage on new code

### References

- Spec: § Decisions #4 (HS256), #8 (Google verification), #9 (filter chain), #14 (JWT claims), #15 (AppUser)
- [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) § 3.1 (Controllers), § 6.3 (DI)
- [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) § Golden Rule, § 3 (Given/When/Then), § 4 (Mocking)
- ADR-002 (Hexagonal), ADR-020 (Conventions), ADR-022 (Auth Strategy — HS256, JWT claims, AppUser principal)

---

## Issue 3: Google ID token verifier adapter

**Labels:** `backend`, `auth`
**Size:** S
**Depends on:** —

### Business context

The mobile app sends a Google `idToken` obtained from `@react-native-google-signin/google-signin`. The backend must verify this token against Google's JWKS endpoint before trusting the identity claims (email, subject, email_verified).

### Description

Implement `GoogleIdTokenVerifierAdapter` in `com.stablepay.infrastructure.auth.google/` that implements the `SocialIdentityVerifier` domain port.

**Implementation:**
- Receives `(provider, idToken)` — throws `UnsupportedAuthProviderException` (SP-0034) if provider is not `"google"`
- Decodes `idToken` using the `googleJwtDecoder` bean (from issue #2)
- Validates: issuer is `accounts.google.com` or `https://accounts.google.com`, audience matches configured client ID
- Extracts: `sub`, `email`, `email_verified` from JWT claims
- Throws `InvalidIdTokenException` (SP-0032) if token is invalid/expired
- Throws `EmailNotVerifiedException` (SP-0033) if `email_verified` is false
- Returns `SocialIdentity(provider, subject, email, emailVerified)`

### Acceptance criteria

- [ ] `GoogleIdTokenVerifierAdapter` implements `SocialIdentityVerifier` port
- [ ] Rejects non-`"google"` providers with SP-0034
- [ ] Rejects invalid/expired tokens with SP-0032
- [ ] Rejects `email_verified=false` with SP-0033
- [ ] Extracts `sub` (not email) as the identity key
- [ ] `@Component` with `@RequiredArgsConstructor` — no `@Autowired`
- [ ] Unit test with mocked `JwtDecoder`: happy path, invalid token, unverified email, wrong provider — all with `// given // when // then` markers, golden rule (build expected → single `usingRecursiveComparison`), BDDMockito `given()`/`then()`, no generic matchers (`any()`, `anyString()` forbidden)
- [ ] 80% line / 70% branch coverage on new code

### References

- Spec: § Decision #8, § Domain layout `infrastructure/auth/google/`
- [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) § 4.3 (External Integrations)
- [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) § Golden Rule, § 3 (Given/When/Then), § 4 (Mocking)
- ADR-022 (Auth Strategy — Google idToken verification via NimbusJwtDecoder, not filter chain)

---

## Issue 4: Social login, refresh, and logout handlers

**Labels:** `backend`, `auth`
**Size:** M
**Depends on:** #1, #2, #3

### Business context

These are the three core auth operations: sign in (which auto-provisions an MPC wallet on first login), rotate a refresh token, and log out. Together they form the complete auth lifecycle that the mobile app drives.

### Description

Implement three domain handlers in `com.stablepay.domain.auth.handler/`:

**`SocialLoginHandler`:**
- Calls `SocialIdentityVerifier.verify(provider, idToken)` → `SocialIdentity`
- Looks up `SocialIdentityRepository.findByProviderAndSubject(provider, subject)`
- **New user (first login):** creates `AppUser`, saves `SocialIdentity`, calls `CreateWalletHandler.handle(userId)` **synchronously inline** (direct method call, NOT via Temporal workflow — ADR-022). MPC failure rolls back the entire transaction — no partial user (all within `@Transactional`).
- **Returning user:** loads existing `AppUser` and wallet
- Issues tokens via `AuthTokenIssuer.issue(userId)`
- Saves refresh token hash via `RefreshTokenRepository.save()`
- Returns `LoginResult` domain record: `record LoginResult(AuthSession session, AppUser user, Wallet wallet, boolean newUser)` — controller maps to HTTP 201/200 based on `newUser` flag

**`RefreshTokenHandler`:**
- Looks up refresh token by SHA-256 hash of the provided opaque token
- Validates: not revoked, not expired
- Revokes old token, issues new token pair (rotation — single active chain per user)
- Throws `InvalidRefreshTokenException` (SP-0035) or `RefreshTokenExpiredException` (SP-0036) on failure

**`LogoutHandler`:**
- Revokes all refresh tokens for the authenticated user
- Access token remains valid until natural expiry (≤15 min) — no JWT denylist

### Acceptance criteria

- [ ] `SocialLoginHandler` creates user + wallet atomically in a single `@Transactional` — MPC failure rolls back everything
- [ ] `CreateWalletHandler` is called synchronously inline (direct method call, NOT via Temporal workflow) per ADR-022
- [ ] Handler returns `LoginResult` domain record with `boolean newUser` flag — controller maps to 201/200
- [ ] First login returns wallet from `CreateWalletHandler`; returning login loads existing wallet via `WalletRepository.findByUserId()`
- [ ] Refresh token rotation: old token revoked, new token issued, single chain per user
- [ ] `RefreshTokenHandler` rejects revoked tokens (SP-0035) and expired tokens (SP-0036)
- [ ] `LogoutHandler` revokes all refresh tokens for the user
- [ ] All handlers: `@Service`, `@RequiredArgsConstructor`, `@Transactional` — no `@Autowired`
- [ ] Unit tests: `SocialLoginHandlerTest` (new user, returning user, email not verified, MPC failure rollback), `RefreshTokenHandlerTest` (happy, revoked, expired), `LogoutHandlerTest` — all with `// given // when // then` markers, golden rule (build expected → single `usingRecursiveComparison`), BDDMockito `given()`/`then()`, no generic matchers (`any()`, `anyString()` forbidden)
- [ ] 80% line / 70% branch coverage on new code

### References

- Spec: § Decisions #5 (refresh), #6 (eager wallet), #18 (logout)
- [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) § 2.3 (Domain Services)
- [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) § Golden Rule, § 3 (Given/When/Then), § 4 (Mocking)
- ADR-022 (Auth Strategy — eager wallet provisioning, refresh rotation, logout semantics)

---

## Issue 5: Auth controller + DTOs + MapStruct mapper

**Labels:** `backend`, `auth`, `api`
**Size:** S
**Depends on:** #4

### Business context

The mobile app needs three REST endpoints to complete the auth flow: exchange a Google token for app tokens, rotate a refresh token, and log out. These are the only auth-related HTTP endpoints.

### Description

Create `AuthController` in `com.stablepay.application.controller.auth/` with co-located MapStruct mapper. Create DTOs in `com.stablepay.application.dto/`.

**Controller endpoints:**
- `POST /api/auth/social` — public. Request: `SocialLoginRequest(String provider, String idToken)`. Response: `AuthResponse` (201 new user, 200 returning).
- `POST /api/auth/refresh` — public. Request: `RefreshTokenRequest(String refreshToken)`. Response: `AuthResponse` (200, tokens only — no user/wallet).
- `POST /api/auth/logout` — authenticated. No body. Response: 204 No Content.

**DTOs (`com.stablepay.application.dto/`):**
- `SocialLoginRequest` — `@NotBlank provider`, `@NotBlank idToken`
- `RefreshTokenRequest` — `@NotBlank refreshToken`
- `AuthResponse` — `String accessToken`, `String refreshToken`, `String tokenType`, `int expiresIn`, `UserResponse user` (nullable for refresh), `WalletResponse wallet` (nullable for refresh)
- `UserResponse` — `UUID id`, `String email`, `Instant createdAt`

All DTOs are Java records with `@Builder(toBuilder = true)`.

**MapStruct mapper (`mapper/AuthResponseMapper.java`):**
- `@Mapper(componentModel = "spring")`
- Maps domain `AuthSession` + `AppUser` + `Wallet` → `AuthResponse`

**OpenAPI annotations:** `@Tag`, `@Operation`, `@ApiResponses` on all endpoints.

### Acceptance criteria

- [ ] Three endpoints with correct HTTP methods, paths, and status codes
- [ ] Request validation: `@Valid` + `@NotBlank` on request fields
- [ ] `POST /api/auth/social` returns 201 for new users, 200 for returning
- [ ] `POST /api/auth/logout` returns 204 with no body
- [ ] All DTOs are records with `@Builder(toBuilder = true)` in `application/dto/`
- [ ] MapStruct mapper — no manual field mapping
- [ ] Controller is thin — delegates to domain handlers, no business logic
- [ ] OpenAPI annotations present — Swagger UI shows all three endpoints
- [ ] Error responses follow `ErrorResponse` shape (existing `GlobalExceptionHandler` maps new exceptions)
- [ ] Add exception mappings to `GlobalExceptionHandler`: SP-0032 → 401, SP-0033 → 401, SP-0034 → 400, SP-0035 → 401, SP-0036 → 401
- [ ] Integration test with MockMvc (`@SpringBootTest`, `@ActiveProfiles("test")`, TestContainers, WireMock for Google JWKS endpoint): valid social login → 201 (new user) / 200 (returning), invalid token → 401, refresh → 200, logout → 204 — `// given // when // then` markers, BDDMockito `given()`/`then()`, golden rule assertions, no generic matchers
- [ ] 80% line / 70% branch coverage on new code

### References

- Spec: § API section, § Domain layout `application/`
- [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) § 3.1 (Controllers), § 5 (MapStruct)
- [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) § Golden Rule, § 3 (Given/When/Then), § 4 (Mocking), § 7 (Integration Test Setup)
- ADR-018 (Testing Strategy — two-tier: unit + integration with TestContainers)

---

## Issue 6: SecurityConfig — filter chain + public routes

**Labels:** `backend`, `auth`, `security`
**Size:** M
**Depends on:** #2

### Business context

Currently all StablePay endpoints are wide open. This issue wires Spring Security into the filter chain so that every `/api/*` call requires a valid Bearer JWT — except the handful of endpoints that must remain public (auth, claims, webhooks, health, docs).

### Description

Create `SecurityConfig` in `com.stablepay.application.config/`. Wire the `appJwtDecoder` (HS256) into the OAuth2 Resource Server filter chain.

**`SecurityConfig.java`:**
- `@Configuration` + `@EnableWebSecurity`
- `SecurityFilterChain` bean with `HttpSecurity`:
  - CSRF disabled (stateless JWT API)
  - Session management: `STATELESS`
  - Public (permitAll):
    - `POST /api/auth/social`
    - `POST /api/auth/refresh`
    - `GET /api/claims/{token}`
    - `POST /api/claims/{token}`
    - `POST /webhooks/stripe`
    - `GET /actuator/health`
    - `OPTIONS /**`
    - `POST /api/wallets` (conditionally, dev-profile only — full gating in issue #10)
    - `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` (dev/sandbox only — controlled by existing `springdoc.api-docs.enabled` config, set `false` in prod)
  - Everything else: `authenticated()`
  - OAuth2 Resource Server: `jwt()` with `appJwtDecoder` + `AppUserConverter`
  - Exception handling: 401 JSON error body (not Spring's default HTML), matching `ErrorResponse` shape

**Audit logging (in domain handlers, not controllers):**
- `@Slf4j` structured logs in `SocialLoginHandler`, `RefreshTokenHandler`, `LogoutHandler` for `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `REFRESH`, `LOGOUT` with `{userId, ip, userAgent, timestamp}` (spec decision #19)
- IP and userAgent are passed from controller to handler as parameters — handlers do the logging, controllers stay thin

### Acceptance criteria

- [ ] All endpoints in the public list return 200/201 without a Bearer token
- [ ] All other endpoints return 401 with `ErrorResponse` JSON body when no Bearer is provided
- [ ] Valid Bearer token on authenticated endpoints → request proceeds, `@AuthenticationPrincipal AppUser` is populated
- [ ] Invalid/expired Bearer → 401 JSON (not Spring default HTML)
- [ ] CSRF disabled, sessions stateless
- [ ] `StripeWebhookController` comment about permit-all is now enforced by config (not just a comment)
- [ ] Swagger UI accessible without auth in dev profile
- [ ] `./gradlew build` passes — existing tests may need `@WithMockUser` or test security config adjustments (resolved in issue #8)
- [ ] Integration test (`@SpringBootTest`, `@ActiveProfiles("test")`, MockMvc): unauthenticated `GET /api/fx/USD-INR` → 401; authenticated with `SecurityTestBase.asUser()` → 200 — `// given // when // then` markers, BDDMockito, golden rule, no generic matchers
- [ ] 80% line / 70% branch coverage on new code

### References

- Spec: § Decisions #9, #10, #11, #19
- [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) § 3.1 (Controllers)
- [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) § Golden Rule, § 3 (Given/When/Then), § 7 (Integration Test Setup)
- ADR-002 (Hexagonal — config in application layer), ADR-022 (Auth Strategy — filter chain, public routes, 401 JSON)

---

## Issue 7: Migrate existing code to auth + add `GET /api/wallets/me`

**Labels:** `backend`, `auth`, `breaking-change`
**Size:** L
**Depends on:** #6

### Business context

With auth wired in (issue #6), we need to make all existing domain code auth-aware: String user IDs become UUID FKs, controllers stop accepting `senderId`/`userId` in request bodies, and ownership checks prevent users from accessing each other's resources. We also need a new `GET /api/wallets/me` endpoint — the FE currently has no way to refresh the wallet balance after initial login.

### Description

This is the largest ticket. It cascades the `String → UUID` type change through every layer and adds ownership verification.

**Domain model changes:**
- `Wallet.java`: `String userId` → `UUID userId`
- `Remittance.java`: `String senderId` → `UUID senderId`

**Domain port changes:**
- `WalletRepository`: `findByUserId(String)` → `findByUserId(UUID)`
- `RemittanceRepository`: `findBySenderId(String, Pageable)` → `findBySenderId(UUID, Pageable)`

**Domain handler changes:**
- `CreateWalletHandler.handle(String)` → `handle(UUID)`
- `CreateRemittanceHandler`: `senderId` param `String` → `UUID`
- `ListRemittancesQueryHandler.handle(String, Pageable)` → `handle(UUID, Pageable)`
- **New:** `GetWalletQueryHandler.handle(UUID userId)` → `WalletRepository.findByUserId(UUID)` — returns the user's wallet

**Infrastructure entity changes:**
- `WalletEntity.java`: `private String userId` → `private UUID userId`
- `RemittanceEntity.java`: `private String senderId` → `private UUID senderId`
- All JPA repos, adapters, and entity mappers update param types accordingly

**Controller changes:**
- `WalletController`: add `GET /api/wallets/me` using `@AuthenticationPrincipal AppUser` + `GetWalletQueryHandler`. Returns `WalletResponse` (200) or 404.
- `RemittanceController`:
  - `POST /api/remittances`: remove `senderId` from body, read from `@AuthenticationPrincipal`
  - `GET /api/remittances`: remove `@RequestParam senderId`, use authenticated user's UUID
  - `GET /api/remittances/{remittanceId}`: add ownership check — verify `remittance.senderId == authenticatedUser.id`, return 404 on mismatch (not 403, to prevent enumeration)
- `FundingController`:
  - `POST /api/wallets/{id}/fund`: pass authenticated user UUID to handler; handler verifies wallet belongs to user (ownership checks live in domain handlers, not controllers — hexagonal rule)
  - `GET /api/funding-orders/{fundingId}`: handler verifies funding order's wallet belongs to authenticated user
  - `POST /api/funding-orders/{fundingId}/refund`: same ownership check in handler

**DTO changes:**
- `CreateRemittanceRequest`: remove `senderId` field (body becomes `{ recipientPhone, amountUsdc }`)
- `WalletResponse`: remove `userId` field
- `RemittanceResponse`: remove `senderId` field
- `ClaimResponse`: replace `String senderId` with `String senderDisplayName` (email local part before `@`)

**Mapper changes:**
- `ClaimApiMapper`: receives pre-fetched `senderDisplayName` from the claim query handler — **no repository call in the MapStruct mapper** (MapStruct mappers must be stateless, no injected repositories). The `GetClaimQueryHandler` loads the sender email via `UserRepository`, extracts the local part (before `@`), and passes it through the domain model or as a separate parameter.

### Acceptance criteria

- [ ] `GET /api/wallets/me` returns the authenticated user's wallet (200) or 404
- [ ] `GetWalletQueryHandler` uses a plain `findByUserId(UUID)` query — **no pessimistic locking** (`@Lock(PESSIMISTIC_WRITE)` is only for balance-mutation paths per ADR-021)
- [ ] Ownership checks live in domain handlers (not controllers) — controllers pass authenticated user UUID to handlers, handlers verify and return 404 on mismatch
- [ ] `POST /api/remittances` request body has only `recipientPhone` + `amountUsdc` — no `senderId`
- [ ] `GET /api/remittances` returns only the authenticated user's remittances — no query param needed
- [ ] `GET /api/remittances/{id}` returns 404 (not 403) if the remittance belongs to another user
- [ ] `POST /api/wallets/{id}/fund` returns 404 if wallet doesn't belong to the caller
- [ ] `GET /api/funding-orders/{fundingId}` and `POST .../refund` enforce wallet ownership transitively
- [ ] `WalletResponse` no longer contains `userId`
- [ ] `RemittanceResponse` no longer contains `senderId`
- [ ] `ClaimResponse` contains `senderDisplayName` (e.g. "priya") instead of UUID `senderId` — derived in `GetClaimQueryHandler`, not in MapStruct mapper
- [ ] All domain models, ports, handlers, entities, JPA repos, adapters compile with UUID types
- [ ] `./gradlew spotlessApply` passes
- [ ] No `any()` matchers or `@Autowired` introduced
- [ ] Unit tests for `GetWalletQueryHandler` and ownership-check handlers: `// given // when // then` markers, golden rule, BDDMockito, no generic matchers
- [ ] 80% line / 70% branch coverage on new code

### References

- Spec: § Decisions #12 (ownership), #13 (DTO migration), § New+changed endpoints, § Cascading type changes table
- [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) § 1 (Hexagonal), § 2.1 (Domain Models), § 5 (MapStruct)
- [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) § Golden Rule, § 3 (Given/When/Then), § 4 (Mocking)
- ADR-021 (Pessimistic Locking — only for balance mutations, not read-only queries), ADR-022 (Auth Strategy — ownership returns 404)
- Mockup: `docs/mockups/complete-flow-v2.html` screens A4, C2, C5

---

## Issue 8: Test helper `SecurityTestBase` + update all failing tests

**Labels:** `backend`, `auth`, `testing`
**Size:** L
**Depends on:** #6, #7

### Business context

Issues #6 and #7 break most existing tests: controllers now require Bearer tokens, handlers expect UUID user IDs, and request DTOs have changed. This issue creates a shared test helper and updates every broken test.

### Description

**Test helper (`src/testFixtures/java/com/stablepay/testutil/SecurityTestBase.java`):**
- `asUser(UUID userId)` — signs a real HS256 JWT with a test-profile secret, returns a `RequestPostProcessor` (or similar) for `MockMvc`
- Test `application-test.yml` sets a fixed `JWT_SECRET` so tests can sign tokens deterministically
- Optionally provides a `SecurityTestConfig` that configures the test security filter chain

**Test fixture updates:**
- `WalletFixtures`: update `userId` from String to UUID
- `RemittanceFixtures`: update `senderId` from String to UUID
- Add `AuthFixtures` with `SOME_USER_ID`, `SOME_EMAIL`, etc.

**Unit test updates (all handlers):**
- `CreateWalletHandlerTest`: pass UUID instead of String
- `CreateRemittanceHandlerTest`: pass UUID senderId
- `ListRemittancesQueryHandlerTest`: pass UUID senderId
- All other handler tests: verify they still pass with the type changes

**Controller test updates:**
- All `MockMvc` tests must include Bearer token via `SecurityTestBase.asUser()`
- `RemittanceControllerTest`: remove `senderId` from request body and query params
- `WalletControllerTest`: add test for `GET /api/wallets/me`
- `FundingControllerTest`: add ownership check test (wrong user → 404)
- `ClaimControllerTest`: verify `senderDisplayName` in response

**Authorization tests:**
- `@ParameterizedTest` across every authenticated endpoint: no Bearer → 401
- Ownership tests: user A's Bearer on user B's resource → 404

### Acceptance criteria

- [ ] `./gradlew build` passes — zero test failures
- [ ] `./gradlew test` (unit tests) passes
- [ ] `./gradlew integrationTest` passes
- [ ] `SecurityTestBase.asUser(UUID)` signs a valid JWT accepted by the test filter chain
- [ ] Every authenticated endpoint has a test verifying 401 without Bearer
- [ ] At least one ownership test per resource type (wallet, remittance, funding order): wrong user → 404
- [ ] All tests follow golden rule, BDDMockito `given()`/`then()`, no generic matchers (`any()`, `anyString()` forbidden), `// given // when // then` markers
- [ ] MapStruct mappers injected via `@Spy` (not `@Mock`) in unit tests — MapStruct generates real mapping logic that must be exercised
- [ ] Test fixtures in `src/testFixtures/` return builders (not built objects) so tests can customize — e.g., `WalletFixtures.someWallet()` returns `Wallet.builder()`, caller calls `.build()`
- [ ] `AuthFixtures` provides `SOME_USER_ID` (UUID), `SOME_EMAIL`, pre-built `AppUser`, `SocialIdentity`, `AuthSession`
- [ ] No `@Autowired` in unit tests (only in integration tests where needed)
- [ ] 80% line / 70% branch coverage maintained across all updated tests

### References

- Spec: § Test strategy
- [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) — all sections (Golden Rule, Given/When/Then, Mocking, Fixtures, Integration Setup)
- ADR-018 (Testing Strategy — two-tier: unit + integration with TestContainers)

---

## Issue 9: Postman collection — auth-aware endpoints

**Labels:** `docs`, `api`
**Size:** S
**Depends on:** #5

### Business context

The Postman collection (used for E2E runs and demo walkthroughs) currently passes `senderId` in request bodies and has no auth headers. It needs to reflect the new auth flow so that manual testing and demos work against the secured API.

### Description

Update the existing Postman collection (or create a new one) under `e2e-tests/`:

- **Auth flow:**
  - `POST /api/auth/social` — with a Google `idToken` (document how to obtain one via Google OAuth playground)
  - `POST /api/auth/refresh` — using the refresh token from login
  - `POST /api/auth/logout`
- **Collection variables:** `{{accessToken}}`, `{{refreshToken}}`, `{{walletId}}`
- **Pre-request script:** auto-set `Authorization: Bearer {{accessToken}}` on all authenticated requests
- **Updated requests:**
  - `POST /api/remittances`: body has only `recipientPhone` + `amountUsdc` (no `senderId`)
  - `GET /api/remittances`: no `?senderId=` query param
  - `GET /api/wallets/me`: new request
- **Documentation:** Brief notes in collection description on how to obtain a test Google `idToken`

### Acceptance criteria

- [ ] Collection includes all 3 auth endpoints
- [ ] All authenticated requests include `Bearer {{accessToken}}` header
- [ ] `POST /api/remittances` body does not contain `senderId`
- [ ] `GET /api/remittances` has no `senderId` query param
- [ ] `GET /api/wallets/me` request exists
- [ ] Collection is importable and runnable with Newman

### References

- Spec: § Decisions #13 (DTO migration), § New+changed endpoints
- Existing collection: `e2e-tests/`

---

## Issue 10: Dev-profile-gate `POST /api/wallets`

**Labels:** `backend`, `auth`
**Size:** XS
**Depends on:** #7

### Business context

After auth, wallets are created eagerly during `POST /api/auth/social`. The standalone `POST /api/wallets` endpoint is still useful for tests and Postman, but must not be available in production — it would allow wallet creation without authentication.

### Description

Gate `POST /api/wallets` behind a config flag `stablepay.test-endpoints.wallet-create-enabled` (default `false`). When disabled, the endpoint does not exist (404 from Spring's default handler).

**Implementation:** Use `@ConditionalOnProperty(name = "stablepay.test-endpoints.wallet-create-enabled", havingValue = "true")` on a separate `DevWalletController` bean. Extract `POST /api/wallets` from `WalletController` into `DevWalletController`. This is preferred over a runtime check because the endpoint is completely absent from the bean context in production — no accidental exposure.

**Config:**
```yaml
stablepay:
  test-endpoints:
    wallet-create-enabled: ${STABLEPAY_TEST_WALLET_CREATE:false}
```

Set to `true` in `application-test.yml` and `application-sandbox.yml` so tests and Postman keep working.

### Acceptance criteria

- [ ] `@ConditionalOnProperty` used — `DevWalletController` bean absent when `wallet-create-enabled=false` (endpoint returns 404 from Spring's default handler)
- [ ] `POST /api/wallets` works normally when `wallet-create-enabled=true`
- [ ] Test profile has `wallet-create-enabled=true` — existing wallet creation tests pass
- [ ] Default is `false` (safe for production)
- [ ] `./gradlew build` passes
- [ ] Integration test verifies 404 when property is false (use `@TestPropertySource` to override)

### References

- Spec: § Decision #7
- [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) § 3.1 (Controllers)
- [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) § 7 (Integration Test Setup)
- ADR-022 (Auth Strategy — eager wallet provisioning replaces standalone `POST /api/wallets` in production)
