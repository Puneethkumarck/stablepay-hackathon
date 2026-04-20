# 10-Customer Full E2E Run — $1.00 USDC per customer

Generated: 2026-04-19 19:08:26 UTC

## Summary

- Customers attempted: 10
- Passed: 6
- Failed: 4
- Per-customer amount: $1.00 USDC

| # | userId | walletId | address | fundingId | remittanceId | final | USDC after fund | elapsed (s) | PASS |
|---|--------|----------|---------|-----------|--------------|-------|-----------------|-------------|------|
| 1 | `e2e-10x-1776625135759-1` | 19 | `Gvw75z4HiSAuuysi5kLXUfpDCNptBv4f9pSYqwSAqsgv` | `b2c8a29a-fcca-487f-979b-bf355c82faeb` | `None` | - | - | 95.0 | ❌ fund not FUNDED: status=PAYMENT_CONFIRMED |
| 2 | `e2e-10x-1776625230787-2` | 20 | `EJ5nmbwLXZtVZqcRriH4ZzZHnvPt8p31BFxNYdjonfrs` | `e10e7206-4523-4156-ab1b-20a6bb92dbed` | `None` | - | - | 92.9 | ❌ fund not FUNDED: status=PAYMENT_CONFIRMED |
| 3 | `e2e-10x-1776625323684-3` | 21 | `7ZWifM27D53pukKjEDkqNHZiZUuUjVXEJcqcfiaEaseN` | `a5f32227-179e-4181-9f81-25cdcbbd367e` | `None` | - | - | 125.3 | ❌ fund not FUNDED: status=PAYMENT_CONFIRMED |
| 4 | `e2e-10x-1776625448980-4` | 22 | `26u5KveR5nty1h4oyXsEqvJ5arBfox9Zp8k1Eop9tgyi` | `a8f04346-bba5-4bf3-905a-f8746a959921` | `None` | - | - | 93.0 | ❌ fund not FUNDED: status=PAYMENT_CONFIRMED |
| 5 | `e2e-10x-1776625541994-5` | 23 | `BHBm4cN3ZveqboS2i93nNvsgGkYU8tfKDpj1nTcMEJth` | `bfc25787-1cfd-41d9-906c-b18493afa79d` | `b26db8dd-8330-4071-8b30-dde2051dcb9c` | DELIVERED | 1 | 23.4 | ✅ |
| 6 | `e2e-10x-1776625565425-6` | 24 | `Hp3jRdvzsQkC9icQmZJSTqQ2Jty516XsLd6TEF3D2YK1` | `edf93bd6-8407-46a0-a995-f9d2705e2cbe` | `5c3ef5af-8e44-42f3-97d0-0d64adb728d2` | DELIVERED | 1 | 22.1 | ✅ |
| 7 | `e2e-10x-1776625587534-7` | 25 | `2EJTtS636CELwrdh6oBzF37LGMgYsNe9vu3BCxjxgvLP` | `8d16860c-ae69-499a-aad3-74a5b6cea6a3` | `29e02376-179f-43eb-905c-0b2272bc029f` | DELIVERED | 1 | 22.3 | ✅ |
| 8 | `e2e-10x-1776625609866-8` | 26 | `EvuvHjCFj6bNpeM9e3q6o6T6GE4KzvHaDgiS5tB4WjQ6` | `12aa01d4-9eba-4e87-9a0d-f69290de94fb` | `8925ec59-c663-4298-a627-a6736cabc5e5` | DELIVERED | 1 | 22.3 | ✅ |
| 9 | `e2e-10x-1776625632190-9` | 27 | `F2muFfMgmKXfqkMvS8V5CZgPNYmCxWR6bxvJs2MRYBg2` | `99a0ab3a-18ab-4664-9ac2-23f5e896d24e` | `23c1a361-dea3-4f9f-870b-0de778ef4355` | DELIVERED | 1 | 22.3 | ✅ |
| 10 | `e2e-10x-1776625654515-10` | 28 | `CAWjqf8mm5XRicmmWYYmNEaNYpJhAvoieVdTXwW1Ehre` | `4860ae81-8fb1-4156-a782-0fc7cdeee786` | `cf5346df-80ba-4f6c-81d7-7a9dd2fa1418` | DELIVERED | 1 | 52.4 | ✅ |

