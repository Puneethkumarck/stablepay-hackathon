# 10-Customer Full E2E Run — $1.00 USDC per customer

Generated: 2026-04-18 20:44:06 UTC

## Summary

- Customers attempted: 10
- Passed: 9
- Failed: 1
- Per-customer amount: $1.00 USDC

| # | userId | walletId | address | fundingId | remittanceId | final | USDC after fund | elapsed (s) | PASS |
|---|--------|----------|---------|-----------|--------------|-------|-----------------|-------------|------|
| 1 | `e2e-10x-1776544675726-1` | 1 | `8MpRJTD42yxdSDzeQ8QVuKwa8aVihMsSZZxdD6qoHj6n` | `2bf23f57-c1a8-4be5-a5ec-d474e0ac7b8a` | `878de78d-f782-4ba3-9ae4-9034290c343e` | DELIVERED | 1 | 18.7 | ✅ |
| 2 | `e2e-10x-1776544694458-2` | 2 | `HLtcpujz48wv33HsFjfRUwAmNzRQLSAJZEExgWa2Mcws` | `91c602f9-5750-413e-94e4-cd80fabc445c` | `bd9e7996-7263-4eca-9dc6-99bf41e18260` | DELIVERED | 1 | 49.1 | ✅ |
| 3 | `e2e-10x-1776544743600-3` | 3 | `5AgBvzyrag2VzWpVFAk85a31bqk9t2p5zdT6mpUDu8kc` | `a1ef2cdb-09f1-40a3-a83c-b8e263a21431` | `db92a65c-7250-48a4-bd43-6c8433014f29` | DELIVERED | 1 | 16.1 | ✅ |
| 4 | `e2e-10x-1776544759716-4` | 4 | `7MgfRxGAhfwZJuE1qVXjiSJxSjBRXSuSAgamHLhHueAv` | `6806d8a1-76b4-474f-8fdb-6c8cf42056c1` | `4d37df17-1f5a-4866-aec4-4592d2de7360` | DELIVERED | 1 | 46.9 | ✅ |
| 5 | `e2e-10x-1776544806648-5` | 5 | `9xUbYQy64AvxvVUPLkHVugaFTZn6hnjeL4QbPs8WmkKF` | `9c37eed4-01bb-42ae-b890-b9946a50531b` | `22cf8aae-42cb-4180-a602-c2a2ab0ec79b` | DELIVERED | 1 | 16.6 | ✅ |
| 6 | `e2e-10x-1776544823207-6` | 6 | `CG8ikGdacGfxyaiJVedGf5agz28ffYgkmt4PN1JF4NF2` | `eb0bc73a-a30e-4d31-96d4-f443a823d6cc` | `2c417e26-74eb-41b9-85c7-04a2bb10a70d` | DELIVERED | 1 | 79.2 | ✅ |
| 7 | `e2e-10x-1776544902383-7` | 7 | `pdx3wUFtVSVESU8deMy71FX2jKF1z7faXU1UaE94C2V` | `32accf8d-9eff-4a83-8aa6-8ed39c4b00ab` | `65cc4519-b784-4ca2-8488-84d4cadb246f` | DELIVERED | 1 | 80.3 | ✅ |
| 8 | `e2e-10x-1776544982690-8` | 8 | `H3m2y6zyjoReYq287YMPt8zY7pzyRQNgWpAVdFW8V6o4` | `2185ee8c-4040-483a-9fbd-74fae37f2b67` | `9daf9188-9192-453f-894b-73e3b300f51e` | DELIVERED | 1 | 16.8 | ✅ |
| 9 | `e2e-10x-1776544999530-9` | None | `None` | `None` | `None` | - | - | 30.1 | ❌ create wallet failed — expected 201, see detail log |
| 10 | `e2e-10x-1776545029653-10` | 9 | `DD8Ac8hTuwkjFauRJ9NkLgDazTSC3LRXPJgKsAyzgvNy` | `25591127-3111-4ee8-a877-c00bc0f331f0` | `8749a570-a49d-435b-ae49-c1ae8e6b64b1` | DELIVERED | 1 | 16.8 | ✅ |

## Per-customer request / response log

