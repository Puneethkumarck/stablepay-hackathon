---
title: Sender App API Gaps — Design vs. Backend
status: draft
created: 2026-04-23
updated: 2026-04-23
issue: TBD
revision: 2 (source-code-verified)
---

# Sender App API Gaps — Design vs. Backend

## 1. Objective

Document every gap between the sender app UI kit (`stablepay-design-system/project/ui_kits/sender_app/`) and the existing backend REST API. Each gap identifies a design element that has no corresponding API field or endpoint, with a recommended fix and priority classification.

The design has 10 screens: Auth, Home, Send Amount (Step 1/3), Send Recipient (Step 2/3), Send Review (Step 3/3), Sending, Remittance Detail, Add Funds, Activity, and Me. The backend exposes 16 REST endpoints across auth, wallet, remittance, claim, funding, FX, and webhook controllers.

**Verification method:** Every "Covered" and "Missing" claim below was verified by reading the actual source files (DTOs, domain models, JPA entities, controllers, Flyway migrations, and MapStruct mappers). No claim is based on inference or memory.

## 2. Screen-by-Screen Audit

### 2.1 Auth Screen

**Design reference:** `Components.jsx` — `AuthScreen`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| Google sign-in button | `POST /api/auth/social` | `provider`, `idToken` | Covered |
| Live transfer feed (5 recent platform-wide transfers) | None | None | **GAP-1** |

### 2.2 Home Screen

**Design reference:** `Components.jsx` — `HomeScreen`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| USDC balance (`$248.50`) | `GET /api/wallets/me` | `availableBalance` | Covered |
| Wallet address (`CrsMd...DAd18`) | `GET /api/wallets/me` | `solanaAddress` | Covered |
| "Available to send" label | `GET /api/wallets/me` | `availableBalance` | Covered |
| Send / Add funds action buttons | N/A (frontend routing) | N/A | Covered |
| Recent transactions — recipient name | `GET /api/remittances` | Missing | **GAP-2** |
| Recent transactions — phone, amount, status | `GET /api/remittances` | `recipientPhone`, `amountUsdc`, `status` | Covered |
| Recent transactions — relative time ("2m ago") | `GET /api/remittances` | `createdAt` (client formats) | Covered |

### 2.3 Send Amount Screen (Step 1/3)

**Design reference:** `Components.jsx` — `SendAmount`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| Amount input | N/A (client state) | N/A | Covered |
| FX rate (`84.50 INR / USD`) | `GET /api/fx/USD-INR` | `rate` | Covered |
| "They receive ₹X" conversion | Client-side calculation | N/A | Covered |
| Network fee (`$0.002`) | None | None | **GAP-3** |
| Settlement time (`~30 sec`) | None | None | **GAP-4** |
| Corridor (`USD → INR`) | Hardcoded | N/A | Covered |

### 2.4 Send Recipient Screen (Step 2/3)

**Design reference:** `Components.jsx` — `SendRecipient`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| Phone number input | `POST /api/remittances` | `recipientPhone` | Covered |
| Recent contacts list (name + phone) | None | None | **GAP-5** |

### 2.5 Send Review Screen (Step 3/3)

**Design reference:** `Components.jsx` — `SendReview`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| "They receive ₹X" | Client-side from FX rate | N/A | Covered |
| "You send $X USDC" | Client-side | N/A | Covered |
| FX rate | `GET /api/fx/USD-INR` | `rate` | Covered |
| Network fee (`$0.002`) | None | None | **GAP-3** (same) |
| Delivery method ("Instant on-chain + UPI") | Hardcoded | N/A | Covered |
| Claim expires ("48 hours") | Hardcoded (matches system) | N/A | Covered |
| Escrow info banner | Static copy | N/A | Covered |
| "Confirm & send" → creates remittance | `POST /api/remittances` | `recipientPhone`, `amountUsdc` | Covered |

### 2.6 Sending Screen

**Design reference:** `Components.jsx` — `SendingScreen`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| 3-step animated progress (Authorising → Locking → Notifying) | None (no real-time updates) | None | **GAP-6** |
| Final state: "Sent — awaiting claim" | `GET /api/remittances/{id}` | `status` | Covered |
| "48h claim window" | `GET /api/remittances/{id}` | `expiresAt` | Covered |

### 2.7 Remittance Detail Screen