## Per-customer request / response log

### Customer 1 — `e2e-10x-1776625135759-1`

- walletId: `19`  solanaAddress: `Gvw75z4HiSAuuysi5kLXUfpDCNptBv4f9pSYqwSAqsgv`
- fundingId: `b2c8a29a-fcca-487f-979b-bf355c82faeb`  paymentIntentId: `pi_3TO0YZ3nnME1dfOB0dz1SICr`
- remittanceId: `None`  claimTokenId: `None`
- final status: **None**  elapsed: 95.0s  **FAIL: fund not FUNDED: status=PAYMENT_CONFIRMED**
- On-chain USDC after fund: None
- Claim authority USDC after claim: None

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625135759-1"
}
```

Response:
```json
{
  "id": 19,
  "userId": "e2e-10x-1776625135759-1",
  "solanaAddress": "Gvw75z4HiSAuuysi5kLXUfpDCNptBv4f9pSYqwSAqsgv",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T18:58:57.518704602Z",
  "updatedAt": "2026-04-19T18:58:57.518704602Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/19/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "b2c8a29a-fcca-487f-979b-bf355c82faeb",
  "walletId": 19,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0YZ3nnME1dfOB0dz1SICr",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T18:58:57.689975209Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/b2c8a29a-fcca-487f-979b-bf355c82faeb`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "b2c8a29a-fcca-487f-979b-bf355c82faeb",
  "walletId": 19,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0YZ3nnME1dfOB0dz1SICr",
  "createdAt": "2026-04-19T18:58:57.689975Z"
}
```

---

### Customer 2 — `e2e-10x-1776625230787-2`

- walletId: `20`  solanaAddress: `EJ5nmbwLXZtVZqcRriH4ZzZHnvPt8p31BFxNYdjonfrs`
- fundingId: `e10e7206-4523-4156-ab1b-20a6bb92dbed`  paymentIntentId: `pi_3TO0a43nnME1dfOB0MrYiKky`
- remittanceId: `None`  claimTokenId: `None`
- final status: **None**  elapsed: 92.9s  **FAIL: fund not FUNDED: status=PAYMENT_CONFIRMED**
- On-chain USDC after fund: None
- Claim authority USDC after claim: None

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625230787-2"
}
```

Response:
```json
{
  "id": 20,
  "userId": "e2e-10x-1776625230787-2",
  "solanaAddress": "EJ5nmbwLXZtVZqcRriH4ZzZHnvPt8p31BFxNYdjonfrs",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T19:00:30.951370297Z",
  "updatedAt": "2026-04-19T19:00:30.951370297Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/20/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "e10e7206-4523-4156-ab1b-20a6bb92dbed",
  "walletId": 20,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0a43nnME1dfOB0MrYiKky",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T19:00:30.985332093Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/e10e7206-4523-4156-ab1b-20a6bb92dbed`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "e10e7206-4523-4156-ab1b-20a6bb92dbed",
  "walletId": 20,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0a43nnME1dfOB0MrYiKky",
  "createdAt": "2026-04-19T19:00:30.985332Z"
}
```

---

### Customer 3 — `e2e-10x-1776625323684-3`

- walletId: `21`  solanaAddress: `7ZWifM27D53pukKjEDkqNHZiZUuUjVXEJcqcfiaEaseN`
- fundingId: `a5f32227-179e-4181-9f81-25cdcbbd367e`  paymentIntentId: `pi_3TO0c53nnME1dfOB0tm13xXf`
- remittanceId: `None`  claimTokenId: `None`
- final status: **None**  elapsed: 125.3s  **FAIL: fund not FUNDED: status=PAYMENT_CONFIRMED**
- On-chain USDC after fund: None
- Claim authority USDC after claim: None

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625323684-3"
}
```

Response:
```json
{
  "id": 21,
  "userId": "e2e-10x-1776625323684-3",
  "solanaAddress": "7ZWifM27D53pukKjEDkqNHZiZUuUjVXEJcqcfiaEaseN",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T19:02:35.997741733Z",
  "updatedAt": "2026-04-19T19:02:35.997741733Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/21/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "a5f32227-179e-4181-9f81-25cdcbbd367e",
  "walletId": 21,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0c53nnME1dfOB0tm13xXf",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T19:02:36.044561385Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/a5f32227-179e-4181-9f81-25cdcbbd367e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "a5f32227-179e-4181-9f81-25cdcbbd367e",
  "walletId": 21,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0c53nnME1dfOB0tm13xXf",
  "createdAt": "2026-04-19T19:02:36.044561Z"
}
```