### Customer 1 — `e2e-10x-1776544675726-1`

- walletId: `1`  solanaAddress: `8MpRJTD42yxdSDzeQ8QVuKwa8aVihMsSZZxdD6qoHj6n`
- fundingId: `2bf23f57-c1a8-4be5-a5ec-d474e0ac7b8a`  paymentIntentId: `pi_3TNfco3nnME1dfOB1g2sbyZ2`
- remittanceId: `878de78d-f782-4ba3-9ae4-9034290c343e`  claimTokenId: `420de909-b6d7-4aea-a44c-f016649b96ca`
- final status: **DELIVERED**  elapsed: 18.7s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 13

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776544675726-1"
}
```

Response:
```json
{
  "id": 1,
  "userId": "e2e-10x-1776544675726-1",
  "solanaAddress": "8MpRJTD42yxdSDzeQ8QVuKwa8aVihMsSZZxdD6qoHj6n",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T20:37:56.652488292Z",
  "updatedAt": "2026-04-18T20:37:56.652488292Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/1/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "2bf23f57-c1a8-4be5-a5ec-d474e0ac7b8a",
  "walletId": 1,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNfco3nnME1dfOB1g2sbyZ2",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-18T20:37:56.880208082Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/2bf23f57-c1a8-4be5-a5ec-d474e0ac7b8a`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "2bf23f57-c1a8-4be5-a5ec-d474e0ac7b8a",
  "walletId": 1,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNfco3nnME1dfOB1g2sbyZ2",
  "createdAt": "2026-04-18T20:37:56.880208Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776544675726-1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 1,
  "remittanceId": "878de78d-f782-4ba3-9ae4-9034290c343e",
  "senderId": "e2e-10x-1776544675726-1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "420de909-b6d7-4aea-a44c-f016649b96ca",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:38:07.388829600Z",
  "updatedAt": "2026-04-18T20:38:07.398162340Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/878de78d-f782-4ba3-9ae4-9034290c343e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 1,
  "remittanceId": "878de78d-f782-4ba3-9ae4-9034290c343e",
  "senderId": "e2e-10x-1776544675726-1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "420de909-b6d7-4aea-a44c-f016649b96ca",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:38:07.388830Z",
  "updatedAt": "2026-04-18T20:38:08.311274Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/420de909-b6d7-4aea-a44c-f016649b96ca`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "878de78d-f782-4ba3-9ae4-9034290c343e",
  "senderId": "e2e-10x-1776544675726-1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T20:38:07.394736Z"
}
```

#### 7-submit-claim

`POST /api/claims/420de909-b6d7-4aea-a44c-f016649b96ca`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "878de78d-f782-4ba3-9ae4-9034290c343e",
  "senderId": "e2e-10x-1776544675726-1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T20:38:07.394736Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/878de78d-f782-4ba3-9ae4-9034290c343e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 1,
  "remittanceId": "878de78d-f782-4ba3-9ae4-9034290c343e",
  "senderId": "e2e-10x-1776544675726-1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "420de909-b6d7-4aea-a44c-f016649b96ca",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:38:07.388830Z",
  "updatedAt": "2026-04-18T20:38:12.006261Z",
  "expiresAt": null
}
```

---

### Customer 2 — `e2e-10x-1776544694458-2`