**Design reference:** `Components.jsx` — `DetailScreen`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| Amount sent | `GET /api/remittances/{id}` | `amountUsdc` | Covered |
| Recipient name | `GET /api/remittances/{id}` | Missing | **GAP-2** (same) |
| INR amount | `GET /api/remittances/{id}` | `amountInr` | Covered |
| Status badge ("Escrowed · awaiting claim") | `GET /api/remittances/{id}` | `status` | Covered |
| 5-step timeline | `GET /api/remittances/{id}/timeline` | `steps[]` | Covered |
| Remittance ID | `GET /api/remittances/{id}` | `remittanceId` | Covered |
| Escrow PDA | `GET /api/remittances/{id}` | `escrowPda` | Covered |
| On-chain fee (`$0.002`) | `GET /api/remittances/{id}` | Missing | **GAP-3** (same) |
| FX rate | `GET /api/remittances/{id}` | `fxRate` | Covered |
| "Expires in 47h 58m" countdown | `GET /api/remittances/{id}` | `expiresAt` (client calculates) | Covered |

### 2.8 Add Funds Screen

**Design reference:** `Components.jsx` — `AddFundsScreen`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| Amount input with presets ($25, $50, $100, $250) | N/A (client state) | N/A | Covered |
| Min/Max validation ($1 – $10,000) | `POST /api/wallets/{id}/fund` | Server validates `[1.00, 10000.00]` | Covered |
| "Pay with Stripe" button | `POST /api/wallets/{id}/fund` | Returns `stripeClientSecret` | Covered |
| Processing state | `GET /api/funding-orders/{id}` | `status` | Covered |
| Success state ("$50.00 USDC added") | `GET /api/funding-orders/{id}` | `status=FUNDED`, `amountUsdc` | Covered |
| Payment method ("Credit / Debit card") | Hardcoded | N/A | Covered |

**No gaps.** Fully covered by existing APIs.

### 2.9 Activity Screen

**Design reference:** `Components.jsx` — `ActivityScreen`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| All transfers list | `GET /api/remittances` (paginated) | Page<RemittanceResponse> | Covered |
| Recipient name in each row | `GET /api/remittances` | Missing | **GAP-2** (same) |
| Phone, amount, status, time | `GET /api/remittances` | Present in response | Covered |

### 2.10 Me Screen

**Design reference:** `Components.jsx` — `MeScreen`

| Design Element | API Endpoint | API Field | Status |
|---|---|---|---|
| Avatar initial ("R") | Derived from user name | Missing (no name) | **GAP-7** |
| User display name ("Raj Sharma") | `AuthResponse` → `UserResponse` | Missing | **GAP-7** (same) |
| User email | `AuthResponse` → `UserResponse` | `email` | Covered |
| USD balance | `GET /api/wallets/me` | `availableBalance` | Covered |
| Member since | `AuthResponse` → `UserResponse` | `createdAt` | Covered |
| Wallet address | `GET /api/wallets/me` | `solanaAddress` | Covered |
| Network ("Solana Mainnet") | Hardcoded | N/A | Covered |
| KYC status ("Verified") | None | None | **GAP-8** |
| Notifications setting ("On") | None | None | **GAP-9** |
| Support link | N/A (external link) | N/A | Covered |
| Sign out | `POST /api/auth/logout` | N/A | Covered |

---

## 3. Consolidated Gap List

### Critical — Required for basic UI functionality

#### GAP-2: Recipient name missing from remittance model

**Affected screens:** Home, Activity, Detail, Send Recipient (recent contacts)

**Problem:** The design displays recipient names ("Raj Patel", "Meera Iyer") alongside phone numbers in transaction rows, the detail screen, and the recent contacts list. `CreateRemittanceRequest` only accepts `recipientPhone`. `RemittanceResponse` does not include a recipient name.

**Source code evidence:**
- `CreateRemittanceRequest.java` — record has only `recipientPhone` and `amountUsdc`
- `Remittance.java` — domain model has no `recipientName` field (has `recipientPhone`, `senderId`, amounts, status, etc.)
- `RemittanceEntity.java` — JPA entity has no `recipient_name` column
- `RemittanceResponse.java` — response DTO has no `recipientName` field
- `V1__initial_schema.sql` — `remittances` table has no `recipient_name` column
- `CreateRemittanceHandler.java:44` — `handle(UUID senderId, String recipientPhone, BigDecimal amountUsdc)` — no name parameter
- `RemittanceController.java:63` — passes only `request.recipientPhone()` and `request.amountUsdc()` to handler

