# 10-Customer Full E2E Run — $1.00 USDC per customer

Generated: 2026-04-19 11:30:09 UTC

## Summary

- Customers attempted: 10
- Passed: 10
- Failed: 0
- Per-customer amount: $1.00 USDC

| # | userId | walletId | address | fundingId | remittanceId | final | USDC after fund | payout_id | elapsed (s) | PASS |
|---|--------|----------|---------|-----------|--------------|-------|-----------------|-----------|-------------|------|
| 1 | `e2e-10x-1776597783066-1` | 8 | `38LhK36pBiBY6PvaG1PUB4tZemCxMFWxPF4HbnGLmJAj` | `c9cd5fa8-7c67-403f-8a8b-df8c16cb3b28` | `24eb9ede-3bd3-40c2-9630-951ada545130` | DELIVERED | 1 | `pout_wm_1afbxunzb5mb` | 16.9 | ✅ |
| 2 | `e2e-10x-1776597799939-2` | 9 | `HjHG4A5H8cLsac9WPpAB3B6aqJpbWfGrqkDvpX4Zfsht` | `51d94938-615d-4587-ba12-851cea5710af` | `c7f52cb1-9965-40d8-9e1a-927008dd9da6` | DELIVERED | 1 | `pout_wm_4fgovobnrnkj` | 49.1 | ✅ |
| 3 | `e2e-10x-1776597849019-3` | 10 | `GvqrmXdXV6Eak8NRP3F24FAQ2EzP3JkXbzkXarbyeXfZ` | `6f108599-3f3a-4e1c-bf41-d3e5b5c9d488` | `53c43ad7-6fda-4b0d-8c6f-310d6218cecc` | DELIVERED | 1 | `pout_wm_gyf0zd8q7idy` | 46.4 | ✅ |
| 4 | `e2e-10x-1776597895402-4` | 11 | `UqtBgvFcJkagupTZn14Wwe1NkhNj6iD8rgQZ2omyTUR` | `f0b2e2f6-b26e-4ac1-93d8-d7b50d85fa5f` | `3b296846-fe40-41a3-ade8-eda73f5e8af2` | DELIVERED | 1 | `pout_wm_kvrbr3peaes8` | 16.7 | ✅ |
| 5 | `e2e-10x-1776597912110-5` | 12 | `9RsH8fyUypCxnz4xD1KF6MhxaU6cfU4jGSkbGcRKwS5g` | `4d43b24a-0f9b-4f01-8b98-3c8342e80f53` | `721ffdb1-9f30-426b-97ee-565f197549d2` | DELIVERED | 1 | `pout_wm_tg980vmggklj` | 113.0 | ✅ |
| 6 | `e2e-10x-1776598025160-6` | 13 | `51GMAD8xXDEN9jr8virrMkMBXVrG2RAV49etrsMbs6gV` | `108dfcb4-9e95-47ee-a7c1-7b5c60c0e437` | `b9d7812a-aae2-448e-8b40-368810451de6` | DELIVERED | 1 | `pout_wm_pqbbwa8babew` | 50.8 | ✅ |
| 7 | `e2e-10x-1776598075964-7` | 14 | `Fwex4a76GAFvsYEA8XDWYfFu4xBPTNdgmgBa8tzNCdCY` | `8ee040a8-a3a6-459d-a90f-71138b3d65bd` | `1dee3072-adc6-4872-8fbb-593675866249` | DELIVERED | 1 | `pout_wm_6jarhbmk9wio` | 22.9 | ✅ |
| 8 | `e2e-10x-1776598098872-8` | 15 | `CogKwMA8W5oVBhE8fMUKVQKdVBbB6y6uQ5ggCgiytdF3` | `99f047f0-ab7f-415c-9960-66de2917cb54` | `9c6d725f-32dc-42df-bc00-3eaff10f8c9d` | DELIVERED | 1 | `pout_wm_u5hmkb1zwkd0` | 16.3 | ✅ |
| 9 | `e2e-10x-1776598115130-9` | 16 | `74GboL5qRGncSJJYXCrFgabNNpfweqUuC5gfF8YQmfA4` | `32466a08-4bcc-4b60-9e98-4919e12ad5a0` | `af3b0178-7931-4d19-9721-bb3e96727a1c` | DELIVERED | 1 | `pout_wm_pfi0ubnabdur` | 46.8 | ✅ |
| 10 | `e2e-10x-1776598161943-10` | 17 | `G37Th6CcvAVRtjUw5acdDCdJmn3y7H8JTKB9W2ViKze9` | `7a7a7dc6-fd38-4e2a-8572-21cf8b0110ca` | `5138141b-9a20-461f-b7df-e4dbf27d6d46` | DELIVERED | 1 | `pout_wm_mmxjbzzuhsnk` | 47.4 | ✅ |