- walletId: `2`  solanaAddress: `HLtcpujz48wv33HsFjfRUwAmNzRQLSAJZEExgWa2Mcws`
- fundingId: `91c602f9-5750-413e-94e4-cd80fabc445c`  paymentIntentId: `pi_3TNfdc3nnME1dfOB1WrCleJp`
- remittanceId: `bd9e7996-7263-4eca-9dc6-99bf41e18260`  claimTokenId: `c6703791-d18f-448b-a235-a7dba3cc66d5`
- final status: **DELIVERED**  elapsed: 49.1s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 14

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776544694458-2"
}
```

Response:
```json
{
  "id": 2,
  "userId": "e2e-10x-1776544694458-2",
  "solanaAddress": "HLtcpujz48wv33HsFjfRUwAmNzRQLSAJZEExgWa2Mcws",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T20:38:46.731933066Z",
  "updatedAt": "2026-04-18T20:38:46.731933066Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/2/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "91c602f9-5750-413e-94e4-cd80fabc445c",
  "walletId": 2,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNfdc3nnME1dfOB1WrCleJp",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-18T20:38:46.785892798Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/91c602f9-5750-413e-94e4-cd80fabc445c`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "91c602f9-5750-413e-94e4-cd80fabc445c",
  "walletId": 2,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNfdc3nnME1dfOB1WrCleJp",
  "createdAt": "2026-04-18T20:38:46.785893Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776544694458-2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 2,
  "remittanceId": "bd9e7996-7263-4eca-9dc6-99bf41e18260",
  "senderId": "e2e-10x-1776544694458-2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "c6703791-d18f-448b-a235-a7dba3cc66d5",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:38:56.604519077Z",
  "updatedAt": "2026-04-18T20:38:56.609345821Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/bd9e7996-7263-4eca-9dc6-99bf41e18260`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 2,
  "remittanceId": "bd9e7996-7263-4eca-9dc6-99bf41e18260",
  "senderId": "e2e-10x-1776544694458-2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "c6703791-d18f-448b-a235-a7dba3cc66d5",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:38:56.604519Z",
  "updatedAt": "2026-04-18T20:38:57.738075Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/c6703791-d18f-448b-a235-a7dba3cc66d5`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "bd9e7996-7263-4eca-9dc6-99bf41e18260",
  "senderId": "e2e-10x-1776544694458-2",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T20:38:56.607975Z"
}
```

#### 7-submit-claim

`POST /api/claims/c6703791-d18f-448b-a235-a7dba3cc66d5`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "bd9e7996-7263-4eca-9dc6-99bf41e18260",
  "senderId": "e2e-10x-1776544694458-2",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T20:38:56.607975Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/bd9e7996-7263-4eca-9dc6-99bf41e18260`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 2,
  "remittanceId": "bd9e7996-7263-4eca-9dc6-99bf41e18260",
  "senderId": "e2e-10x-1776544694458-2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "c6703791-d18f-448b-a235-a7dba3cc66d5",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:38:56.604519Z",
  "updatedAt": "2026-04-18T20:39:01.159921Z",
  "expiresAt": null
}
```

---

### Customer 3 — `e2e-10x-1776544743600-3`

- walletId: `3`  solanaAddress: `5AgBvzyrag2VzWpVFAk85a31bqk9t2p5zdT6mpUDu8kc`
- fundingId: `a1ef2cdb-09f1-40a3-a83c-b8e263a21431`  paymentIntentId: `pi_3TNfdt3nnME1dfOB15aAfHUh`
- remittanceId: `db92a65c-7250-48a4-bd43-6c8433014f29`  claimTokenId: `dfa83610-7780-4846-9487-654ac2ed4b4a`
- final status: **DELIVERED**  elapsed: 16.1s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 15

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776544743600-3"
}
```