**Recommended fix:**

1. Add `recipientName` field to `CreateRemittanceRequest`:
   ```java
   public record CreateRemittanceRequest(
       @NotBlank String recipientPhone,
       @NotNull @Positive BigDecimal amountUsdc,
       @Size(max = 100) String recipientName  // new, optional
   ) {}
   ```

2. Add `recipientName` field to `Remittance` domain model.

3. Add `recipient_name` column to `RemittanceEntity`.

4. Add `recipientName` field to `RemittanceResponse`.

5. Update `CreateRemittanceHandler.handle()` signature to accept `recipientName`.

6. Update `RemittanceController.createRemittance()` to pass `request.recipientName()`.

7. Flyway migration: `V{N}__add_recipient_name_to_remittances.sql`
   ```sql
   ALTER TABLE remittances ADD COLUMN recipient_name VARCHAR(100);
   ```

**Impact:** 7 files changed. Low risk — additive only, nullable column, MapStruct auto-maps matching field names.

---

#### GAP-5: Recent recipients API missing

**Affected screens:** Send Recipient (Step 2/3)

**Problem:** The design shows a "Recent" contacts list with 4 recipients (name + phone) that the sender can tap to pre-fill. No endpoint returns distinct past recipients.

**Source code evidence:**
- No controller in `application/controller/` handles recipients or contacts
- No handler in `domain/*/handler/` provides this query
- No repository method returns distinct recipients
- `RemittanceRepository` port has no `findDistinctRecipients` or similar method
- The design's `SendRecipient` component (`Components.jsx:220-257`) shows 4 contacts: `['Raj Patel', '+91 98765 43210']`, etc.

**Dependency:** Requires GAP-2 (recipient name stored on remittances) to be resolved first. Without `recipientName` on the remittance model, the query can only return phone numbers, not names.

**Recommended fix:**

Add `GET /api/recipients/recent` (authenticated):
```
GET /api/recipients/recent?limit=10
Authorization: Bearer {token}

Response 200:
[
  { "name": "Raj Patel", "phone": "+91 98765 43210", "lastSentAt": "2026-04-22T..." },
  { "name": "Meera Iyer", "phone": "+91 99887 66554", "lastSentAt": "2026-04-21T..." }
]
```

**Implementation:** New `RecentRecipientsHandler` + repository query: `SELECT DISTINCT ON (recipient_phone) recipient_name, recipient_phone, created_at FROM remittances WHERE sender_id = ? ORDER BY recipient_phone, created_at DESC LIMIT ?`

**Impact:** New endpoint + handler + repository query + DTO. Low risk.

---

#### GAP-7: User display name missing

**Affected screens:** Me screen (name + avatar initial), Claim page (senderDisplayName)

**Problem:** `UserResponse` returns only `id`, `email`, `createdAt`. The design shows a full name ("Raj Sharma") and derives an avatar initial ("R") from it. Google OAuth provides the user's name in the ID token JWT (`name` claim), but `GoogleIdTokenVerifierAdapter` only extracts `sub`, `email`, `email_verified` — the name claim is available but discarded.

**Source code evidence:**
- `GoogleIdTokenVerifierAdapter.java:48` — builds `SocialIdentity` without name
- `SocialIdentity.java` — no `name` field in the record
- `AppUser.java` — no `name` field (only `id`, `email`, `createdAt`)
- `UserEntity.java` — no `name` column
- `V8__users_and_auth.sql` — `users` table has no `name` column
- `UserResponse.java` — no `name` field

**Cascade impact:** `GetClaimQueryHandler.java:31-32` derives `senderDisplayName` from `user.email().split("@")[0]` (e.g., "raj" from "raj@gmail.com"). If this gap is fixed, the claim handler should use the real name instead of the email prefix.

**Recommended fix:**

1. Extract `name` from Google JWT in `GoogleIdTokenVerifierAdapter`:
   ```java
   var name = jwt.getClaimAsString("name"); // available in Google ID tokens
   ```

2. Add `name` field to `SocialIdentity` model.

3. Store `name` in `AppUser` via `SocialLoginHandler`.