---

### Customer 4 — `e2e-10x-1776625448980-4`

- walletId: `22`  solanaAddress: `26u5KveR5nty1h4oyXsEqvJ5arBfox9Zp8k1Eop9tgyi`
- fundingId: `a8f04346-bba5-4bf3-905a-f8746a959921`  paymentIntentId: `pi_3TO0da3nnME1dfOB1VxP75xo`
- remittanceId: `None`  claimTokenId: `None`
- final status: **None**  elapsed: 93.0s  **FAIL: fund not FUNDED: status=PAYMENT_CONFIRMED**
- On-chain USDC after fund: None
- Claim authority USDC after claim: None

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625448980-4"
}
```

Response:
```json
{
  "id": 22,
  "userId": "e2e-10x-1776625448980-4",
  "solanaAddress": "26u5KveR5nty1h4oyXsEqvJ5arBfox9Zp8k1Eop9tgyi",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T19:04:09.207185794Z",
  "updatedAt": "2026-04-19T19:04:09.207185794Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/22/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "a8f04346-bba5-4bf3-905a-f8746a959921",
  "walletId": 22,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0da3nnME1dfOB1VxP75xo",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T19:04:09.248421368Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/a8f04346-bba5-4bf3-905a-f8746a959921`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "a8f04346-bba5-4bf3-905a-f8746a959921",
  "walletId": 22,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0da3nnME1dfOB1VxP75xo",
  "createdAt": "2026-04-19T19:04:09.248421Z"
}
```

---

### Customer 5 — `e2e-10x-1776625541994-5`

- walletId: `23`  solanaAddress: `BHBm4cN3ZveqboS2i93nNvsgGkYU8tfKDpj1nTcMEJth`
- fundingId: `bfc25787-1cfd-41d9-906c-b18493afa79d`  paymentIntentId: `pi_3TO0f53nnME1dfOB0HeZyeRu`
- remittanceId: `b26db8dd-8330-4071-8b30-dde2051dcb9c`  claimTokenId: `188d3d81-4871-4751-959a-ca13bddb4ae5`
- final status: **DELIVERED**  elapsed: 23.4s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 38

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625541994-5"
}
```

Response:
```json
{
  "id": 23,
  "userId": "e2e-10x-1776625541994-5",
  "solanaAddress": "BHBm4cN3ZveqboS2i93nNvsgGkYU8tfKDpj1nTcMEJth",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T19:05:42.160330871Z",
  "updatedAt": "2026-04-19T19:05:42.160330871Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/23/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "bfc25787-1cfd-41d9-906c-b18493afa79d",
  "walletId": 23,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0f53nnME1dfOB0HeZyeRu",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T19:05:42.182085132Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/bfc25787-1cfd-41d9-906c-b18493afa79d`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "bfc25787-1cfd-41d9-906c-b18493afa79d",
  "walletId": 23,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TO0f53nnME1dfOB0HeZyeRu",
  "createdAt": "2026-04-19T19:05:42.182085Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776625541994-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 13,
  "remittanceId": "b26db8dd-8330-4071-8b30-dde2051dcb9c",
  "senderId": "e2e-10x-1776625541994-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "188d3d81-4871-4751-959a-ca13bddb4ae5",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:05:52.315181754Z",
  "updatedAt": "2026-04-19T19:05:52.330885107Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/b26db8dd-8330-4071-8b30-dde2051dcb9c`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 13,
  "remittanceId": "b26db8dd-8330-4071-8b30-dde2051dcb9c",
  "senderId": "e2e-10x-1776625541994-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "188d3d81-4871-4751-959a-ca13bddb4ae5",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:05:52.315182Z",
  "updatedAt": "2026-04-19T19:05:56.930484Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/188d3d81-4871-4751-959a-ca13bddb4ae5`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "b26db8dd-8330-4071-8b30-dde2051dcb9c",
  "senderId": "e2e-10x-1776625541994-5",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T19:05:52.325623Z"
}
```

#### 7-submit-claim

`POST /api/claims/188d3d81-4871-4751-959a-ca13bddb4ae5`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "b26db8dd-8330-4071-8b30-dde2051dcb9c",
  "senderId": "e2e-10x-1776625541994-5",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T19:05:52.325623Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/b26db8dd-8330-4071-8b30-dde2051dcb9c`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 13,
  "remittanceId": "b26db8dd-8330-4071-8b30-dde2051dcb9c",
  "senderId": "e2e-10x-1776625541994-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "188d3d81-4871-4751-959a-ca13bddb4ae5",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:05:52.315182Z",
  "updatedAt": "2026-04-19T19:06:03.622223Z",
  "expiresAt": null
}
```

---

### Customer 6 — `e2e-10x-1776625565425-6`

- walletId: `24`  solanaAddress: `Hp3jRdvzsQkC9icQmZJSTqQ2Jty516XsLd6TEF3D2YK1`
- fundingId: `edf93bd6-8407-46a0-a995-f9d2705e2cbe`  paymentIntentId: `pi_3TO0fS3nnME1dfOB1T93KrgL`
- remittanceId: `5c3ef5af-8e44-42f3-97d0-0d64adb728d2`  claimTokenId: `5d7849b3-eef4-4105-a2a1-60b6b9c61209`
- final status: **DELIVERED**  elapsed: 22.1s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 39

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625565425-6"
}
```

