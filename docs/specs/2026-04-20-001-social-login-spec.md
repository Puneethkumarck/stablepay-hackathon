# STA-XX — Social login (Google) + Spring Security

**Date:** 2026-04-20
**Author:** grill session with Claude
**Status:** Spec — ready to break into tickets

---

## Summary

Introduce authenticated access to StablePay. First provider: Google, on Android only, for the hackathon demo. Every `/api/*` endpoint becomes authenticated except claim (token-scoped) and Stripe webhook (signature-scoped). First login auto-provisions an MPC wallet.

---

## Decisions (locked during grill)

| # | Area | Decision |
|---|---|---|
| 1 | Providers at launch | **Google only.** Apple deferred — iOS App Store will reject until Apple is added (accepted). Phone OTP deferred indefinitely. |
| 2 | Mobile SDK | `@react-native-google-signin/google-signin` with **EAS development client** (Android-only). Android OAuth Client ID. No Expo Go support. |
| 3 | Identity model | Two tables: `users(UUID PK)` + `social_identities(provider, subject, email, email_verified)` with `UNIQUE(provider, subject)`. `wallets.user_id` FK → `users.id`. `remittances.sender_id` FK → `users.id`. Look up by `(provider, subject)`, never email. Reject login when `email_verified=false`. |
| 4 | JWT signing | **HS256.** Secret in `JWT_SECRET` env var (≥32 bytes, base64). |
| 5 | Refresh tokens | Opaque 32-byte random, SHA-256 hashed in DB, rotated on use, single active chain per user. Access 15 min; refresh 30 days. Logout = revoke row. |
| 6 | First-login wallet | **Eager.** `SocialLoginHandler` calls `CreateWalletHandler` inline. MPC failure rolls back the whole transaction — no partial user. |
| 7 | `POST /api/wallets` | **Kept but dev-profile-gated** (`STABLEPAY_TEST_WALLET_CREATE=false` in prod). Used by tests + Postman. |
| 8 | Google idToken verification | Spring `NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")` + `JwtIssuerValidator` + `JwtClaimValidator<aud>`. Separate bean, called manually from `SocialLoginHandler`, **not in filter chain**. |
| 9 | App JWT verification | Spring OAuth2 Resource Server + `NimbusJwtDecoder.withSecretKey(HMAC)`. **Wired into filter chain.** |
| 10 | Public endpoints | `POST /api/auth/social`, `POST /api/auth/refresh`, `GET\|POST /api/claims/{token}`, `POST /webhooks/stripe`, `GET /actuator/health`, `OPTIONS *`, `POST /api/wallets` (dev-only), `/v3/api-docs/**` + `/swagger-ui/**` (dev/sandbox only). |
| 11 | Authenticated endpoints | Everything else including `GET /api/wallets/me`, `/api/fx/**`, `/api/remittances/**`, `/api/wallets/{id}/fund`, `/api/funding-orders/**`, `POST /api/auth/logout`. |
| 12 | Ownership checks | Load-by-id handlers verify the resource's `user_id` matches authenticated user. **404 on mismatch** (not 403) to avoid user enumeration. |
| 13 | Request DTO migration | Clean break. Remove `senderId`/`userId` from `CreateRemittanceRequest`, `CreateWalletRequest`, etc. Controllers read `@AuthenticationPrincipal AppUser`. Drop `?senderId=` query param on `GET /api/remittances` (handler uses authenticated user's UUID internally). Remove `userId` from `WalletResponse` and `RemittanceResponse` (always the authenticated user). Replace `ClaimResponse.senderId` with `senderDisplayName` (email local part from `users.email`, e.g. "priya" — claim page is public, recipients see a human name not a UUID). |
| 14 | JWT claims | Minimal: `sub = user_id UUID`, `iat`, `exp`. No email, name, roles, wallet. |
| 15 | `AppUser` principal | `record AppUser(UUID id)` produced by a `Converter<Jwt, AbstractAuthenticationToken>`. |
| 16 | Rate limiting | **None for hackathon.** Add Bucket4j before any production deploy. |
| 17 | Mobile 401 handling | Axios interceptor → silent `POST /api/auth/refresh` → retry original once. Refresh returning 401 → hard-redirect to login. |
| 18 | Logout | Revoke refresh row; access token valid until natural expiry (≤15 min). No JWT denylist. |
| 19 | Audit logging | `@Slf4j` structured logs for `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `REFRESH`, `LOGOUT` with `{userId, ip, userAgent, timestamp}`. |
| 20 | Secret rotation | Manual env var change + rolling restart. All access tokens invalid; refresh tokens survive (re-issue on next `/refresh`). |
| 21 | Mobile token storage | `expo-secure-store` for both tokens. Never `AsyncStorage`. |

---

## API

### `POST /api/auth/social`

**Public.** Exchanges a Google `idToken` for app tokens + user + wallet.

Request:
```json
{ "provider": "google", "idToken": "eyJhbGci..." }
```

Response (201 on new user, 200 on returning):
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "r1_AbCdEf...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user":   { "id": "uuid", "email": "priya@gmail.com", "createdAt": "..." },
  "wallet": { "id": 1, "solanaAddress": "7xKXzd...", "availableBalance": "0.00", ... }
}
```

Errors: `400` unsupported provider · `401` invalid idToken / email not verified · `502` MPC sidecar failure.

### `POST /api/auth/refresh`

**Public.** Rotates the refresh token.

Request: `{ "refreshToken": "r1_..." }`
Response 200: `{ accessToken, refreshToken, tokenType, expiresIn }`
Errors: `401` unknown / revoked / expired refresh.

### `POST /api/auth/logout`

**Authenticated.** Revokes the caller's current refresh token. 204 on success.

---

## New + changed endpoints for FE integration

### `GET /api/wallets/me` (new)

**Authenticated.** Returns the authenticated user's wallet. Required by FE for:
- Home screen reload / app resume (mockup A4)
- Balance refresh after funding completes (mockup B4)
- Pre-send balance check (mockup C2)

Response 200:
```json
{ "id": 1, "solanaAddress": "7xKXzd...", "availableBalance": "250.00", "totalBalance": "250.00", ... }
```
Errors: `404` user has no wallet (should not happen after auth — wallet is created eagerly).

Domain: `GetWalletQueryHandler` (new) → `WalletRepository.findByUserId(UUID)`.

### `GET /api/remittances` (changed)

**Authenticated.** No longer accepts `?senderId=` query param. Returns paginated remittances for the authenticated user (mockup C5 "History"). Pageable params (`page`, `size`, `sort`) still apply.

### `POST /api/remittances` (changed)

**Authenticated.** Body shrinks to `{ "recipientPhone": "...", "amountUsdc": 100.00 }`. `senderId` is taken from `@AuthenticationPrincipal`.

### `GET /api/claims/{token}` (unchanged — public)

Response `ClaimResponse` replaces `senderId` (was String, now UUID) with `senderDisplayName` (String, email local part). Mockup D2 displays "from Priya S." — a UUID would be meaningless to the recipient.

---

## FE flow changes from mockup v2

The following mockup screens change behavior after auth:

| Screen | Before auth | After auth |
|---|---|---|
| A2 Sign in | No backend call | `POST /api/auth/social` → tokens + user + wallet |
| A3 Creating wallet | `POST /api/wallets` (separate call) | Wallet created inline during `/api/auth/social`. A3 spinner shows during the single auth call — no separate wallet endpoint needed. |
| A4 Home | `WalletResponse` from creation | First load: wallet from auth response. Subsequent: `GET /api/wallets/me`. |
| C2 Compose | Body includes `senderId` | Body is `{ recipientPhone, amountUsdc }` only. |
| C5 History | `GET /api/remittances?senderId=X` | `GET /api/remittances` (server reads user from JWT). |
| E5 Returning user | `POST /api/wallets` → 409 | `POST /api/auth/social` → 200 with existing user + wallet. No 409. |

---

## Domain layout (hexagonal)

```
com.stablepay.domain.auth/
├── model/
│   ├── AppUser.java               // record AppUser(UUID id, String email, Instant createdAt)
│   ├── SocialIdentity.java        // record SocialIdentity(String provider, String subject, String email, boolean emailVerified)
│   ├── AuthSession.java           // record AuthSession(String accessToken, String refreshToken, Instant accessExpiresAt)
│   └── RefreshToken.java          // record RefreshToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant revokedAt)
├── port/
│   ├── SocialIdentityVerifier.java  // interface — verify(provider, idToken) → SocialIdentity
│   ├── AuthTokenIssuer.java         // interface — issue(userId) → AuthSession
│   ├── UserRepository.java          // findById, save
│   ├── SocialIdentityRepository.java  // findByProviderAndSubject, save
│   └── RefreshTokenRepository.java    // findByHash, save, revoke
├── handler/
│   ├── SocialLoginHandler.java
│   ├── RefreshTokenHandler.java
│   └── LogoutHandler.java
└── exception/
    ├── InvalidIdTokenException.java          (SP-0032)
    ├── EmailNotVerifiedException.java        (SP-0033)
    ├── UnsupportedAuthProviderException.java (SP-0034)
    ├── InvalidRefreshTokenException.java     (SP-0035)
    └── RefreshTokenExpiredException.java     (SP-0036)

com.stablepay.domain.wallet/handler/
└── GetWalletQueryHandler.java         // new — findByUserId(authenticatedUser.id)

com.stablepay.application.controller.auth/
├── AuthController.java
└── mapper/AuthResponseMapper.java  (MapStruct)

com.stablepay.application.dto/
├── SocialLoginRequest.java
├── RefreshTokenRequest.java
├── AuthResponse.java
└── UserResponse.java

com.stablepay.application.config/
├── SecurityConfig.java             // filter chain
├── JwtConfig.java                  // two JwtDecoder beans
├── AppUserConverter.java           // Jwt → AppUser principal
└── GoogleAuthProps.java            // @ConfigurationProperties

com.stablepay.infrastructure.auth/
├── jwt/
│   ├── JwtTokenIssuerAdapter.java          // implements AuthTokenIssuer
│   └── RefreshTokenGenerator.java          // 32-byte random + SHA-256 hash
└── google/
    └── GoogleIdTokenVerifierAdapter.java   // implements SocialIdentityVerifier

com.stablepay.infrastructure.db.user/
├── UserEntity.java
├── UserJpaRepository.java
├── UserRepositoryAdapter.java
├── UserMapper.java
├── SocialIdentityEntity.java
├── SocialIdentityJpaRepository.java
├── SocialIdentityRepositoryAdapter.java
├── RefreshTokenEntity.java
├── RefreshTokenJpaRepository.java
└── RefreshTokenRepositoryAdapter.java
```

---

## Flyway migration `V8__users_and_auth.sql`

```sql
CREATE TABLE users (
    id         UUID         PRIMARY KEY,
    email      VARCHAR(320) NOT NULL,       -- last known, display only
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE social_identities (
    id             UUID         PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider       VARCHAR(32)  NOT NULL,
    subject        VARCHAR(255) NOT NULL,
    email          VARCHAR(320) NOT NULL,
    email_verified BOOLEAN      NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_social_identity UNIQUE (provider, subject)
);

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    issued_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    CONSTRAINT uk_refresh_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_user  ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_hash  ON refresh_tokens(token_hash) WHERE revoked_at IS NULL;

-- wallets.user_id and remittances.sender_id are VARCHAR strings; migrate to UUID FK.
-- For hackathon we truncate since dev data isn't precious.
-- CASCADE will also empty remittances, claim_tokens, and funding_orders (FK chain).
TRUNCATE TABLE wallets CASCADE;
ALTER TABLE wallets DROP COLUMN user_id;
ALTER TABLE wallets ADD COLUMN user_id UUID NOT NULL REFERENCES users(id) UNIQUE;
CREATE UNIQUE INDEX idx_wallet_user ON wallets(user_id);

TRUNCATE TABLE remittances CASCADE;
ALTER TABLE remittances DROP COLUMN sender_id;
ALTER TABLE remittances ADD COLUMN sender_id UUID NOT NULL REFERENCES users(id);
CREATE INDEX idx_remittance_sender ON remittances(sender_id);
```

---

## Error codes added

| Code | Meaning | HTTP |
|---|---|---|
| SP-0032 | Invalid Google ID token | 401 |
| SP-0033 | Email not verified | 401 |
| SP-0034 | Unsupported auth provider | 400 |
| SP-0035 | Invalid refresh token | 401 |
| SP-0036 | Refresh token expired | 401 |

---

## Dependencies to add (`build.gradle.kts`)

```kotlin
implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
```

`spring-boot-starter-oauth2-resource-server` transitively pulls in Spring Security and `nimbus-jose-jwt` (needed for both Google JWKS verification and HS256 app JWT signing).

---

## Cascading type changes (String → UUID userId)

The `wallets.user_id` and `remittances.sender_id` migration from VARCHAR to UUID FK requires matching type changes across all layers:

| Layer | File | Change |
|---|---|---|
| Domain model | `Wallet.java` | `String userId` → `UUID userId` |
| Domain model | `Remittance.java` | `String senderId` → `UUID senderId` |
| Domain port | `WalletRepository.java` | `findByUserId(String)` → `findByUserId(UUID)` |
| Domain port | `RemittanceRepository.java` | `findBySenderId(String, Pageable)` → `findBySenderId(UUID, Pageable)` |
| Domain handler | `CreateWalletHandler.java` | `handle(String userId)` → `handle(UUID userId)` |
| Domain handler | `ListRemittancesQueryHandler.java` | `handle(String senderId, ...)` → `handle(UUID senderId, ...)` |
| Domain handler | `CreateRemittanceHandler.java` | `senderId` param: `String` → `UUID` |
| Infra entity | `WalletEntity.java` | `private String userId` → `private UUID userId` |
| Infra entity | `RemittanceEntity.java` | `private String senderId` → `private UUID senderId` |
| Infra JPA | `WalletJpaRepository.java` | query methods update param type |
| Infra JPA | `RemittanceJpaRepository.java` | `findBySenderId(String, ...)` → `findBySenderId(UUID, ...)` |
| Infra adapter | `WalletRepositoryAdapter.java` | pass-through type change |
| Infra adapter | `RemittanceRepositoryAdapter.java` | pass-through type change |
| Domain handler | `GetWalletQueryHandler.java` | **new** — `handle(UUID userId)` → `WalletRepository.findByUserId(UUID)` |
| App DTO | `CreateWalletRequest.java` | remove `userId` field (dev-gated endpoint keeps it as `String` for Postman) |
| App DTO | `CreateRemittanceRequest.java` | remove `senderId` field |
| App DTO | `WalletResponse.java` | remove `userId` field |
| App DTO | `RemittanceResponse.java` | remove `senderId` field |
| App DTO | `ClaimResponse.java` | replace `String senderId` with `String senderDisplayName` |
| App controller | `WalletController.java` | add `GET /api/wallets/me` using `@AuthenticationPrincipal` + `GetWalletQueryHandler` |
| App controller | `RemittanceController.java` | drop `@RequestParam senderId`, use `@AuthenticationPrincipal` |
| App controller | `FundingController.java` | add ownership check: wallet belongs to authenticated user |
| App mapper | `ClaimApiMapper.java` | map sender email → display name (local part before `@`) |

---

## Config

```yaml
stablepay:
  jwt:
    secret: ${JWT_SECRET}                      # base64, 32+ bytes
    access-ttl: PT15M
    refresh-ttl: P30D
  auth:
    google:
      client-ids:
        - ${GOOGLE_ANDROID_CLIENT_ID}
  test-endpoints:
    wallet-create-enabled: ${STABLEPAY_TEST_WALLET_CREATE:false}

springdoc:
  api-docs:
    enabled: ${SPRINGDOC_ENABLED:true}         # false in prod
```

---

## Test strategy

- **Unit** — `SocialLoginHandlerTest` with stubbed `SocialIdentityVerifier` (happy path, new vs returning user, email-not-verified rejection, MPC failure rollback).
- **Integration** — `AuthControllerIT` using WireMock for Google JWKS endpoint. Full flow: signed idToken → `/api/auth/social` → returns app JWT + wallet → `GET /api/wallets/me` with Bearer → 200 → `POST /api/wallets/{id}/fund` with Bearer → 201.
- **Authorization** — `@ParameterizedTest` across every authenticated endpoint (including `/api/wallets/{id}/fund`, `/api/funding-orders/{fundingId}`, `/api/funding-orders/{fundingId}/refund`): no Bearer → 401, someone else's Bearer on my resource → 404.
- **Ownership** — Funding endpoints verify wallet ownership transitively: `POST /api/wallets/{id}/fund` checks wallet belongs to caller; `GET|POST /api/funding-orders/{fundingId}` checks the funding order's wallet belongs to caller.
- **Helper** — `SecurityTestBase` exposes `asUser(UUID)` that signs a real JWT with the test profile secret.

---

## Ticket breakdown

| # | Ticket | Rough size | Depends on |
|---|---|---|---|
| 1 | Schema + user domain skeleton (`V8__users_and_auth.sql` migration including `wallets.user_id` and `remittances.sender_id` UUID FK, entities, repos, exceptions) | M | — |
| 2 | JWT machinery (`JwtTokenIssuerAdapter`, `RefreshTokenGenerator`, both `JwtDecoder` beans, `AppUser` principal) + add `spring-boot-starter-oauth2-resource-server` dependency | M | 1 |
| 3 | `SocialIdentityVerifier` + `GoogleIdTokenVerifierAdapter` + Google config | S | — |
| 4 | `SocialLoginHandler` (eager wallet creation) + `RefreshTokenHandler` + `LogoutHandler` | M | 1, 2, 3 |
| 5 | `AuthController` + DTOs + MapStruct mappers + OpenAPI annotations | S | 4 |
| 6 | `SecurityConfig` (filter chain, public routes, 401 error mapping) | M | 2 |
| 7 | Migrate domain models + handlers String→UUID (`Wallet.userId`, `Remittance.senderId`, `CreateWalletHandler`, `CreateRemittanceHandler`, `ListRemittancesQueryHandler`, all JPA entities/repos/adapters). Add `GetWalletQueryHandler` + `GET /api/wallets/me`. Migrate controllers off `senderId`/`userId` DTOs to `@AuthenticationPrincipal`. Add ownership checks to `WalletController`, `RemittanceController`, `FundingController` (wallet-belongs-to-user for `/wallets/{id}/fund`, `/funding-orders/{fundingId}`, `/funding-orders/{fundingId}/refund`). Remove `userId` from `WalletResponse` and `RemittanceResponse`. Replace `ClaimResponse.senderId` with `senderDisplayName`. | L | 6 |
| 8 | Test helper `SecurityTestBase.asUser(...)` + update all failing tests (unit + integration across wallet, remittance, funding, claim, fx controllers and handlers) | L | 6, 7 |
| 9 | Postman collection: remove `senderId`, add Bearer token auth, document refresh | S | 5 |
| 10 | Dev-profile-gate `POST /api/wallets` | XS | 7 |

Total: ~25–30 files touched, ~900–1200 LOC including tests.

---

## Out of scope (deferred)

- Apple Sign-In (required for iOS App Store, not for hackathon devnet)
- Phone OTP
- Rate limiting
- Multi-device session management
- Refresh token reuse-detection (RFC 6819 rotation theft alert)
- RS256 / JWKS endpoint
- JWT denylist for instant revocation
- Account linking (Google + Apple merging onto one user)
- Admin UI / user management endpoints