4. Add `name` column to `users` table (not `app_users` — the table is named `users` per V8 migration).

5. Add `name` field to `UserResponse`.

6. Flyway migration: `V{N}__add_name_to_users.sql`
   ```sql
   ALTER TABLE users ADD COLUMN name VARCHAR(200);
   ```

7. Update `GetClaimQueryHandler` to prefer real name over email prefix.

**Impact:** 6 files changed across adapter → domain model → entity → DTO → migration. Low risk — additive only, nullable column.

---

### Nice-to-Have — Can be hardcoded or deferred for hackathon

#### GAP-1: Live transfer feed on auth screen

**Affected screens:** Auth screen

**Problem:** The auth screen shows a real-time feed of 5 recent transfers across the platform (not per-user). Example: "US→IN $200 → ₹16,900 · 12s ago". No public endpoint provides this.

**Recommended fix (if implemented):**

Add `GET /api/feed/recent` (public, unauthenticated):
```
GET /api/feed/recent?limit=5

Response 200:
[
  {
    "corridor": "USD-INR",
    "amountUsdc": 200.00,
    "amountInr": 16900.00,
    "completedAt": "2026-04-23T10:00:00Z"
  }
]
```

Must anonymize — no sender/recipient info, no remittance IDs.

**Hackathon alternative:** Hardcode the feed in the mobile app. The auth screen is seen once; this is cosmetic.

**Priority:** Low

---

#### GAP-3: Network fee not in API responses

**Affected screens:** Send Amount, Send Review, Remittance Detail

**Problem:** The design displays "$0.002" as the network fee in multiple places. No API returns this value. Currently, Solana transaction fees are near-constant (~5000 lamports = ~$0.002), but the exact value depends on network conditions.

**Recommended fix (if implemented):**

Option A — Add `networkFee` field to `RemittanceResponse`:
```java
public record RemittanceResponse(
    // ... existing fields
    BigDecimal networkFee  // new
) {}
```

Option B — Add a fee estimation endpoint:
```
GET /api/fees/estimate?corridor=USD-INR&amount=100

Response 200:
{ "networkFee": 0.002, "currency": "USD" }
```

**Hackathon alternative:** Hardcode `$0.002` in the mobile app. The value is stable on Solana and unlikely to change during the demo.

**Priority:** Low

---

#### GAP-4: Settlement time not in API

**Affected screens:** Send Amount

**Problem:** The design shows "Settlement: ~30 sec". No API provides an estimated settlement time.

**Hackathon alternative:** Hardcode in mobile app. Settlement time is a known constant for the USD→INR corridor (on-chain finality + UPI rails).

**Priority:** Low

---

#### GAP-6: No real-time progress during send

**Affected screens:** Sending screen

**Problem:** The design shows a 3-step animated progress tracker during remittance creation: (1) Authorising transfer, (2) Locking funds, (3) Notifying recipient. The current API returns synchronously from `POST /api/remittances` — there's no mechanism for the client to receive step-by-step progress updates.

**Recommended fix (if implemented):**

Option A — Server-Sent Events (SSE):
```
GET /api/remittances/{id}/progress
Accept: text/event-stream

event: step
data: { "step": "AUTHORISING", "status": "completed" }

event: step
data: { "step": "LOCKING", "status": "in_progress" }
```

Option B — Short polling on timeline:
```
GET /api/remittances/{id}/timeline  (poll every 2s)
```

**Hackathon alternative:** Simulate the progress animation client-side after `POST /api/remittances` returns successfully. The design prototype itself does exactly this — it uses `setTimeout` to animate through steps. This is the pragmatic approach.

**Priority:** Low

---

#### GAP-8: KYC status missing

**Affected screens:** Me screen

**Problem:** The design shows "KYC status: Verified" on the Me screen. There is no KYC domain model, status field, or API endpoint.

**Hackathon alternative:** Hardcode "Verified" in the mobile app. KYC is not in scope for the hackathon demo — all users are assumed verified.

**Priority:** Low (out of hackathon scope)

---

#### GAP-9: Notification preferences missing

**Affected screens:** Me screen

**Problem:** The design shows a "Notifications: On" row on the Me screen. No settings or preferences API exists.

**Recommended fix (if implemented):**
```
GET  /api/settings/notifications → { "enabled": true }
PUT  /api/settings/notifications → { "enabled": true }
```