## Per-customer request / response log

### Customer 1 — `e2e-10x-1776597783066-1`

- walletId: `8`  solanaAddress: `38LhK36pBiBY6PvaG1PUB4tZemCxMFWxPF4HbnGLmJAj`
- fundingId: `c9cd5fa8-7c67-403f-8a8b-df8c16cb3b28`  paymentIntentId: `pi_3TNtRM3nnME1dfOB1jvCcDMS`
- remittanceId: `24eb9ede-3bd3-40c2-9630-951ada545130`  claimTokenId: `60e97f79-f86d-4227-aa13-532400e8f7db`
- final status: **DELIVERED**  elapsed: 16.9s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 27
- Payout: id=`pout_wm_1afbxunzb5mb` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776597783066-1"
}
```

Response:
```json
{
  "id": 8,
  "userId": "e2e-10x-1776597783066-1",
  "solanaAddress": "38LhK36pBiBY6PvaG1PUB4tZemCxMFWxPF4HbnGLmJAj",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:23:03.237617654Z",
  "updatedAt": "2026-04-19T11:23:03.237617654Z"
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
  "fundingId": "c9cd5fa8-7c67-403f-8a8b-df8c16cb3b28",
  "walletId": 8,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtRM3nnME1dfOB1jvCcDMS",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:23:03.262730811Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/c9cd5fa8-7c67-403f-8a8b-df8c16cb3b28`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "c9cd5fa8-7c67-403f-8a8b-df8c16cb3b28",
  "walletId": 8,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtRM3nnME1dfOB1jvCcDMS",
  "createdAt": "2026-04-19T11:23:03.262731Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776597783066-1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 2,
  "remittanceId": "24eb9ede-3bd3-40c2-9630-951ada545130",
  "senderId": "e2e-10x-1776597783066-1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "60e97f79-f86d-4227-aa13-532400e8f7db",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:23:12.893929266Z",
  "updatedAt": "2026-04-19T11:23:12.897653796Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/24eb9ede-3bd3-40c2-9630-951ada545130`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 2,
  "remittanceId": "24eb9ede-3bd3-40c2-9630-951ada545130",
  "senderId": "e2e-10x-1776597783066-1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "60e97f79-f86d-4227-aa13-532400e8f7db",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:23:12.893929Z",
  "updatedAt": "2026-04-19T11:23:13.765042Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/60e97f79-f86d-4227-aa13-532400e8f7db`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "24eb9ede-3bd3-40c2-9630-951ada545130",
  "senderId": "e2e-10x-1776597783066-1",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:23:12.896743Z"
}
```

#### 7-submit-claim

`POST /api/claims/60e97f79-f86d-4227-aa13-532400e8f7db`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "24eb9ede-3bd3-40c2-9630-951ada545130",
  "senderId": "e2e-10x-1776597783066-1",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:23:12.896743Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/24eb9ede-3bd3-40c2-9630-951ada545130`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 2,
  "remittanceId": "24eb9ede-3bd3-40c2-9630-951ada545130",
  "senderId": "e2e-10x-1776597783066-1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "60e97f79-f86d-4227-aa13-532400e8f7db",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:23:12.893929Z",
  "updatedAt": "2026-04-19T11:23:17.783516Z",
  "expiresAt": null
}
```

---

### Customer 2 — `e2e-10x-1776597799939-2`

- walletId: `9`  solanaAddress: `HjHG4A5H8cLsac9WPpAB3B6aqJpbWfGrqkDvpX4Zfsht`
- fundingId: `51d94938-615d-4587-ba12-851cea5710af`  paymentIntentId: `pi_3TNtS93nnME1dfOB0r2lPGyp`
- remittanceId: `c7f52cb1-9965-40d8-9e1a-927008dd9da6`  claimTokenId: `44d16798-e4fa-4530-aa6d-1e0a6d81c767`
- final status: **DELIVERED**  elapsed: 49.1s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 28
- Payout: id=`pout_wm_4fgovobnrnkj` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776597799939-2"
}
```