Response:
```json
{
  "id": 24,
  "userId": "e2e-10x-1776625565425-6",
  "solanaAddress": "Hp3jRdvzsQkC9icQmZJSTqQ2Jty516XsLd6TEF3D2YK1",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T19:06:05.572299827Z",
  "updatedAt": "2026-04-19T19:06:05.572299827Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/24/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "edf93bd6-8407-46a0-a995-f9d2705e2cbe",
  "walletId": 24,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0fS3nnME1dfOB1T93KrgL",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T19:06:05.589472678Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/edf93bd6-8407-46a0-a995-f9d2705e2cbe`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "edf93bd6-8407-46a0-a995-f9d2705e2cbe",
  "walletId": 24,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TO0fS3nnME1dfOB1T93KrgL",
  "createdAt": "2026-04-19T19:06:05.589473Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776625565425-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 14,
  "remittanceId": "5c3ef5af-8e44-42f3-97d0-0d64adb728d2",
  "senderId": "e2e-10x-1776625565425-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "5d7849b3-eef4-4105-a2a1-60b6b9c61209",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:06:14.316230063Z",
  "updatedAt": "2026-04-19T19:06:14.321558639Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/5c3ef5af-8e44-42f3-97d0-0d64adb728d2`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 14,
  "remittanceId": "5c3ef5af-8e44-42f3-97d0-0d64adb728d2",
  "senderId": "e2e-10x-1776625565425-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "5d7849b3-eef4-4105-a2a1-60b6b9c61209",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:06:14.316230Z",
  "updatedAt": "2026-04-19T19:06:18.909359Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/5d7849b3-eef4-4105-a2a1-60b6b9c61209`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "5c3ef5af-8e44-42f3-97d0-0d64adb728d2",
  "senderId": "e2e-10x-1776625565425-6",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T19:06:14.319893Z"
}
```

#### 7-submit-claim

`POST /api/claims/5d7849b3-eef4-4105-a2a1-60b6b9c61209`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "5c3ef5af-8e44-42f3-97d0-0d64adb728d2",
  "senderId": "e2e-10x-1776625565425-6",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T19:06:14.319893Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/5c3ef5af-8e44-42f3-97d0-0d64adb728d2`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 14,
  "remittanceId": "5c3ef5af-8e44-42f3-97d0-0d64adb728d2",
  "senderId": "e2e-10x-1776625565425-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "5d7849b3-eef4-4105-a2a1-60b6b9c61209",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:06:14.316230Z",
  "updatedAt": "2026-04-19T19:06:25.791184Z",
  "expiresAt": null
}
```

---

### Customer 7 — `e2e-10x-1776625587534-7`

- walletId: `25`  solanaAddress: `2EJTtS636CELwrdh6oBzF37LGMgYsNe9vu3BCxjxgvLP`
- fundingId: `8d16860c-ae69-499a-aad3-74a5b6cea6a3`  paymentIntentId: `pi_3TO0fp3nnME1dfOB1doaELNR`
- remittanceId: `29e02376-179f-43eb-905c-0b2272bc029f`  claimTokenId: `70f4b9ba-c26a-49f1-873f-498f8483bebf`
- final status: **DELIVERED**  elapsed: 22.3s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 40

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625587534-7"
}
```