**Hackathon alternative:** Hardcode "On" in the mobile app.

**Priority:** Low (out of hackathon scope)

---

## 4. Missing DTO Fields Summary

| DTO | Missing Field | Type | Source | Gap |
|---|---|---|---|---|
| `CreateRemittanceRequest` | `recipientName` | `String` (optional) | User input | GAP-2 |
| `RemittanceResponse` | `recipientName` | `String` | Remittance model | GAP-2 |
| `RemittanceResponse` | `networkFee` | `BigDecimal` | Transaction data | GAP-3 |
| `UserResponse` | `name` | `String` | Google OAuth profile | GAP-7 |

## 5. Missing Endpoints Summary

| Endpoint | Method | Auth | Gap |
|---|---|---|---|
| `/api/recipients/recent` | `GET` | Bearer JWT | GAP-5 |
| `/api/feed/recent` | `GET` | Public | GAP-1 |
| `/api/fees/estimate` | `GET` | Public | GAP-3 |
| `/api/settings/notifications` | `GET` / `PUT` | Bearer JWT | GAP-9 |

## 6. Recommended Implementation Order

For hackathon, implement only the critical gaps:

1. **GAP-7** — User display name (enables Me screen + avatar)
2. **GAP-2** — Recipient name (enables transaction rows + detail screen)
3. **GAP-5** — Recent recipients API (enables send flow contact picker)

Everything else can be hardcoded in the mobile app for the demo.

## 7. Existing API Coverage Summary

| Screen | Endpoints Used | Coverage |
|---|---|---|
| Auth | `POST /api/auth/social` | 90% (missing live feed) |
| Home | `GET /api/wallets/me`, `GET /api/remittances` | 90% (missing recipient name) |
| Send Amount | `GET /api/fx/USD-INR` | 85% (missing fee, settlement) |
| Send Recipient | `POST /api/remittances` | 50% (missing contacts API) |
| Send Review | `GET /api/fx/USD-INR` | 95% (missing fee) |
| Sending | `POST /api/remittances` | 70% (missing real-time progress) |
| Detail | `GET /api/remittances/{id}`, `GET /api/remittances/{id}/timeline` | 90% (missing name, fee) |
| Add Funds | `POST /api/wallets/{id}/fund`, `GET /api/funding-orders/{id}` | **100%** |
| Activity | `GET /api/remittances` | 90% (missing recipient name) |
| Me | `POST /api/auth/logout`, `GET /api/wallets/me` | 70% (missing name, KYC, notifications) |

**Overall backend API coverage for sender app: ~85%.** Three targeted changes (GAP-2, GAP-5, GAP-7) bring it to ~95%+.

## 8. Design Notes (Not Gaps — Data Sufficient)

These are design-vs-API differences that do NOT require backend changes:

### 8.1 Five-step detail timeline vs four-step API

The design's `DetailScreen` shows 5 visual steps:
1. "Initiated" (done)
2. "Escrowed on Solana" (done)
3. "Claim SMS delivered" (done)
4. "Awaiting recipient claim" (live)
5. "Delivery via UPI" (pending)

The API timeline (`GET /api/remittances/{id}/timeline`) returns 4 steps: INITIATED, ESCROWED, CLAIMED, DELIVERED. This is **not a gap** — the mobile app can render 5 visual steps by splitting the CLAIMED step into "SMS delivered" and "Awaiting claim" based on the `smsNotificationFailed` flag available in `RemittanceResponse`.

### 8.2 Token refresh does not return user/wallet data

`POST /api/auth/refresh` returns `AuthResponse` with `user=null` and `wallet=null` (per `AuthResponseMapper.java:26-28` — both are `@Mapping(ignore = true)`). After a token refresh, the client must rely on cached user/wallet data from the initial login.

For the hackathon this is fine — the mobile app can cache the login response. For production, consider either returning user+wallet on refresh, or adding `GET /api/users/me` and `GET /api/wallets/me` (the wallet endpoint already exists).

### 8.3 Sender display name on claim page uses email prefix

`GetClaimQueryHandler.java:31-32` derives `senderDisplayName` from `user.email().split("@")[0]` — so "raj@gmail.com" becomes "Raj" (after client-side capitalization). The design's claim page shows "Raj sent you ₹8,450.00". This works for the hackathon but should use the real name once GAP-7 is implemented.