Response:
```json
{
  "id": 9,
  "userId": "e2e-10x-1776597799939-2",
  "solanaAddress": "HjHG4A5H8cLsac9WPpAB3B6aqJpbWfGrqkDvpX4Zfsht",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:23:52.149506393Z",
  "updatedAt": "2026-04-19T11:23:52.149506393Z"
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
  "fundingId": "51d94938-615d-4587-ba12-851cea5710af",
  "walletId": 9,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtS93nnME1dfOB0r2lPGyp",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:23:52.226785757Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/51d94938-615d-4587-ba12-851cea5710af`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "51d94938-615d-4587-ba12-851cea5710af",
  "walletId": 9,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtS93nnME1dfOB0r2lPGyp",
  "createdAt": "2026-04-19T11:23:52.226786Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776597799939-2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 3,
  "remittanceId": "c7f52cb1-9965-40d8-9e1a-927008dd9da6",
  "senderId": "e2e-10x-1776597799939-2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "44d16798-e4fa-4530-aa6d-1e0a6d81c767",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:24:01.942099622Z",
  "updatedAt": "2026-04-19T11:24:01.946288239Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/c7f52cb1-9965-40d8-9e1a-927008dd9da6`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 3,
  "remittanceId": "c7f52cb1-9965-40d8-9e1a-927008dd9da6",
  "senderId": "e2e-10x-1776597799939-2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "44d16798-e4fa-4530-aa6d-1e0a6d81c767",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:24:01.942100Z",
  "updatedAt": "2026-04-19T11:24:03.070156Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/44d16798-e4fa-4530-aa6d-1e0a6d81c767`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "c7f52cb1-9965-40d8-9e1a-927008dd9da6",
  "senderId": "e2e-10x-1776597799939-2",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:24:01.945198Z"
}
```

#### 7-submit-claim

`POST /api/claims/44d16798-e4fa-4530-aa6d-1e0a6d81c767`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "c7f52cb1-9965-40d8-9e1a-927008dd9da6",
  "senderId": "e2e-10x-1776597799939-2",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:24:01.945198Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/c7f52cb1-9965-40d8-9e1a-927008dd9da6`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 3,
  "remittanceId": "c7f52cb1-9965-40d8-9e1a-927008dd9da6",
  "senderId": "e2e-10x-1776597799939-2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "44d16798-e4fa-4530-aa6d-1e0a6d81c767",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:24:01.942100Z",
  "updatedAt": "2026-04-19T11:24:06.602173Z",
  "expiresAt": null
}
```

---

### Customer 3 — `e2e-10x-1776597849019-3`

- walletId: `10`  solanaAddress: `GvqrmXdXV6Eak8NRP3F24FAQ2EzP3JkXbzkXarbyeXfZ`
- fundingId: `6f108599-3f3a-4e1c-bf41-d3e5b5c9d488`  paymentIntentId: `pi_3TNtSQ3nnME1dfOB0siozHXF`
- remittanceId: `53c43ad7-6fda-4b0d-8c6f-310d6218cecc`  claimTokenId: `88d625be-bb00-4fb2-be56-f2d2b452683e`
- final status: **DELIVERED**  elapsed: 46.4s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 29
- Payout: id=`pout_wm_gyf0zd8q7idy` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776597849019-3"
}
```

Response:
```json
{
  "id": 10,
  "userId": "e2e-10x-1776597849019-3",
  "solanaAddress": "GvqrmXdXV6Eak8NRP3F24FAQ2EzP3JkXbzkXarbyeXfZ",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:24:09.110994670Z",
  "updatedAt": "2026-04-19T11:24:09.110994670Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/10/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "6f108599-3f3a-4e1c-bf41-d3e5b5c9d488",
  "walletId": 10,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtSQ3nnME1dfOB0siozHXF",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:24:09.128502434Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/6f108599-3f3a-4e1c-bf41-d3e5b5c9d488`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "6f108599-3f3a-4e1c-bf41-d3e5b5c9d488",
  "walletId": 10,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtSQ3nnME1dfOB0siozHXF",
  "createdAt": "2026-04-19T11:24:09.128502Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776597849019-3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 4,
  "remittanceId": "53c43ad7-6fda-4b0d-8c6f-310d6218cecc",
  "senderId": "e2e-10x-1776597849019-3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "88d625be-bb00-4fb2-be56-f2d2b452683e",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:24:18.065621408Z",
  "updatedAt": "2026-04-19T11:24:18.069339354Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/53c43ad7-6fda-4b0d-8c6f-310d6218cecc`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 4,
  "remittanceId": "53c43ad7-6fda-4b0d-8c6f-310d6218cecc",
  "senderId": "e2e-10x-1776597849019-3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "88d625be-bb00-4fb2-be56-f2d2b452683e",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:24:18.065621Z",
  "updatedAt": "2026-04-19T11:24:49.479968Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/88d625be-bb00-4fb2-be56-f2d2b452683e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "53c43ad7-6fda-4b0d-8c6f-310d6218cecc",
  "senderId": "e2e-10x-1776597849019-3",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:24:18.068118Z"
}
```

#### 7-submit-claim

`POST /api/claims/88d625be-bb00-4fb2-be56-f2d2b452683e`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "53c43ad7-6fda-4b0d-8c6f-310d6218cecc",
  "senderId": "e2e-10x-1776597849019-3",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:24:18.068118Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/53c43ad7-6fda-4b0d-8c6f-310d6218cecc`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 4,
  "remittanceId": "53c43ad7-6fda-4b0d-8c6f-310d6218cecc",
  "senderId": "e2e-10x-1776597849019-3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "88d625be-bb00-4fb2-be56-f2d2b452683e",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:24:18.065621Z",
  "updatedAt": "2026-04-19T11:24:52.866001Z",
  "expiresAt": null
}
```

---

### Customer 4 — `e2e-10x-1776597895402-4`

- walletId: `11`  solanaAddress: `UqtBgvFcJkagupTZn14Wwe1NkhNj6iD8rgQZ2omyTUR`
- fundingId: `f0b2e2f6-b26e-4ac1-93d8-d7b50d85fa5f`  paymentIntentId: `pi_3TNtTA3nnME1dfOB06dB2yza`
- remittanceId: `3b296846-fe40-41a3-ade8-eda73f5e8af2`  claimTokenId: `607e544f-2ec9-4d08-a684-947c25a2c1be`
- final status: **DELIVERED**  elapsed: 16.7s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 30
- Payout: id=`pout_wm_kvrbr3peaes8` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776597895402-4"
}
```