Response:
```json
{
  "id": 25,
  "userId": "e2e-10x-1776625587534-7",
  "solanaAddress": "2EJTtS636CELwrdh6oBzF37LGMgYsNe9vu3BCxjxgvLP",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T19:06:27.646606450Z",
  "updatedAt": "2026-04-19T19:06:27.646606450Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/25/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "8d16860c-ae69-499a-aad3-74a5b6cea6a3",
  "walletId": 25,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0fp3nnME1dfOB1doaELNR",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T19:06:27.659964640Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/8d16860c-ae69-499a-aad3-74a5b6cea6a3`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "8d16860c-ae69-499a-aad3-74a5b6cea6a3",
  "walletId": 25,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TO0fp3nnME1dfOB1doaELNR",
  "createdAt": "2026-04-19T19:06:27.659965Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776625587534-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 15,
  "remittanceId": "29e02376-179f-43eb-905c-0b2272bc029f",
  "senderId": "e2e-10x-1776625587534-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "70f4b9ba-c26a-49f1-873f-498f8483bebf",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:06:36.814173551Z",
  "updatedAt": "2026-04-19T19:06:36.822497664Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/29e02376-179f-43eb-905c-0b2272bc029f`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 15,
  "remittanceId": "29e02376-179f-43eb-905c-0b2272bc029f",
  "senderId": "e2e-10x-1776625587534-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "70f4b9ba-c26a-49f1-873f-498f8483bebf",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:06:36.814174Z",
  "updatedAt": "2026-04-19T19:06:41.809937Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/70f4b9ba-c26a-49f1-873f-498f8483bebf`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "29e02376-179f-43eb-905c-0b2272bc029f",
  "senderId": "e2e-10x-1776625587534-7",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T19:06:36.820068Z"
}
```

#### 7-submit-claim

`POST /api/claims/70f4b9ba-c26a-49f1-873f-498f8483bebf`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "29e02376-179f-43eb-905c-0b2272bc029f",
  "senderId": "e2e-10x-1776625587534-7",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T19:06:36.820068Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/29e02376-179f-43eb-905c-0b2272bc029f`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 15,
  "remittanceId": "29e02376-179f-43eb-905c-0b2272bc029f",
  "senderId": "e2e-10x-1776625587534-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "70f4b9ba-c26a-49f1-873f-498f8483bebf",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:06:36.814174Z",
  "updatedAt": "2026-04-19T19:06:48.098718Z",
  "expiresAt": null
}
```

---

### Customer 8 — `e2e-10x-1776625609866-8`

- walletId: `26`  solanaAddress: `EvuvHjCFj6bNpeM9e3q6o6T6GE4KzvHaDgiS5tB4WjQ6`
- fundingId: `12aa01d4-9eba-4e87-9a0d-f69290de94fb`  paymentIntentId: `pi_3TO0gB3nnME1dfOB1fg2vQKt`
- remittanceId: `8925ec59-c663-4298-a627-a6736cabc5e5`  claimTokenId: `3485830d-5f46-40fd-b70e-c619e30bdc4c`
- final status: **DELIVERED**  elapsed: 22.3s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 41

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625609866-8"
}
```

