# 10-Customer Full E2E Run — $1.00 USDC per customer

Generated: 2026-04-22 11:30:33 UTC

## Summary

- Customers attempted: 1
- Passed: 1
- Failed: 0
- Per-customer amount: $1.00 USDC

| # | userId | walletId | address | fundingId | remittanceId | final | USDC after fund | payout_id | elapsed (s) | PASS |
|---|--------|----------|---------|-----------|--------------|-------|-----------------|-----------|-------------|------|
| 1 | `829f2ae9-1eb0-4ab0-87dd-2fe5fd85284c` | 27 | `YviLrjMGPYxL6wtxka9YiV3zKvXjzodQynMnrPJWNUB` | `af965dda-3a4b-40cc-8415-066dccc55f5f` | `352e73d4-bfc6-4269-b0e9-560b10899180` | DELIVERED | 1 | `pout_wm_y5p1vg6mpk3w` | 24.9 | ✅ |

## Per-customer request / response log

### Customer 1 — `829f2ae9-1eb0-4ab0-87dd-2fe5fd85284c`

- walletId: `27`  solanaAddress: `YviLrjMGPYxL6wtxka9YiV3zKvXjzodQynMnrPJWNUB`
- fundingId: `af965dda-3a4b-40cc-8415-066dccc55f5f`  paymentIntentId: `pi_3TOyyt3nnME1dfOB1OzXdrrF`
- remittanceId: `352e73d4-bfc6-4269-b0e9-560b10899180`  claimTokenId: `e881c665-6823-4194-af1b-191dcc03da73`
- final status: **DELIVERED**  elapsed: 24.9s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 63
- Payout: id=`pout_wm_y5p1vg6mpk3w` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "829f2ae9-1eb0-4ab0-87dd-2fe5fd85284c"
}
```

Response:
```json
{
  "id": 27,
  "solanaAddress": "YviLrjMGPYxL6wtxka9YiV3zKvXjzodQynMnrPJWNUB",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-22T11:30:09.262967270Z",
  "updatedAt": "2026-04-22T11:30:09.262967270Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/27/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "af965dda-3a4b-40cc-8415-066dccc55f5f",
  "walletId": 27,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TOyyt3nnME1dfOB1OzXdrrF",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-22T11:30:09.462987601Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/af965dda-3a4b-40cc-8415-066dccc55f5f`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "af965dda-3a4b-40cc-8415-066dccc55f5f",
  "walletId": 27,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TOyyt3nnME1dfOB1OzXdrrF",
  "createdAt": "2026-04-22T11:30:09.462988Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 20,
  "remittanceId": "352e73d4-bfc6-4269-b0e9-560b10899180",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "e881c665-6823-4194-af1b-191dcc03da73",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-22T11:30:20.056045620Z",
  "updatedAt": "2026-04-22T11:30:20.074055322Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/352e73d4-bfc6-4269-b0e9-560b10899180`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 20,
  "remittanceId": "352e73d4-bfc6-4269-b0e9-560b10899180",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "e881c665-6823-4194-af1b-191dcc03da73",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-22T11:30:20.056046Z",
  "updatedAt": "2026-04-22T11:30:25.011906Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/e881c665-6823-4194-af1b-191dcc03da73`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "352e73d4-bfc6-4269-b0e9-560b10899180",
  "senderDisplayName": "e2e-829f2ae9",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-24T11:30:20.072076Z"
}
```

#### 7-submit-claim

`POST /api/claims/e881c665-6823-4194-af1b-191dcc03da73`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "352e73d4-bfc6-4269-b0e9-560b10899180",
  "senderDisplayName": "e2e-829f2ae9",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-24T11:30:20.072076Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/352e73d4-bfc6-4269-b0e9-560b10899180`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 20,
  "remittanceId": "352e73d4-bfc6-4269-b0e9-560b10899180",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "e881c665-6823-4194-af1b-191dcc03da73",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-22T11:30:20.056046Z",
  "updatedAt": "2026-04-22T11:30:31.800042Z",
  "expiresAt": null
}
```

---