Response:
```json
{
  "id": 11,
  "userId": "e2e-10x-1776597895402-4",
  "solanaAddress": "UqtBgvFcJkagupTZn14Wwe1NkhNj6iD8rgQZ2omyTUR",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:24:55.485703917Z",
  "updatedAt": "2026-04-19T11:24:55.485703917Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/11/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "f0b2e2f6-b26e-4ac1-93d8-d7b50d85fa5f",
  "walletId": 11,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtTA3nnME1dfOB06dB2yza",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:24:55.503511475Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/f0b2e2f6-b26e-4ac1-93d8-d7b50d85fa5f`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "f0b2e2f6-b26e-4ac1-93d8-d7b50d85fa5f",
  "walletId": 11,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtTA3nnME1dfOB06dB2yza",
  "createdAt": "2026-04-19T11:24:55.503511Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776597895402-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 5,
  "remittanceId": "3b296846-fe40-41a3-ade8-eda73f5e8af2",
  "senderId": "e2e-10x-1776597895402-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "607e544f-2ec9-4d08-a684-947c25a2c1be",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:25:05.082749926Z",
  "updatedAt": "2026-04-19T11:25:05.090090234Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/3b296846-fe40-41a3-ade8-eda73f5e8af2`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 5,
  "remittanceId": "3b296846-fe40-41a3-ade8-eda73f5e8af2",
  "senderId": "e2e-10x-1776597895402-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "607e544f-2ec9-4d08-a684-947c25a2c1be",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:25:05.082750Z",
  "updatedAt": "2026-04-19T11:25:06.166160Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/607e544f-2ec9-4d08-a684-947c25a2c1be`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "3b296846-fe40-41a3-ade8-eda73f5e8af2",
  "senderId": "e2e-10x-1776597895402-4",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:25:05.087736Z"
}
```

#### 7-submit-claim

`POST /api/claims/607e544f-2ec9-4d08-a684-947c25a2c1be`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "3b296846-fe40-41a3-ade8-eda73f5e8af2",
  "senderId": "e2e-10x-1776597895402-4",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:25:05.087736Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/3b296846-fe40-41a3-ade8-eda73f5e8af2`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 5,
  "remittanceId": "3b296846-fe40-41a3-ade8-eda73f5e8af2",
  "senderId": "e2e-10x-1776597895402-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "607e544f-2ec9-4d08-a684-947c25a2c1be",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:25:05.082750Z",
  "updatedAt": "2026-04-19T11:25:09.402940Z",
  "expiresAt": null
}
```

---

### Customer 5 — `e2e-10x-1776597912110-5`

- walletId: `12`  solanaAddress: `9RsH8fyUypCxnz4xD1KF6MhxaU6cfU4jGSkbGcRKwS5g`
- fundingId: `4d43b24a-0f9b-4f01-8b98-3c8342e80f53`  paymentIntentId: `pi_3TNtTy3nnME1dfOB1uBLkSQH`
- remittanceId: `721ffdb1-9f30-426b-97ee-565f197549d2`  claimTokenId: `fdc7ec65-d273-4d6a-83d9-df68e3fbe5cc`
- final status: **DELIVERED**  elapsed: 113.0s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 31
- Payout: id=`pout_wm_tg980vmggklj` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776597912110-5"
}
```