Response:
```json
{
  "id": 3,
  "userId": "e2e-10x-1776544743600-3",
  "solanaAddress": "5AgBvzyrag2VzWpVFAk85a31bqk9t2p5zdT6mpUDu8kc",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T20:39:03.706249660Z",
  "updatedAt": "2026-04-18T20:39:03.706249660Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/3/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "a1ef2cdb-09f1-40a3-a83c-b8e263a21431",
  "walletId": 3,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNfdt3nnME1dfOB15aAfHUh",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-18T20:39:03.727390845Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/a1ef2cdb-09f1-40a3-a83c-b8e263a21431`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "a1ef2cdb-09f1-40a3-a83c-b8e263a21431",
  "walletId": 3,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNfdt3nnME1dfOB15aAfHUh",
  "createdAt": "2026-04-18T20:39:03.727391Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776544743600-3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 3,
  "remittanceId": "db92a65c-7250-48a4-bd43-6c8433014f29",
  "senderId": "e2e-10x-1776544743600-3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "dfa83610-7780-4846-9487-654ac2ed4b4a",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:39:12.723255245Z",
  "updatedAt": "2026-04-18T20:39:12.733579567Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/db92a65c-7250-48a4-bd43-6c8433014f29`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 3,
  "remittanceId": "db92a65c-7250-48a4-bd43-6c8433014f29",
  "senderId": "e2e-10x-1776544743600-3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "dfa83610-7780-4846-9487-654ac2ed4b4a",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:39:12.723255Z",
  "updatedAt": "2026-04-18T20:39:13.675252Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/dfa83610-7780-4846-9487-654ac2ed4b4a`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "db92a65c-7250-48a4-bd43-6c8433014f29",
  "senderId": "e2e-10x-1776544743600-3",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T20:39:12.730616Z"
}
```

#### 7-submit-claim

`POST /api/claims/dfa83610-7780-4846-9487-654ac2ed4b4a`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "db92a65c-7250-48a4-bd43-6c8433014f29",
  "senderId": "e2e-10x-1776544743600-3",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T20:39:12.730616Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/db92a65c-7250-48a4-bd43-6c8433014f29`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 3,
  "remittanceId": "db92a65c-7250-48a4-bd43-6c8433014f29",
  "senderId": "e2e-10x-1776544743600-3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "dfa83610-7780-4846-9487-654ac2ed4b4a",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:39:12.723255Z",
  "updatedAt": "2026-04-18T20:39:17.010958Z",
  "expiresAt": null
}
```

---

### Customer 4 — `e2e-10x-1776544759716-4`

- walletId: `4`  solanaAddress: `7MgfRxGAhfwZJuE1qVXjiSJxSjBRXSuSAgamHLhHueAv`
- fundingId: `6806d8a1-76b4-474f-8fdb-6c8cf42056c1`  paymentIntentId: `pi_3TNfe93nnME1dfOB1h1PTW0q`
- remittanceId: `4d37df17-1f5a-4866-aec4-4592d2de7360`  claimTokenId: `82913d08-9c4c-4691-96de-6de874a3b73e`
- final status: **DELIVERED**  elapsed: 46.9s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 16

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776544759716-4"
}
```

Response:
```json
{
  "id": 4,
  "userId": "e2e-10x-1776544759716-4",
  "solanaAddress": "7MgfRxGAhfwZJuE1qVXjiSJxSjBRXSuSAgamHLhHueAv",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T20:39:19.855974086Z",
  "updatedAt": "2026-04-18T20:39:19.855974086Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/4/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "6806d8a1-76b4-474f-8fdb-6c8cf42056c1",
  "walletId": 4,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNfe93nnME1dfOB1h1PTW0q",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-18T20:39:19.871769818Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/6806d8a1-76b4-474f-8fdb-6c8cf42056c1`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "6806d8a1-76b4-474f-8fdb-6c8cf42056c1",
  "walletId": 4,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNfe93nnME1dfOB1h1PTW0q",
  "createdAt": "2026-04-18T20:39:19.871770Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776544759716-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 4,
  "remittanceId": "4d37df17-1f5a-4866-aec4-4592d2de7360",
  "senderId": "e2e-10x-1776544759716-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "82913d08-9c4c-4691-96de-6de874a3b73e",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:39:29.127759305Z",
  "updatedAt": "2026-04-18T20:39:29.133621965Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/4d37df17-1f5a-4866-aec4-4592d2de7360`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 4,
  "remittanceId": "4d37df17-1f5a-4866-aec4-4592d2de7360",
  "senderId": "e2e-10x-1776544759716-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "82913d08-9c4c-4691-96de-6de874a3b73e",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:39:29.127759Z",
  "updatedAt": "2026-04-18T20:40:00.178513Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/82913d08-9c4c-4691-96de-6de874a3b73e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "4d37df17-1f5a-4866-aec4-4592d2de7360",
  "senderId": "e2e-10x-1776544759716-4",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T20:39:29.131885Z"
}
```

#### 7-submit-claim

`POST /api/claims/82913d08-9c4c-4691-96de-6de874a3b73e`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "4d37df17-1f5a-4866-aec4-4592d2de7360",
  "senderId": "e2e-10x-1776544759716-4",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T20:39:29.131885Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/4d37df17-1f5a-4866-aec4-4592d2de7360`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 4,
  "remittanceId": "4d37df17-1f5a-4866-aec4-4592d2de7360",
  "senderId": "e2e-10x-1776544759716-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "82913d08-9c4c-4691-96de-6de874a3b73e",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:39:29.127759Z",
  "updatedAt": "2026-04-18T20:40:04.097028Z",
  "expiresAt": null
}
```

---

### Customer 5 — `e2e-10x-1776544806648-5`

- walletId: `5`  solanaAddress: `9xUbYQy64AvxvVUPLkHVugaFTZn6hnjeL4QbPs8WmkKF`
- fundingId: `9c37eed4-01bb-42ae-b890-b9946a50531b`  paymentIntentId: `pi_3TNfeu3nnME1dfOB0DQil9Ov`
- remittanceId: `22cf8aae-42cb-4180-a602-c2a2ab0ec79b`  claimTokenId: `b9f65afd-cc42-414c-b37c-29b55acf2b1a`
- final status: **DELIVERED**  elapsed: 16.6s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 17

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776544806648-5"
}
```

