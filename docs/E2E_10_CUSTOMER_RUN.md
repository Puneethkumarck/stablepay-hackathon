# 10-Customer Full E2E Run — $1.00 USDC per customer

Generated: 2026-04-22 12:50:55 UTC

## Summary

- Customers attempted: 1
- Passed: 1
- Failed: 0
- Per-customer amount: $1.00 USDC

| # | userId | walletId | address | fundingId | remittanceId | final | USDC after fund | payout_id | elapsed (s) | PASS |
|---|--------|----------|---------|-----------|--------------|-------|-----------------|-----------|-------------|------|
| 1 | `d616746d-3a96-4d07-85fb-0e7bec333406` | 29 | `iJVSrrFcMxxvu85oeDYCT6DyCS2pmAYvqrCRkuSW9JJ` | `139a9810-997b-4f4a-b4e4-ddcb9e9dd626` | `b5dbdd21-8735-46bb-8a7e-637148b21ddd` | DELIVERED | 1 | `pout_wm_kxggkuvvttr8` | 27.0 | ✅ |

## Per-customer request / response log

### Customer 1 — `d616746d-3a96-4d07-85fb-0e7bec333406`

- walletId: `29`  solanaAddress: `iJVSrrFcMxxvu85oeDYCT6DyCS2pmAYvqrCRkuSW9JJ`
- fundingId: `139a9810-997b-4f4a-b4e4-ddcb9e9dd626`  paymentIntentId: `pi_3TP0Eb3nnME1dfOB075HbZoi`
- remittanceId: `b5dbdd21-8735-46bb-8a7e-637148b21ddd`  claimTokenId: `6b63c70a-57b6-4b38-b20c-6edfbce5262b`
- final status: **DELIVERED**  elapsed: 27.0s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 65
- Payout: id=`pout_wm_kxggkuvvttr8` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "d616746d-3a96-4d07-85fb-0e7bec333406"
}
```

Response:
```json
{
  "id": 29,
  "solanaAddress": "iJVSrrFcMxxvu85oeDYCT6DyCS2pmAYvqrCRkuSW9JJ",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-22T12:50:28.568477359Z",
  "updatedAt": "2026-04-22T12:50:28.568477359Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/29/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "139a9810-997b-4f4a-b4e4-ddcb9e9dd626",
  "walletId": 29,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TP0Eb3nnME1dfOB075HbZoi",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-22T12:50:28.598815756Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/139a9810-997b-4f4a-b4e4-ddcb9e9dd626`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "139a9810-997b-4f4a-b4e4-ddcb9e9dd626",
  "walletId": 29,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TP0Eb3nnME1dfOB075HbZoi",
  "createdAt": "2026-04-22T12:50:28.598816Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "recipientPhone": "+919620038444",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 22,
  "remittanceId": "b5dbdd21-8735-46bb-8a7e-637148b21ddd",
  "recipientPhone": "+919620038444",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "6b63c70a-57b6-4b38-b20c-6edfbce5262b",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-22T12:50:38.671874873Z",
  "updatedAt": "2026-04-22T12:50:38.677255020Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/b5dbdd21-8735-46bb-8a7e-637148b21ddd`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 22,
  "remittanceId": "b5dbdd21-8735-46bb-8a7e-637148b21ddd",
  "recipientPhone": "+919620038444",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "6b63c70a-57b6-4b38-b20c-6edfbce5262b",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-22T12:50:38.671875Z",
  "updatedAt": "2026-04-22T12:50:43.714644Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/6b63c70a-57b6-4b38-b20c-6edfbce5262b`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "b5dbdd21-8735-46bb-8a7e-637148b21ddd",
  "senderDisplayName": "e2e-d616746d",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-24T12:50:38.676326Z"
}
```

#### 7-submit-claim

`POST /api/claims/6b63c70a-57b6-4b38-b20c-6edfbce5262b`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "b5dbdd21-8735-46bb-8a7e-637148b21ddd",
  "senderDisplayName": "e2e-d616746d",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-24T12:50:38.676326Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/b5dbdd21-8735-46bb-8a7e-637148b21ddd`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 22,
  "remittanceId": "b5dbdd21-8735-46bb-8a7e-637148b21ddd",
  "recipientPhone": "+919620038444",
  "amountUsdc": 1.0,
  "amountInr": 93.61,
  "fxRate": 93.605785,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "6b63c70a-57b6-4b38-b20c-6edfbce5262b",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-22T12:50:38.671875Z",
  "updatedAt": "2026-04-22T12:50:51.337352Z",
  "expiresAt": null
}
```

---