Response:
```json
{
  "id": 12,
  "userId": "e2e-10x-1776597912110-5",
  "solanaAddress": "9RsH8fyUypCxnz4xD1KF6MhxaU6cfU4jGSkbGcRKwS5g",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:25:44.274575379Z",
  "updatedAt": "2026-04-19T11:25:44.274575379Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/12/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "4d43b24a-0f9b-4f01-8b98-3c8342e80f53",
  "walletId": 12,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtTy3nnME1dfOB1uBLkSQH",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:25:44.340020702Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/4d43b24a-0f9b-4f01-8b98-3c8342e80f53`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "4d43b24a-0f9b-4f01-8b98-3c8342e80f53",
  "walletId": 12,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtTy3nnME1dfOB1uBLkSQH",
  "createdAt": "2026-04-19T11:25:44.340021Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776597912110-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 6,
  "remittanceId": "721ffdb1-9f30-426b-97ee-565f197549d2",
  "senderId": "e2e-10x-1776597912110-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "fdc7ec65-d273-4d6a-83d9-df68e3fbe5cc",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:25:54.292973907Z",
  "updatedAt": "2026-04-19T11:25:54.302824570Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/721ffdb1-9f30-426b-97ee-565f197549d2`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 6,
  "remittanceId": "721ffdb1-9f30-426b-97ee-565f197549d2",
  "senderId": "e2e-10x-1776597912110-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "fdc7ec65-d273-4d6a-83d9-df68e3fbe5cc",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:25:54.292974Z",
  "updatedAt": "2026-04-19T11:26:58.460790Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/fdc7ec65-d273-4d6a-83d9-df68e3fbe5cc`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "721ffdb1-9f30-426b-97ee-565f197549d2",
  "senderId": "e2e-10x-1776597912110-5",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:25:54.297624Z"
}
```

#### 7-submit-claim

`POST /api/claims/fdc7ec65-d273-4d6a-83d9-df68e3fbe5cc`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "721ffdb1-9f30-426b-97ee-565f197549d2",
  "senderId": "e2e-10x-1776597912110-5",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:25:54.297624Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/721ffdb1-9f30-426b-97ee-565f197549d2`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 6,
  "remittanceId": "721ffdb1-9f30-426b-97ee-565f197549d2",
  "senderId": "e2e-10x-1776597912110-5",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "fdc7ec65-d273-4d6a-83d9-df68e3fbe5cc",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:25:54.292974Z",
  "updatedAt": "2026-04-19T11:27:02.456120Z",
  "expiresAt": null
}
```

---

### Customer 6 — `e2e-10x-1776598025160-6`

- walletId: `13`  solanaAddress: `51GMAD8xXDEN9jr8virrMkMBXVrG2RAV49etrsMbs6gV`
- fundingId: `108dfcb4-9e95-47ee-a7c1-7b5c60c0e437`  paymentIntentId: `pi_3TNtVG3nnME1dfOB1t6dXwZU`
- remittanceId: `b9d7812a-aae2-448e-8b40-368810451de6`  claimTokenId: `9aea7175-5647-4f1d-90f4-80126fdc28a7`
- final status: **DELIVERED**  elapsed: 50.8s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 32
- Payout: id=`pout_wm_pqbbwa8babew` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776598025160-6"
}
```