Response:
```json
{
  "id": 5,
  "userId": "e2e-10x-1776544806648-5",
  "solanaAddress": "9xUbYQy64AvxvVUPLkHVugaFTZn6hnjeL4QbPs8WmkKF",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T20:40:06.732436440Z",
  "updatedAt": "2026-04-18T20:40:06.732436440Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/5/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "9c37eed4-01bb-42ae-b890-b9946a50531b",
  "walletId": 5,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNfeu3nnME1dfOB0DQil9Ov",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-18T20:40:06.745684717Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/9c37eed4-01bb-42ae-b890-b9946a50531b`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "9c37eed4-01bb-42ae-b890-b9946a50531b",
  "walletId": 5,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNfeu3nnME1dfOB0DQil9Ov",
  "createdAt": "2026-04-18T20:40:06.745685Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776544806648-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 5,
  "remittanceId": "22cf8aae-42cb-4180-a602-c2a2ab0ec79b",
  "senderId": "e2e-10x-1776544806648-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "b9f65afd-cc42-414c-b37c-29b55acf2b1a",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:40:16.251870801Z",
  "updatedAt": "2026-04-18T20:40:16.255556505Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/22cf8aae-42cb-4180-a602-c2a2ab0ec79b`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 5,
  "remittanceId": "22cf8aae-42cb-4180-a602-c2a2ab0ec79b",
  "senderId": "e2e-10x-1776544806648-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "b9f65afd-cc42-414c-b37c-29b55acf2b1a",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:40:16.251871Z",
  "updatedAt": "2026-04-18T20:40:17.280969Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/b9f65afd-cc42-414c-b37c-29b55acf2b1a`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "22cf8aae-42cb-4180-a602-c2a2ab0ec79b",
  "senderId": "e2e-10x-1776544806648-5",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T20:40:16.253890Z"
}
```

#### 7-submit-claim

`POST /api/claims/b9f65afd-cc42-414c-b37c-29b55acf2b1a`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "22cf8aae-42cb-4180-a602-c2a2ab0ec79b",
  "senderId": "e2e-10x-1776544806648-5",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T20:40:16.253890Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/22cf8aae-42cb-4180-a602-c2a2ab0ec79b`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 5,
  "remittanceId": "22cf8aae-42cb-4180-a602-c2a2ab0ec79b",
  "senderId": "e2e-10x-1776544806648-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "b9f65afd-cc42-414c-b37c-29b55acf2b1a",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:40:16.251871Z",
  "updatedAt": "2026-04-18T20:40:20.651686Z",
  "expiresAt": null
}
```

---

### Customer 6 — `e2e-10x-1776544823207-6`

- walletId: `6`  solanaAddress: `CG8ikGdacGfxyaiJVedGf5agz28ffYgkmt4PN1JF4NF2`
- fundingId: `eb0bc73a-a30e-4d31-96d4-f443a823d6cc`  paymentIntentId: `pi_3TNffg3nnME1dfOB1MVsm1VK`
- remittanceId: `2c417e26-74eb-41b9-85c7-04a2bb10a70d`  claimTokenId: `e0447639-ac9f-47d3-8587-585a52aba94b`
- final status: **DELIVERED**  elapsed: 79.2s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 18

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776544823207-6"
}
```

