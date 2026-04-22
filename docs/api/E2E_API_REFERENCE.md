# StablePay API Reference (E2E Verified)

Captured from a 10/10 passing E2E run on 2026-04-22.
All authenticated endpoints require `Authorization: Bearer <JWT>` (HS256, `sub` = user UUID).

---

## Authentication

### POST /api/auth/social
Exchanges a Google ID token for app access and refresh tokens. Creates user + wallet on first login.

**Auth:** Public

**Request:**
```json
{
  "provider": "GOOGLE",
  "idToken": "<google-id-token>"
}
```

**Response (201 Created — new user):**
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "7d4718ba-a6f3-485c-b89b-77afa2caf206",
    "email": "user@gmail.com"
  },
  "wallet": {
    "id": 16,
    "solanaAddress": "CrsMdkbkAQRz7srMgeTe9sanoiHkeQBCKnhhVR9DAd18",
    "availableBalance": 0,
    "totalBalance": 0,
    "createdAt": "2026-04-22T06:52:59.379393834Z",
    "updatedAt": "2026-04-22T06:52:59.379393834Z"
  }
}
```

**Response (200 OK — returning user):** Same shape.

### POST /api/auth/refresh
Rotates refresh token and issues a new access token.

**Auth:** Public

**Request:**
```json
{
  "refreshToken": "<opaque-token>"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "<new-jwt>",
  "refreshToken": "<new-opaque-token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": null,
  "wallet": null
}
```

### POST /api/auth/logout
Revokes all refresh tokens for the authenticated user.

**Auth:** Bearer JWT

**Response:** `204 No Content`

---

## Wallets

### POST /api/wallets
Creates an MPC-backed Solana wallet for a user.

**Auth:** Public (dev-gated; production flow uses social login)

**Request:**
```json
{
  "userId": "7d4718ba-a6f3-485c-b89b-77afa2caf206"
}
```

**Response (201 Created):**
```json
{
  "id": 16,
  "solanaAddress": "CrsMdkbkAQRz7srMgeTe9sanoiHkeQBCKnhhVR9DAd18",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-22T06:52:59.379393834Z",
  "updatedAt": "2026-04-22T06:52:59.379393834Z"
}
```

### GET /api/wallets/me
Returns the authenticated user's wallet.

**Auth:** Bearer JWT

**Response (200 OK):**
```json
{
  "id": 16,
  "solanaAddress": "CrsMdkbkAQRz7srMgeTe9sanoiHkeQBCKnhhVR9DAd18",
  "availableBalance": 1.0,
  "totalBalance": 1.0,
  "createdAt": "2026-04-22T06:52:59.379394Z",
  "updatedAt": "2026-04-22T06:53:11.529385Z"
}
```

---

## Funding

### POST /api/wallets/{id}/fund
Creates a Stripe PaymentIntent and a funding order. Returns a one-time Stripe client secret.

**Auth:** Bearer JWT (ownership check — wallet must belong to authenticated user)

**Request:**
```json
{
  "amount": 1.0
}
```

**Response (201 Created):**
```json
{
  "fundingId": "26841929-ab9a-4fe9-b7c1-b9181d7a0b5f",
  "walletId": 16,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TOuek3nnME1dfOB11rVQqb5",
  "stripeClientSecret": "pi_3TOuek3nnME1dfOB11rVQqb5_secret_...",
  "createdAt": "2026-04-22T06:53:04.099261855Z"
}
```

### GET /api/funding-orders/{fundingId}
Returns the current status of a funding order. The Stripe client secret is never returned here.

**Auth:** Bearer JWT (ownership check)

**Response (200 OK) — while pending:**
```json
{
  "fundingId": "26841929-ab9a-4fe9-b7c1-b9181d7a0b5f",
  "walletId": 16,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TOuek3nnME1dfOB11rVQqb5",
  "createdAt": "2026-04-22T06:53:04.099262Z"
}
```

**Response (200 OK) — after treasury transfer completes:**
```json
{
  "fundingId": "26841929-ab9a-4fe9-b7c1-b9181d7a0b5f",
  "walletId": 16,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TOuek3nnME1dfOB11rVQqb5",
  "createdAt": "2026-04-22T06:53:04.099262Z"
}
```

**Funding status progression:** `PENDING` → `PAYMENT_CONFIRMED` → `FUNDED`

### POST /api/funding-orders/{fundingId}/refund
Refunds a FUNDED order via Stripe.

**Auth:** Bearer JWT (ownership check)

**Response (200 OK):**
```json
{
  "fundingId": "26841929-ab9a-4fe9-b7c1-b9181d7a0b5f",
  "walletId": 16,
  "amountUsdc": 1.0,
  "status": "REFUNDED",
  "stripePaymentIntentId": "pi_3TOuek3nnME1dfOB11rVQqb5",
  "createdAt": "2026-04-22T06:53:04.099262Z"
}
```

---

## Remittances

### POST /api/remittances
Initiates a new USD to INR remittance with a locked FX rate. Sender is derived from the JWT principal.

**Auth:** Bearer JWT

**Request:**
```json
{
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

**Response (201 Created):**
```json
{
  "id": 10,
  "remittanceId": "8ce317d2-639e-4054-bcae-204706dc2c9a",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "82a56560-ad6f-4b97-a26d-12c36b722f58",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-22T06:53:15.632889681Z",
  "updatedAt": "2026-04-22T06:53:15.646040707Z",
  "expiresAt": null
}
```

### GET /api/remittances/{remittanceId}
Retrieves a remittance by its unique identifier.

**Auth:** Bearer JWT (ownership check — remittance must belong to authenticated user)

**Response (200 OK) — ESCROWED:**
```json
{
  "id": 10,
  "remittanceId": "8ce317d2-639e-4054-bcae-204706dc2c9a",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "82a56560-ad6f-4b97-a26d-12c36b722f58",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-22T06:53:15.632890Z",
  "updatedAt": "2026-04-22T06:53:20.506303Z",
  "expiresAt": null
}
```

**Response (200 OK) — DELIVERED:**
```json
{
  "id": 10,
  "remittanceId": "8ce317d2-639e-4054-bcae-204706dc2c9a",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "82a56560-ad6f-4b97-a26d-12c36b722f58",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-22T06:53:15.632890Z",
  "updatedAt": "2026-04-22T06:53:27.715133Z",
  "expiresAt": null
}
```

**Remittance status progression:** `INITIATED` → `ESCROWED` → `CLAIMED` → `DELIVERED`

### GET /api/remittances
Returns a paginated list of remittances for the authenticated user.

**Auth:** Bearer JWT

**Query params:** Standard Spring Pageable (`page`, `size`, `sort`)

**Response (200 OK):**
```json
{
  "content": [ /* array of RemittanceResponse */ ],
  "pageable": { ... },
  "totalElements": 1,
  "totalPages": 1
}
```

---

## Claims

### GET /api/claims/{token}
Returns claim details for a remittance. Used by the web claim page.

**Auth:** Public (token-scoped)

**Response (200 OK):**
```json
{
  "remittanceId": "8ce317d2-639e-4054-bcae-204706dc2c9a",
  "senderDisplayName": "e2e-7d4718ba",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-24T06:53:15.642463Z"
}
```

### POST /api/claims/{token}
Submits a claim with the recipient's UPI ID. Triggers INR disbursement.

**Auth:** Public (token-scoped)

**Request:**
```json
{
  "upiId": "test@upi"
}
```

**Response (200 OK):**
```json
{
  "remittanceId": "8ce317d2-639e-4054-bcae-204706dc2c9a",
  "senderDisplayName": "e2e-7d4718ba",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-24T06:53:15.642463Z"
}
```

---

## Webhooks

### POST /webhooks/stripe
Receives Stripe webhook events (e.g., `payment_intent.succeeded`).

**Auth:** Public (verified by Stripe signature)

---

## Error Responses

All errors follow this shape:

```json
{
  "errorCode": "SP-XXXX",
  "message": "SP-XXXX: Human-readable description",
  "timestamp": "2026-04-22T06:48:38.980157337Z",
  "path": "/api/wallets"
}
```

| Code | Meaning |
|------|---------|
| SP-0001 | Wallet already exists for this user |
| SP-0010 | Wallet not found |
| SP-0020 | Remittance not found |
| SP-0032 | Invalid ID token |
| SP-0033 | Email not verified |
| SP-0034 | Unsupported auth provider |
| SP-0035 | Invalid refresh token |
| SP-0036 | Refresh token expired |
| SP-0040 | Authentication required |

---

## End-to-End Flow

```
1. POST /api/auth/social      → access token + wallet (production)
   — or —
   POST /api/wallets           → wallet (dev/e2e)

2. POST /api/wallets/{id}/fund → funding order + Stripe client secret
   (poll) GET /api/funding-orders/{fundingId} → status=FUNDED

3. POST /api/remittances       → remittance + claim token
   (poll) GET /api/remittances/{id} → status=ESCROWED

4. GET  /api/claims/{token}    → claim details (recipient's web page)
5. POST /api/claims/{token}    → submit UPI ID
   (poll) GET /api/remittances/{id} → status=DELIVERED
```

**Typical latency:** 22–60s end-to-end per customer (MPC keygen ~5–30s, Stripe ~2s, on-chain escrow ~5s, disbursement ~3s).