Response:
```json
{
  "id": 13,
  "userId": "e2e-10x-1776598025160-6",
  "solanaAddress": "51GMAD8xXDEN9jr8virrMkMBXVrG2RAV49etrsMbs6gV",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:27:05.320014949Z",
  "updatedAt": "2026-04-19T11:27:05.320014949Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/13/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "108dfcb4-9e95-47ee-a7c1-7b5c60c0e437",
  "walletId": 13,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtVG3nnME1dfOB1t6dXwZU",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:27:05.352018460Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/108dfcb4-9e95-47ee-a7c1-7b5c60c0e437`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "108dfcb4-9e95-47ee-a7c1-7b5c60c0e437",
  "walletId": 13,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtVG3nnME1dfOB1t6dXwZU",
  "createdAt": "2026-04-19T11:27:05.352018Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776598025160-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 7,
  "remittanceId": "b9d7812a-aae2-448e-8b40-368810451de6",
  "senderId": "e2e-10x-1776598025160-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "9aea7175-5647-4f1d-90f4-80126fdc28a7",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:27:15.108897695Z",
  "updatedAt": "2026-04-19T11:27:15.118965735Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/b9d7812a-aae2-448e-8b40-368810451de6`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 7,
  "remittanceId": "b9d7812a-aae2-448e-8b40-368810451de6",
  "senderId": "e2e-10x-1776598025160-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "9aea7175-5647-4f1d-90f4-80126fdc28a7",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:27:15.108898Z",
  "updatedAt": "2026-04-19T11:27:48.830549Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/9aea7175-5647-4f1d-90f4-80126fdc28a7`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "b9d7812a-aae2-448e-8b40-368810451de6",
  "senderId": "e2e-10x-1776598025160-6",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:27:15.116943Z"
}
```

#### 7-submit-claim

`POST /api/claims/9aea7175-5647-4f1d-90f4-80126fdc28a7`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "b9d7812a-aae2-448e-8b40-368810451de6",
  "senderId": "e2e-10x-1776598025160-6",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:27:15.116943Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/b9d7812a-aae2-448e-8b40-368810451de6`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 7,
  "remittanceId": "b9d7812a-aae2-448e-8b40-368810451de6",
  "senderId": "e2e-10x-1776598025160-6",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "9aea7175-5647-4f1d-90f4-80126fdc28a7",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:27:15.108898Z",
  "updatedAt": "2026-04-19T11:27:53.242882Z",
  "expiresAt": null
}
```

---

### Customer 7 — `e2e-10x-1776598075964-7`

- walletId: `14`  solanaAddress: `Fwex4a76GAFvsYEA8XDWYfFu4xBPTNdgmgBa8tzNCdCY`
- fundingId: `8ee040a8-a3a6-459d-a90f-71138b3d65bd`  paymentIntentId: `pi_3TNtW53nnME1dfOB1KfgIqZa`
- remittanceId: `1dee3072-adc6-4872-8fbb-593675866249`  claimTokenId: `ac5d8a94-830f-4cf2-802a-24f064e08681`
- final status: **DELIVERED**  elapsed: 22.9s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 33
- Payout: id=`pout_wm_6jarhbmk9wio` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776598075964-7"
}
```

Response:
```json
{
  "id": 14,
  "userId": "e2e-10x-1776598075964-7",
  "solanaAddress": "Fwex4a76GAFvsYEA8XDWYfFu4xBPTNdgmgBa8tzNCdCY",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:27:56.061156720Z",
  "updatedAt": "2026-04-19T11:27:56.061156720Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/14/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "8ee040a8-a3a6-459d-a90f-71138b3d65bd",
  "walletId": 14,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtW53nnME1dfOB1KfgIqZa",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:27:56.084879579Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/8ee040a8-a3a6-459d-a90f-71138b3d65bd`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "8ee040a8-a3a6-459d-a90f-71138b3d65bd",
  "walletId": 14,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtW53nnME1dfOB1KfgIqZa",
  "createdAt": "2026-04-19T11:27:56.084880Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776598075964-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 8,
  "remittanceId": "1dee3072-adc6-4872-8fbb-593675866249",
  "senderId": "e2e-10x-1776598075964-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "ac5d8a94-830f-4cf2-802a-24f064e08681",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:28:11.801409970Z",
  "updatedAt": "2026-04-19T11:28:11.808384151Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/1dee3072-adc6-4872-8fbb-593675866249`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 8,
  "remittanceId": "1dee3072-adc6-4872-8fbb-593675866249",
  "senderId": "e2e-10x-1776598075964-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "ac5d8a94-830f-4cf2-802a-24f064e08681",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:28:11.801410Z",
  "updatedAt": "2026-04-19T11:28:12.958556Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/ac5d8a94-830f-4cf2-802a-24f064e08681`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "1dee3072-adc6-4872-8fbb-593675866249",
  "senderId": "e2e-10x-1776598075964-7",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:28:11.805796Z"
}
```

#### 7-submit-claim

`POST /api/claims/ac5d8a94-830f-4cf2-802a-24f064e08681`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "1dee3072-adc6-4872-8fbb-593675866249",
  "senderId": "e2e-10x-1776598075964-7",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:28:11.805796Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/1dee3072-adc6-4872-8fbb-593675866249`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 8,
  "remittanceId": "1dee3072-adc6-4872-8fbb-593675866249",
  "senderId": "e2e-10x-1776598075964-7",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "ac5d8a94-830f-4cf2-802a-24f064e08681",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:28:11.801410Z",
  "updatedAt": "2026-04-19T11:28:16.173532Z",
  "expiresAt": null
}
```

---

### Customer 8 — `e2e-10x-1776598098872-8`

- walletId: `15`  solanaAddress: `CogKwMA8W5oVBhE8fMUKVQKdVBbB6y6uQ5ggCgiytdF3`
- fundingId: `99f047f0-ab7f-415c-9960-66de2917cb54`  paymentIntentId: `pi_3TNtWS3nnME1dfOB1oC0Ax7k`
- remittanceId: `9c6d725f-32dc-42df-bc00-3eaff10f8c9d`  claimTokenId: `73835f3a-0c6e-4d91-9fc4-0f76391e46ac`
- final status: **DELIVERED**  elapsed: 16.3s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 34
- Payout: id=`pout_wm_u5hmkb1zwkd0` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776598098872-8"
}
```