Response:
```json
{
  "id": 6,
  "userId": "e2e-10x-1776544823207-6",
  "solanaAddress": "CG8ikGdacGfxyaiJVedGf5agz28ffYgkmt4PN1JF4NF2",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T20:40:55.363840871Z",
  "updatedAt": "2026-04-18T20:40:55.363840871Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/6/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "eb0bc73a-a30e-4d31-96d4-f443a823d6cc",
  "walletId": 6,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNffg3nnME1dfOB1MVsm1VK",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-18T20:40:55.409724737Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/eb0bc73a-a30e-4d31-96d4-f443a823d6cc`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "eb0bc73a-a30e-4d31-96d4-f443a823d6cc",
  "walletId": 6,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNffg3nnME1dfOB1MVsm1VK",
  "createdAt": "2026-04-18T20:40:55.409725Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776544823207-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 6,
  "remittanceId": "2c417e26-74eb-41b9-85c7-04a2bb10a70d",
  "senderId": "e2e-10x-1776544823207-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "e0447639-ac9f-47d3-8587-585a52aba94b",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:41:05.096232361Z",
  "updatedAt": "2026-04-18T20:41:05.101448231Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/2c417e26-74eb-41b9-85c7-04a2bb10a70d`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 6,
  "remittanceId": "2c417e26-74eb-41b9-85c7-04a2bb10a70d",
  "senderId": "e2e-10x-1776544823207-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "e0447639-ac9f-47d3-8587-585a52aba94b",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:41:05.096232Z",
  "updatedAt": "2026-04-18T20:41:36.311689Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/e0447639-ac9f-47d3-8587-585a52aba94b`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "2c417e26-74eb-41b9-85c7-04a2bb10a70d",
  "senderId": "e2e-10x-1776544823207-6",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T20:41:05.099944Z"
}
```

#### 7-submit-claim

`POST /api/claims/e0447639-ac9f-47d3-8587-585a52aba94b`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "2c417e26-74eb-41b9-85c7-04a2bb10a70d",
  "senderId": "e2e-10x-1776544823207-6",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T20:41:05.099944Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/2c417e26-74eb-41b9-85c7-04a2bb10a70d`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 6,
  "remittanceId": "2c417e26-74eb-41b9-85c7-04a2bb10a70d",
  "senderId": "e2e-10x-1776544823207-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "e0447639-ac9f-47d3-8587-585a52aba94b",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:41:05.096232Z",
  "updatedAt": "2026-04-18T20:41:39.855014Z",
  "expiresAt": null
}
```

---

### Customer 7 — `e2e-10x-1776544902383-7`

- walletId: `7`  solanaAddress: `pdx3wUFtVSVESU8deMy71FX2jKF1z7faXU1UaE94C2V`
- fundingId: `32accf8d-9eff-4a83-8aa6-8ed39c4b00ab`  paymentIntentId: `pi_3TNfgS3nnME1dfOB1LqWa8H9`
- remittanceId: `65cc4519-b784-4ca2-8488-84d4cadb246f`  claimTokenId: `face7b80-45e5-44c4-a200-b7db4fb0065f`
- final status: **DELIVERED**  elapsed: 80.3s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 19

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776544902383-7"
}
```