Response:
```json
{
  "id": 26,
  "userId": "e2e-10x-1776625609866-8",
  "solanaAddress": "EvuvHjCFj6bNpeM9e3q6o6T6GE4KzvHaDgiS5tB4WjQ6",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T19:06:49.969699599Z",
  "updatedAt": "2026-04-19T19:06:49.969699599Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/26/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "12aa01d4-9eba-4e87-9a0d-f69290de94fb",
  "walletId": 26,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0gB3nnME1dfOB1fg2vQKt",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T19:06:49.988436740Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/12aa01d4-9eba-4e87-9a0d-f69290de94fb`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "12aa01d4-9eba-4e87-9a0d-f69290de94fb",
  "walletId": 26,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TO0gB3nnME1dfOB1fg2vQKt",
  "createdAt": "2026-04-19T19:06:49.988437Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776625609866-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 16,
  "remittanceId": "8925ec59-c663-4298-a627-a6736cabc5e5",
  "senderId": "e2e-10x-1776625609866-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "3485830d-5f46-40fd-b70e-c619e30bdc4c",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:06:58.777742412Z",
  "updatedAt": "2026-04-19T19:06:58.787357774Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/8925ec59-c663-4298-a627-a6736cabc5e5`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 16,
  "remittanceId": "8925ec59-c663-4298-a627-a6736cabc5e5",
  "senderId": "e2e-10x-1776625609866-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "3485830d-5f46-40fd-b70e-c619e30bdc4c",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:06:58.777742Z",
  "updatedAt": "2026-04-19T19:07:03.492727Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/3485830d-5f46-40fd-b70e-c619e30bdc4c`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "8925ec59-c663-4298-a627-a6736cabc5e5",
  "senderId": "e2e-10x-1776625609866-8",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T19:06:58.781537Z"
}
```

#### 7-submit-claim

`POST /api/claims/3485830d-5f46-40fd-b70e-c619e30bdc4c`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "8925ec59-c663-4298-a627-a6736cabc5e5",
  "senderId": "e2e-10x-1776625609866-8",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T19:06:58.781537Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/8925ec59-c663-4298-a627-a6736cabc5e5`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 16,
  "remittanceId": "8925ec59-c663-4298-a627-a6736cabc5e5",
  "senderId": "e2e-10x-1776625609866-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "3485830d-5f46-40fd-b70e-c619e30bdc4c",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:06:58.777742Z",
  "updatedAt": "2026-04-19T19:07:09.774656Z",
  "expiresAt": null
}
```

---

### Customer 9 — `e2e-10x-1776625632190-9`

- walletId: `27`  solanaAddress: `F2muFfMgmKXfqkMvS8V5CZgPNYmCxWR6bxvJs2MRYBg2`
- fundingId: `99a0ab3a-18ab-4664-9ac2-23f5e896d24e`  paymentIntentId: `pi_3TO0gX3nnME1dfOB065tNPzK`
- remittanceId: `23c1a361-dea3-4f9f-870b-0de778ef4355`  claimTokenId: `499a5e65-84a8-4ddb-8258-d8c60dda9f88`
- final status: **DELIVERED**  elapsed: 22.3s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 42

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625632190-9"
}
```

Response:
```json
{
  "id": 27,
  "userId": "e2e-10x-1776625632190-9",
  "solanaAddress": "F2muFfMgmKXfqkMvS8V5CZgPNYmCxWR6bxvJs2MRYBg2",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T19:07:12.344890426Z",
  "updatedAt": "2026-04-19T19:07:12.344890426Z"
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
  "fundingId": "99a0ab3a-18ab-4664-9ac2-23f5e896d24e",
  "walletId": 27,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0gX3nnME1dfOB065tNPzK",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T19:07:12.373961260Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/99a0ab3a-18ab-4664-9ac2-23f5e896d24e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "99a0ab3a-18ab-4664-9ac2-23f5e896d24e",
  "walletId": 27,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TO0gX3nnME1dfOB065tNPzK",
  "createdAt": "2026-04-19T19:07:12.373961Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776625632190-9",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 17,
  "remittanceId": "23c1a361-dea3-4f9f-870b-0de778ef4355",
  "senderId": "e2e-10x-1776625632190-9",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "499a5e65-84a8-4ddb-8258-d8c60dda9f88",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:07:21.424828857Z",
  "updatedAt": "2026-04-19T19:07:21.438793004Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/23c1a361-dea3-4f9f-870b-0de778ef4355`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 17,
  "remittanceId": "23c1a361-dea3-4f9f-870b-0de778ef4355",
  "senderId": "e2e-10x-1776625632190-9",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "499a5e65-84a8-4ddb-8258-d8c60dda9f88",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:07:21.424829Z",
  "updatedAt": "2026-04-19T19:07:26.434834Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/499a5e65-84a8-4ddb-8258-d8c60dda9f88`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "23c1a361-dea3-4f9f-870b-0de778ef4355",
  "senderId": "e2e-10x-1776625632190-9",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T19:07:21.435853Z"
}
```

#### 7-submit-claim

`POST /api/claims/499a5e65-84a8-4ddb-8258-d8c60dda9f88`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "23c1a361-dea3-4f9f-870b-0de778ef4355",
  "senderId": "e2e-10x-1776625632190-9",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T19:07:21.435853Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/23c1a361-dea3-4f9f-870b-0de778ef4355`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 17,
  "remittanceId": "23c1a361-dea3-4f9f-870b-0de778ef4355",
  "senderId": "e2e-10x-1776625632190-9",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "499a5e65-84a8-4ddb-8258-d8c60dda9f88",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:07:21.424829Z",
  "updatedAt": "2026-04-19T19:07:32.502073Z",
  "expiresAt": null
}
```

---

### Customer 10 — `e2e-10x-1776625654515-10`

- walletId: `28`  solanaAddress: `CAWjqf8mm5XRicmmWYYmNEaNYpJhAvoieVdTXwW1Ehre`
- fundingId: `4860ae81-8fb1-4156-a782-0fc7cdeee786`  paymentIntentId: `pi_3TO0gu3nnME1dfOB1MBgLKRc`
- remittanceId: `cf5346df-80ba-4f6c-81d7-7a9dd2fa1418`  claimTokenId: `6a931f2e-c22e-4c60-b2c5-ada5d4872c5d`
- final status: **DELIVERED**  elapsed: 52.4s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 43

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776625654515-10"
}
```