Response:
```json
{
  "id": 15,
  "userId": "e2e-10x-1776598098872-8",
  "solanaAddress": "CogKwMA8W5oVBhE8fMUKVQKdVBbB6y6uQ5ggCgiytdF3",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:28:18.977911813Z",
  "updatedAt": "2026-04-19T11:28:18.977911813Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/15/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "99f047f0-ab7f-415c-9960-66de2917cb54",
  "walletId": 15,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtWS3nnME1dfOB1oC0Ax7k",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:28:18.997408221Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/99f047f0-ab7f-415c-9960-66de2917cb54`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "99f047f0-ab7f-415c-9960-66de2917cb54",
  "walletId": 15,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtWS3nnME1dfOB1oC0Ax7k",
  "createdAt": "2026-04-19T11:28:18.997408Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776598098872-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 9,
  "remittanceId": "9c6d725f-32dc-42df-bc00-3eaff10f8c9d",
  "senderId": "e2e-10x-1776598098872-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "73835f3a-0c6e-4d91-9fc4-0f76391e46ac",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:28:27.892978521Z",
  "updatedAt": "2026-04-19T11:28:27.912344928Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/9c6d725f-32dc-42df-bc00-3eaff10f8c9d`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 9,
  "remittanceId": "9c6d725f-32dc-42df-bc00-3eaff10f8c9d",
  "senderId": "e2e-10x-1776598098872-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "73835f3a-0c6e-4d91-9fc4-0f76391e46ac",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:28:27.892979Z",
  "updatedAt": "2026-04-19T11:28:28.804371Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/73835f3a-0c6e-4d91-9fc4-0f76391e46ac`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "9c6d725f-32dc-42df-bc00-3eaff10f8c9d",
  "senderId": "e2e-10x-1776598098872-8",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:28:27.908348Z"
}
```

#### 7-submit-claim

`POST /api/claims/73835f3a-0c6e-4d91-9fc4-0f76391e46ac`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "9c6d725f-32dc-42df-bc00-3eaff10f8c9d",
  "senderId": "e2e-10x-1776598098872-8",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:28:27.908348Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/9c6d725f-32dc-42df-bc00-3eaff10f8c9d`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 9,
  "remittanceId": "9c6d725f-32dc-42df-bc00-3eaff10f8c9d",
  "senderId": "e2e-10x-1776598098872-8",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "73835f3a-0c6e-4d91-9fc4-0f76391e46ac",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:28:27.892979Z",
  "updatedAt": "2026-04-19T11:28:32.418422Z",
  "expiresAt": null
}
```

---

### Customer 9 — `e2e-10x-1776598115130-9`

- walletId: `16`  solanaAddress: `74GboL5qRGncSJJYXCrFgabNNpfweqUuC5gfF8YQmfA4`
- fundingId: `32466a08-4bcc-4b60-9e98-4919e12ad5a0`  paymentIntentId: `pi_3TNtWi3nnME1dfOB0Yevm0QL`
- remittanceId: `af3b0178-7931-4d19-9721-bb3e96727a1c`  claimTokenId: `131682fa-885a-4dab-b44b-7b9cf65c791e`
- final status: **DELIVERED**  elapsed: 46.8s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 35
- Payout: id=`pout_wm_pfi0ubnabdur` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776598115130-9"
}
```