Response:
```json
{
  "id": 7,
  "userId": "e2e-10x-1776544902383-7",
  "solanaAddress": "pdx3wUFtVSVESU8deMy71FX2jKF1z7faXU1UaE94C2V",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T20:41:42.517385288Z",
  "updatedAt": "2026-04-18T20:41:42.517385288Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/7/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "32accf8d-9eff-4a83-8aa6-8ed39c4b00ab",
  "walletId": 7,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNfgS3nnME1dfOB1LqWa8H9",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-18T20:41:42.537205099Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/32accf8d-9eff-4a83-8aa6-8ed39c4b00ab`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "32accf8d-9eff-4a83-8aa6-8ed39c4b00ab",
  "walletId": 7,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNfgS3nnME1dfOB1LqWa8H9",
  "createdAt": "2026-04-18T20:41:42.537205Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776544902383-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 7,
  "remittanceId": "65cc4519-b784-4ca2-8488-84d4cadb246f",
  "senderId": "e2e-10x-1776544902383-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "face7b80-45e5-44c4-a200-b7db4fb0065f",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:41:52.068536146Z",
  "updatedAt": "2026-04-18T20:41:52.073649015Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/65cc4519-b784-4ca2-8488-84d4cadb246f`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 7,
  "remittanceId": "65cc4519-b784-4ca2-8488-84d4cadb246f",
  "senderId": "e2e-10x-1776544902383-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "face7b80-45e5-44c4-a200-b7db4fb0065f",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:41:52.068536Z",
  "updatedAt": "2026-04-18T20:42:55.878818Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/face7b80-45e5-44c4-a200-b7db4fb0065f`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "65cc4519-b784-4ca2-8488-84d4cadb246f",
  "senderId": "e2e-10x-1776544902383-7",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T20:41:52.072290Z"
}
```

#### 7-submit-claim

`POST /api/claims/face7b80-45e5-44c4-a200-b7db4fb0065f`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "65cc4519-b784-4ca2-8488-84d4cadb246f",
  "senderId": "e2e-10x-1776544902383-7",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T20:41:52.072290Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/65cc4519-b784-4ca2-8488-84d4cadb246f`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 7,
  "remittanceId": "65cc4519-b784-4ca2-8488-84d4cadb246f",
  "senderId": "e2e-10x-1776544902383-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "face7b80-45e5-44c4-a200-b7db4fb0065f",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:41:52.068536Z",
  "updatedAt": "2026-04-18T20:43:00.164488Z",
  "expiresAt": null
}
```

---

### Customer 8 — `e2e-10x-1776544982690-8`

- walletId: `8`  solanaAddress: `H3m2y6zyjoReYq287YMPt8zY7pzyRQNgWpAVdFW8V6o4`
- fundingId: `2185ee8c-4040-483a-9fbd-74fae37f2b67`  paymentIntentId: `pi_3TNfhk3nnME1dfOB08abFjKl`
- remittanceId: `9daf9188-9192-453f-894b-73e3b300f51e`  claimTokenId: `94374633-dc1f-410e-8b29-efe0f81efb92`
- final status: **DELIVERED**  elapsed: 16.8s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 20

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776544982690-8"
}
```

Response:
```json
{
  "id": 8,
  "userId": "e2e-10x-1776544982690-8",
  "solanaAddress": "H3m2y6zyjoReYq287YMPt8zY7pzyRQNgWpAVdFW8V6o4",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T20:43:02.790865965Z",
  "updatedAt": "2026-04-18T20:43:02.790865965Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/8/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "2185ee8c-4040-483a-9fbd-74fae37f2b67",
  "walletId": 8,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNfhk3nnME1dfOB08abFjKl",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-18T20:43:02.808638862Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/2185ee8c-4040-483a-9fbd-74fae37f2b67`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "2185ee8c-4040-483a-9fbd-74fae37f2b67",
  "walletId": 8,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNfhk3nnME1dfOB08abFjKl",
  "createdAt": "2026-04-18T20:43:02.808639Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776544982690-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 8,
  "remittanceId": "9daf9188-9192-453f-894b-73e3b300f51e",
  "senderId": "e2e-10x-1776544982690-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "94374633-dc1f-410e-8b29-efe0f81efb92",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:43:12.568963206Z",
  "updatedAt": "2026-04-18T20:43:12.572987451Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/9daf9188-9192-453f-894b-73e3b300f51e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 8,
  "remittanceId": "9daf9188-9192-453f-894b-73e3b300f51e",
  "senderId": "e2e-10x-1776544982690-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "94374633-dc1f-410e-8b29-efe0f81efb92",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:43:12.568963Z",
  "updatedAt": "2026-04-18T20:43:13.833559Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/94374633-dc1f-410e-8b29-efe0f81efb92`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "9daf9188-9192-453f-894b-73e3b300f51e",
  "senderId": "e2e-10x-1776544982690-8",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T20:43:12.571695Z"
}
```

#### 7-submit-claim

`POST /api/claims/94374633-dc1f-410e-8b29-efe0f81efb92`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "9daf9188-9192-453f-894b-73e3b300f51e",
  "senderId": "e2e-10x-1776544982690-8",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T20:43:12.571695Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/9daf9188-9192-453f-894b-73e3b300f51e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 8,
  "remittanceId": "9daf9188-9192-453f-894b-73e3b300f51e",
  "senderId": "e2e-10x-1776544982690-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "94374633-dc1f-410e-8b29-efe0f81efb92",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:43:12.568963Z",
  "updatedAt": "2026-04-18T20:43:17.015649Z",
  "expiresAt": null
}
```