Response:
```json
{
  "id": 28,
  "userId": "e2e-10x-1776625654515-10",
  "solanaAddress": "CAWjqf8mm5XRicmmWYYmNEaNYpJhAvoieVdTXwW1Ehre",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T19:07:34.634291456Z",
  "updatedAt": "2026-04-19T19:07:34.634291456Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/28/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "4860ae81-8fb1-4156-a782-0fc7cdeee786",
  "walletId": 28,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TO0gu3nnME1dfOB1MBgLKRc",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T19:07:34.656950049Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/4860ae81-8fb1-4156-a782-0fc7cdeee786`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "4860ae81-8fb1-4156-a782-0fc7cdeee786",
  "walletId": 28,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TO0gu3nnME1dfOB1MBgLKRc",
  "createdAt": "2026-04-19T19:07:34.656950Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776625654515-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 18,
  "remittanceId": "cf5346df-80ba-4f6c-81d7-7a9dd2fa1418",
  "senderId": "e2e-10x-1776625654515-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "6a931f2e-c22e-4c60-b2c5-ada5d4872c5d",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:07:43.466338785Z",
  "updatedAt": "2026-04-19T19:07:43.479094974Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/cf5346df-80ba-4f6c-81d7-7a9dd2fa1418`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 18,
  "remittanceId": "cf5346df-80ba-4f6c-81d7-7a9dd2fa1418",
  "senderId": "e2e-10x-1776625654515-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "6a931f2e-c22e-4c60-b2c5-ada5d4872c5d",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:07:43.466339Z",
  "updatedAt": "2026-04-19T19:08:18.559514Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/6a931f2e-c22e-4c60-b2c5-ada5d4872c5d`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "cf5346df-80ba-4f6c-81d7-7a9dd2fa1418",
  "senderId": "e2e-10x-1776625654515-10",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T19:07:43.473649Z"
}
```

#### 7-submit-claim

`POST /api/claims/6a931f2e-c22e-4c60-b2c5-ada5d4872c5d`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "cf5346df-80ba-4f6c-81d7-7a9dd2fa1418",
  "senderId": "e2e-10x-1776625654515-10",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T19:07:43.473649Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/cf5346df-80ba-4f6c-81d7-7a9dd2fa1418`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 18,
  "remittanceId": "cf5346df-80ba-4f6c-81d7-7a9dd2fa1418",
  "senderId": "e2e-10x-1776625654515-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "6a931f2e-c22e-4c60-b2c5-ada5d4872c5d",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T19:07:43.466339Z",
  "updatedAt": "2026-04-19T19:08:24.868512Z",
  "expiresAt": null
}
```

---