Response:
```json
{
  "id": 16,
  "userId": "e2e-10x-1776598115130-9",
  "solanaAddress": "74GboL5qRGncSJJYXCrFgabNNpfweqUuC5gfF8YQmfA4",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:28:35.330552520Z",
  "updatedAt": "2026-04-19T11:28:35.330552520Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/16/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "32466a08-4bcc-4b60-9e98-4919e12ad5a0",
  "walletId": 16,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtWi3nnME1dfOB0Yevm0QL",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:28:35.375705052Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/32466a08-4bcc-4b60-9e98-4919e12ad5a0`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "32466a08-4bcc-4b60-9e98-4919e12ad5a0",
  "walletId": 16,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtWi3nnME1dfOB0Yevm0QL",
  "createdAt": "2026-04-19T11:28:35.375705Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776598115130-9",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 10,
  "remittanceId": "af3b0178-7931-4d19-9721-bb3e96727a1c",
  "senderId": "e2e-10x-1776598115130-9",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "131682fa-885a-4dab-b44b-7b9cf65c791e",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:28:44.550884574Z",
  "updatedAt": "2026-04-19T11:28:44.556493453Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/af3b0178-7931-4d19-9721-bb3e96727a1c`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 10,
  "remittanceId": "af3b0178-7931-4d19-9721-bb3e96727a1c",
  "senderId": "e2e-10x-1776598115130-9",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "131682fa-885a-4dab-b44b-7b9cf65c791e",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:28:44.550885Z",
  "updatedAt": "2026-04-19T11:29:15.976473Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/131682fa-885a-4dab-b44b-7b9cf65c791e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "af3b0178-7931-4d19-9721-bb3e96727a1c",
  "senderId": "e2e-10x-1776598115130-9",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:28:44.555437Z"
}
```

#### 7-submit-claim

`POST /api/claims/131682fa-885a-4dab-b44b-7b9cf65c791e`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "af3b0178-7931-4d19-9721-bb3e96727a1c",
  "senderId": "e2e-10x-1776598115130-9",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:28:44.555437Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/af3b0178-7931-4d19-9721-bb3e96727a1c`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 10,
  "remittanceId": "af3b0178-7931-4d19-9721-bb3e96727a1c",
  "senderId": "e2e-10x-1776598115130-9",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "131682fa-885a-4dab-b44b-7b9cf65c791e",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:28:44.550885Z",
  "updatedAt": "2026-04-19T11:29:19.498799Z",
  "expiresAt": null
}
```

---

### Customer 10 — `e2e-10x-1776598161943-10`

- walletId: `17`  solanaAddress: `G37Th6CcvAVRtjUw5acdDCdJmn3y7H8JTKB9W2ViKze9`
- fundingId: `7a7a7dc6-fd38-4e2a-8572-21cf8b0110ca`  paymentIntentId: `pi_3TNtXT3nnME1dfOB0n69tF9W`
- remittanceId: `5138141b-9a20-461f-b7df-e4dbf27d6d46`  claimTokenId: `765aac14-5e5f-49b1-9943-08806535ed75`
- final status: **DELIVERED**  elapsed: 47.4s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: 36
- Payout: id=`pout_wm_mmxjbzzuhsnk` status=`processing`

#### 1-create-wallet

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776598161943-10"
}
```

Response:
```json
{
  "id": 17,
  "userId": "e2e-10x-1776598161943-10",
  "solanaAddress": "G37Th6CcvAVRtjUw5acdDCdJmn3y7H8JTKB9W2ViKze9",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-19T11:29:22.027111483Z",
  "updatedAt": "2026-04-19T11:29:22.027111483Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/17/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "7a7a7dc6-fd38-4e2a-8572-21cf8b0110ca",
  "walletId": 17,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNtXT3nnME1dfOB0n69tF9W",
  "stripeClientSecret": "***REDACTED***",
  "createdAt": "2026-04-19T11:29:22.040386216Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/7a7a7dc6-fd38-4e2a-8572-21cf8b0110ca`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "7a7a7dc6-fd38-4e2a-8572-21cf8b0110ca",
  "walletId": 17,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNtXT3nnME1dfOB0n69tF9W",
  "createdAt": "2026-04-19T11:29:22.040386Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776598161943-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 11,
  "remittanceId": "5138141b-9a20-461f-b7df-e4dbf27d6d46",
  "senderId": "e2e-10x-1776598161943-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "765aac14-5e5f-49b1-9943-08806535ed75",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:29:31.785075306Z",
  "updatedAt": "2026-04-19T11:29:31.790060930Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/5138141b-9a20-461f-b7df-e4dbf27d6d46`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 11,
  "remittanceId": "5138141b-9a20-461f-b7df-e4dbf27d6d46",
  "senderId": "e2e-10x-1776598161943-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "765aac14-5e5f-49b1-9943-08806535ed75",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:29:31.785075Z",
  "updatedAt": "2026-04-19T11:30:03.054652Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/765aac14-5e5f-49b1-9943-08806535ed75`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "5138141b-9a20-461f-b7df-e4dbf27d6d46",
  "senderId": "e2e-10x-1776598161943-10",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-21T11:29:31.788423Z"
}
```

#### 7-submit-claim

`POST /api/claims/765aac14-5e5f-49b1-9943-08806535ed75`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "5138141b-9a20-461f-b7df-e4dbf27d6d46",
  "senderId": "e2e-10x-1776598161943-10",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-21T11:29:31.788423Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/5138141b-9a20-461f-b7df-e4dbf27d6d46`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 11,
  "remittanceId": "5138141b-9a20-461f-b7df-e4dbf27d6d46",
  "senderId": "e2e-10x-1776598161943-10",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.92,
  "fxRate": 92.922391,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "765aac14-5e5f-49b1-9943-08806535ed75",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-19T11:29:31.785075Z",
  "updatedAt": "2026-04-19T11:30:06.334869Z",
  "expiresAt": null
}
```

---