---

### Customer 9 — `e2e-10x-1776544999530-9`

- walletId: `None`  solanaAddress: `None`
- fundingId: `None`  paymentIntentId: `None`
- remittanceId: `None`  claimTokenId: `None`
- final status: **None**  elapsed: 30.1s  **FAIL: create wallet failed — expected 201, see detail log**
- On-chain USDC after fund: None
- Claim authority USDC after claim: None

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 500**

Request:
```json
{
  "userId": "e2e-10x-1776544999530-9"
}
```

Response:
```json
{
  "timestamp": "2026-04-18T20:43:49.641Z",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/wallets"
}
```

---

### Customer 10 — `e2e-10x-1776545029653-10`

- walletId: `9`  solanaAddress: `DD8Ac8hTuwkjFauRJ9NkLgDazTSC3LRXPJgKsAyzgvNy`
- fundingId: `25591127-3111-4ee8-a877-c00bc0f331f0`  paymentIntentId: `pi_3TNfiV3nnME1dfOB0mkk4Ehk`
- remittanceId: `8749a570-a49d-435b-ae49-c1ae8e6b64b1`  claimTokenId: `6944bf13-810b-48d7-852c-85e31f5d9cf7`
- final status: **DELIVERED**  elapsed: 16.8s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 21

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776545029653-10"
}
```

Response:
```json
{
  "id": 9,
  "userId": "e2e-10x-1776545029653-10",
  "solanaAddress": "DD8Ac8hTuwkjFauRJ9NkLgDazTSC3LRXPJgKsAyzgvNy",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T20:43:49.779329695Z",
  "updatedAt": "2026-04-18T20:43:49.779329695Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/9/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "25591127-3111-4ee8-a877-c00bc0f331f0",
  "walletId": 9,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNfiV3nnME1dfOB0mkk4Ehk",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-18T20:43:49.804239709Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/25591127-3111-4ee8-a877-c00bc0f331f0`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "25591127-3111-4ee8-a877-c00bc0f331f0",
  "walletId": 9,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNfiV3nnME1dfOB0mkk4Ehk",
  "createdAt": "2026-04-18T20:43:49.804240Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776545029653-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 9,
  "remittanceId": "8749a570-a49d-435b-ae49-c1ae8e6b64b1",
  "senderId": "e2e-10x-1776545029653-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "6944bf13-810b-48d7-852c-85e31f5d9cf7",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:43:59.477564024Z",
  "updatedAt": "2026-04-18T20:43:59.486155431Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/8749a570-a49d-435b-ae49-c1ae8e6b64b1`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 9,
  "remittanceId": "8749a570-a49d-435b-ae49-c1ae8e6b64b1",
  "senderId": "e2e-10x-1776545029653-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "6944bf13-810b-48d7-852c-85e31f5d9cf7",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:43:59.477564Z",
  "updatedAt": "2026-04-18T20:44:00.757699Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/6944bf13-810b-48d7-852c-85e31f5d9cf7`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "8749a570-a49d-435b-ae49-c1ae8e6b64b1",
  "senderId": "e2e-10x-1776545029653-10",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T20:43:59.483835Z"
}
```

#### 7-submit-claim

`POST /api/claims/6944bf13-810b-48d7-852c-85e31f5d9cf7`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "8749a570-a49d-435b-ae49-c1ae8e6b64b1",
  "senderId": "e2e-10x-1776545029653-10",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T20:43:59.483835Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/8749a570-a49d-435b-ae49-c1ae8e6b64b1`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 9,
  "remittanceId": "8749a570-a49d-435b-ae49-c1ae8e6b64b1",
  "senderId": "e2e-10x-1776545029653-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "6944bf13-810b-48d7-852c-85e31f5d9cf7",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T20:43:59.477564Z",
  "updatedAt": "2026-04-18T20:44:03.803Z",
  "expiresAt": null
}
```

---
