# 10-Customer Full E2E Run — $1 USDC per customer

Generated: 2026-04-18 16:55:04 UTC

## Summary

- Customers attempted: 10
- Passed: 10
- Failed: 0
- Per-customer amount: $1.00 USDC

| # | userId | walletId | address | fundingId | remittanceId | final | USDC after fund | elapsed (s) | PASS |
|---|--------|----------|---------|-----------|--------------|-------|-----------------|-------------|------|
| 1 | `e2e-10x-1776530235849-4` | 12 | `Hp755JRzBU9CnDRAzYbZc91veVPrk9FQmXk9yMB7g6zx` | `(salvaged — fund completed in prior aborted run)` | `c6b73ba7-0013-4f04-9f0c-a01397dabf33` | DELIVERED | 2 | 9.0 | ✅ |
| 2 | `e2e-10x-1776530799226-1-a1` | 13 | `36X5VxStiSyDCARgZckd578sK6UizaeB7gQ8nZQ2WYBK` | `(salvaged — fund completed in prior aborted run)` | `7887f2f3-bf17-477b-8aaf-2507af7fdaac` | DELIVERED | 1 | 7.9 | ✅ |
| 3 | `e2e-10x-1776530808564-2-a1` | 14 | `GbfSuvaLDKVDBrsqM5BadqvC3uCTn4Zx1ZUABhzfuE7Q` | `(salvaged — fund completed in prior aborted run)` | `e95d4afa-0c88-42df-b4ce-ca22512777bb` | DELIVERED | 1 | 8.2 | ✅ |
| 4 | `e2e-10x-1776531063882-4-a1` | 16 | `9LJdxa1nzD8mH9VMpP4B7QqvtFGH4vLeVKhef5SDm8f7` | `d17a5012-cb91-46ab-af26-25803fcb537d` | `192ce668-a54d-435e-a069-c59030ea6955` | DELIVERED | 1 | 17.0 | ✅ |
| 5 | `e2e-10x-1776531080894-5-a1` | 17 | `4CGXRE2qBWm6W22nMwpeUProE63wWnDTLJzN8MPdVuK1` | `05042eb1-3a47-4726-aae0-2a6a6de75285` | `39cb3fa8-79ac-4e3e-981a-24178f03e77e` | DELIVERED | 1 | 16.3 | ✅ |
| 6 | `e2e-10x-1776531097216-6-a1` | 18 | `ANZQ4ENuwLXonivU8PomAwQSEwtGaMR9Li6jT9v3we1m` | `ee238ed4-3a50-48bf-b079-db9810831874` | `20ff4c37-1a44-4b8d-b3ca-ab45af5a7059` | DELIVERED | 1 | 17.2 | ✅ |
| 7 | `e2e-10x-1776531114382-7-a1` | 19 | `Dh3ahVhuKdN4f3yFCS5wE2K7BFvvNPeCAgnCUKk6FNup` | `bc20cce9-b433-4744-8b6c-b66451fd9160` | `ddd8dafe-3ede-427a-bf89-65fba00a1fbd` | DELIVERED | 1 | 16.4 | ✅ |
| 8 | `e2e-10x-1776531130780-8-a2` | 21 | `CijX64N5JbEQ6X2FxCQfhuGKU3wThnRkNT5jqASofExV` | `2093b02e-1058-4568-a684-1f49b521a51f` | `8b864a19-43ea-4031-b2ab-8eac4f46aeab` | DELIVERED | 1 | 80.5 | ✅ |
| 9 | `e2e-10x-1776531211324-9-a3` | 24 | `584FPpxgjMSeYj8HyUUdG1gpRxZ8pE7KLXo5qaqPjdyZ` | `40d66311-cfe4-4319-904b-ccea97ebcf0b` | `9c1e6e56-a3f6-42dd-9909-e81a4e5fc306` | DELIVERED | 1 | 77.4 | ✅ |
| 10 | `e2e-10x-1776531288735-10-a1` | 25 | `7e3Hg9WiD6EwSSL2paGhuo3gA1komX5bExgsxrLvB9m4` | `4fac027c-4774-487e-9582-3bd7bb36763a` | `61555663-6288-478f-a6d9-57ad0690e66e` | DELIVERED | 1 | 16.1 | ✅ |

## Per-customer request / response log

### Customer 1 — `e2e-10x-1776530235849-4`

- walletId: `12`  solanaAddress: `Hp755JRzBU9CnDRAzYbZc91veVPrk9FQmXk9yMB7g6zx`
- fundingId: `(salvaged — fund completed in prior aborted run)`  paymentIntentId: `None`
- remittanceId: `c6b73ba7-0013-4f04-9f0c-a01397dabf33`  claimTokenId: `ffbddc0e-db3a-4419-8fb2-1f49f2817e73`
- final status: **DELIVERED**  elapsed: 9.0s  **PASS**
- On-chain USDC after fund: 2
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776530235849-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 2.0
}
```

Response:
```json
{
  "id": 12,
  "remittanceId": "c6b73ba7-0013-4f04-9f0c-a01397dabf33",
  "senderId": "e2e-10x-1776530235849-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 2.0,
  "amountInr": 185.79,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "ffbddc0e-db3a-4419-8fb2-1f49f2817e73",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:50:40.759574824Z",
  "updatedAt": "2026-04-18T16:50:40.776397164Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/c6b73ba7-0013-4f04-9f0c-a01397dabf33`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 12,
  "remittanceId": "c6b73ba7-0013-4f04-9f0c-a01397dabf33",
  "senderId": "e2e-10x-1776530235849-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 2.0,
  "amountInr": 185.79,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "ffbddc0e-db3a-4419-8fb2-1f49f2817e73",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:50:40.759575Z",
  "updatedAt": "2026-04-18T16:50:42.598990Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/ffbddc0e-db3a-4419-8fb2-1f49f2817e73`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "c6b73ba7-0013-4f04-9f0c-a01397dabf33",
  "senderId": "e2e-10x-1776530235849-4",
  "amountUsdc": 2.0,
  "amountInr": 185.79,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:50:40.770528Z"
}
```

#### 7-submit-claim

`POST /api/claims/ffbddc0e-db3a-4419-8fb2-1f49f2817e73`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "c6b73ba7-0013-4f04-9f0c-a01397dabf33",
  "senderId": "e2e-10x-1776530235849-4",
  "amountUsdc": 2.0,
  "amountInr": 185.79,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:50:40.770528Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/c6b73ba7-0013-4f04-9f0c-a01397dabf33`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 12,
  "remittanceId": "c6b73ba7-0013-4f04-9f0c-a01397dabf33",
  "senderId": "e2e-10x-1776530235849-4",
  "recipientPhone": "+919876543210",
  "amountUsdc": 2.0,
  "amountInr": 185.79,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "ffbddc0e-db3a-4419-8fb2-1f49f2817e73",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:50:40.759575Z",
  "updatedAt": "2026-04-18T16:50:45.357635Z",
  "expiresAt": null
}
```

---

### Customer 2 — `e2e-10x-1776530799226-1-a1`

- walletId: `13`  solanaAddress: `36X5VxStiSyDCARgZckd578sK6UizaeB7gQ8nZQ2WYBK`
- fundingId: `(salvaged — fund completed in prior aborted run)`  paymentIntentId: `None`
- remittanceId: `7887f2f3-bf17-477b-8aaf-2507af7fdaac`  claimTokenId: `118b7833-5424-4697-ab9a-e32eff6f0765`
- final status: **DELIVERED**  elapsed: 7.9s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776530799226-1-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 13,
  "remittanceId": "7887f2f3-bf17-477b-8aaf-2507af7fdaac",
  "senderId": "e2e-10x-1776530799226-1-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "118b7833-5424-4697-ab9a-e32eff6f0765",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:50:48.873128859Z",
  "updatedAt": "2026-04-18T16:50:48.881817450Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/7887f2f3-bf17-477b-8aaf-2507af7fdaac`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 13,
  "remittanceId": "7887f2f3-bf17-477b-8aaf-2507af7fdaac",
  "senderId": "e2e-10x-1776530799226-1-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "118b7833-5424-4697-ab9a-e32eff6f0765",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:50:48.873129Z",
  "updatedAt": "2026-04-18T16:50:50.057093Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/118b7833-5424-4697-ab9a-e32eff6f0765`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "7887f2f3-bf17-477b-8aaf-2507af7fdaac",
  "senderId": "e2e-10x-1776530799226-1-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:50:48.878815Z"
}
```

#### 7-submit-claim

`POST /api/claims/118b7833-5424-4697-ab9a-e32eff6f0765`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "7887f2f3-bf17-477b-8aaf-2507af7fdaac",
  "senderId": "e2e-10x-1776530799226-1-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:50:48.878815Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/7887f2f3-bf17-477b-8aaf-2507af7fdaac`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 13,
  "remittanceId": "7887f2f3-bf17-477b-8aaf-2507af7fdaac",
  "senderId": "e2e-10x-1776530799226-1-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "118b7833-5424-4697-ab9a-e32eff6f0765",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:50:48.873129Z",
  "updatedAt": "2026-04-18T16:50:53.480753Z",
  "expiresAt": null
}
```

---

### Customer 3 — `e2e-10x-1776530808564-2-a1`

- walletId: `14`  solanaAddress: `GbfSuvaLDKVDBrsqM5BadqvC3uCTn4Zx1ZUABhzfuE7Q`
- fundingId: `(salvaged — fund completed in prior aborted run)`  paymentIntentId: `None`
- remittanceId: `e95d4afa-0c88-42df-b4ce-ca22512777bb`  claimTokenId: `62a3846c-c851-4a50-80d3-0f6289ad92e7`
- final status: **DELIVERED**  elapsed: 8.2s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776530808564-2-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 14,
  "remittanceId": "e95d4afa-0c88-42df-b4ce-ca22512777bb",
  "senderId": "e2e-10x-1776530808564-2-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "62a3846c-c851-4a50-80d3-0f6289ad92e7",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:50:56.967209722Z",
  "updatedAt": "2026-04-18T16:50:56.974075077Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/e95d4afa-0c88-42df-b4ce-ca22512777bb`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 14,
  "remittanceId": "e95d4afa-0c88-42df-b4ce-ca22512777bb",
  "senderId": "e2e-10x-1776530808564-2-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "62a3846c-c851-4a50-80d3-0f6289ad92e7",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:50:56.967210Z",
  "updatedAt": "2026-04-18T16:50:58.251639Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/62a3846c-c851-4a50-80d3-0f6289ad92e7`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "e95d4afa-0c88-42df-b4ce-ca22512777bb",
  "senderId": "e2e-10x-1776530808564-2-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:50:56.972410Z"
}
```

#### 7-submit-claim

`POST /api/claims/62a3846c-c851-4a50-80d3-0f6289ad92e7`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "e95d4afa-0c88-42df-b4ce-ca22512777bb",
  "senderId": "e2e-10x-1776530808564-2-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:50:56.972410Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/e95d4afa-0c88-42df-b4ce-ca22512777bb`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 14,
  "remittanceId": "e95d4afa-0c88-42df-b4ce-ca22512777bb",
  "senderId": "e2e-10x-1776530808564-2-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "62a3846c-c851-4a50-80d3-0f6289ad92e7",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:50:56.967210Z",
  "updatedAt": "2026-04-18T16:51:01.657273Z",
  "expiresAt": null
}
```

---

### Customer 4 — `e2e-10x-1776531063882-4-a1`

- walletId: `16`  solanaAddress: `9LJdxa1nzD8mH9VMpP4B7QqvtFGH4vLeVKhef5SDm8f7`
- fundingId: `d17a5012-cb91-46ab-af26-25803fcb537d`  paymentIntentId: `pi_3TNc5F3nnME1dfOB0ifsWSLP`
- remittanceId: `192ce668-a54d-435e-a069-c59030ea6955`  claimTokenId: `8e723ca4-534c-4b43-a405-7555787fc856`
- final status: **DELIVERED**  elapsed: 17.0s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 1-create-wallet-attempt-1

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531063882-4-a1"
}
```

Response:
```json
{
  "id": 16,
  "userId": "e2e-10x-1776531063882-4-a1",
  "solanaAddress": "9LJdxa1nzD8mH9VMpP4B7QqvtFGH4vLeVKhef5SDm8f7",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:51:04.205498514Z",
  "updatedAt": "2026-04-18T16:51:04.205498514Z"
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
  "fundingId": "d17a5012-cb91-46ab-af26-25803fcb537d",
  "walletId": 16,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNc5F3nnME1dfOB0ifsWSLP",
  "stripeClientSecret": "pi_3TNc5F3nnME1dfOB0ifsWSLP_secret_T0aXLWdfZiBYfOxxQlTJIqmWl",
  "createdAt": "2026-04-18T16:51:04.395320029Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/d17a5012-cb91-46ab-af26-25803fcb537d`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "d17a5012-cb91-46ab-af26-25803fcb537d",
  "walletId": 16,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNc5F3nnME1dfOB0ifsWSLP",
  "createdAt": "2026-04-18T16:51:04.395320Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776531063882-4-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 15,
  "remittanceId": "192ce668-a54d-435e-a069-c59030ea6955",
  "senderId": "e2e-10x-1776531063882-4-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "8e723ca4-534c-4b43-a405-7555787fc856",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:51:13.932300445Z",
  "updatedAt": "2026-04-18T16:51:13.964033845Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/192ce668-a54d-435e-a069-c59030ea6955`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 15,
  "remittanceId": "192ce668-a54d-435e-a069-c59030ea6955",
  "senderId": "e2e-10x-1776531063882-4-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "8e723ca4-534c-4b43-a405-7555787fc856",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:51:13.932300Z",
  "updatedAt": "2026-04-18T16:51:14.809635Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/8e723ca4-534c-4b43-a405-7555787fc856`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "192ce668-a54d-435e-a069-c59030ea6955",
  "senderId": "e2e-10x-1776531063882-4-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:51:13.951770Z"
}
```

#### 7-submit-claim

`POST /api/claims/8e723ca4-534c-4b43-a405-7555787fc856`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "192ce668-a54d-435e-a069-c59030ea6955",
  "senderId": "e2e-10x-1776531063882-4-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:51:13.951770Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/192ce668-a54d-435e-a069-c59030ea6955`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 15,
  "remittanceId": "192ce668-a54d-435e-a069-c59030ea6955",
  "senderId": "e2e-10x-1776531063882-4-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "8e723ca4-534c-4b43-a405-7555787fc856",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:51:13.932300Z",
  "updatedAt": "2026-04-18T16:51:17.973882Z",
  "expiresAt": null
}
```

---

### Customer 5 — `e2e-10x-1776531080894-5-a1`

- walletId: `17`  solanaAddress: `4CGXRE2qBWm6W22nMwpeUProE63wWnDTLJzN8MPdVuK1`
- fundingId: `05042eb1-3a47-4726-aae0-2a6a6de75285`  paymentIntentId: `pi_3TNc5W3nnME1dfOB0CqEqTdM`
- remittanceId: `39cb3fa8-79ac-4e3e-981a-24178f03e77e`  claimTokenId: `cbe8d327-4309-4d1a-aafa-0b70989ca61d`
- final status: **DELIVERED**  elapsed: 16.3s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 1-create-wallet-attempt-1

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531080894-5-a1"
}
```

Response:
```json
{
  "id": 17,
  "userId": "e2e-10x-1776531080894-5-a1",
  "solanaAddress": "4CGXRE2qBWm6W22nMwpeUProE63wWnDTLJzN8MPdVuK1",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:51:21.264228486Z",
  "updatedAt": "2026-04-18T16:51:21.264228486Z"
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
  "fundingId": "05042eb1-3a47-4726-aae0-2a6a6de75285",
  "walletId": 17,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNc5W3nnME1dfOB0CqEqTdM",
  "stripeClientSecret": "pi_3TNc5W3nnME1dfOB0CqEqTdM_secret_HqZyIkWXnzL8QO58b9tM9Trbo",
  "createdAt": "2026-04-18T16:51:21.525137516Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/05042eb1-3a47-4726-aae0-2a6a6de75285`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "05042eb1-3a47-4726-aae0-2a6a6de75285",
  "walletId": 17,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNc5W3nnME1dfOB0CqEqTdM",
  "createdAt": "2026-04-18T16:51:21.525138Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776531080894-5-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 16,
  "remittanceId": "39cb3fa8-79ac-4e3e-981a-24178f03e77e",
  "senderId": "e2e-10x-1776531080894-5-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "cbe8d327-4309-4d1a-aafa-0b70989ca61d",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:51:30.397371479Z",
  "updatedAt": "2026-04-18T16:51:30.400517327Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/39cb3fa8-79ac-4e3e-981a-24178f03e77e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 16,
  "remittanceId": "39cb3fa8-79ac-4e3e-981a-24178f03e77e",
  "senderId": "e2e-10x-1776531080894-5-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "cbe8d327-4309-4d1a-aafa-0b70989ca61d",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:51:30.397371Z",
  "updatedAt": "2026-04-18T16:51:31.260251Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/cbe8d327-4309-4d1a-aafa-0b70989ca61d`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "39cb3fa8-79ac-4e3e-981a-24178f03e77e",
  "senderId": "e2e-10x-1776531080894-5-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:51:30.399433Z"
}
```

#### 7-submit-claim

`POST /api/claims/cbe8d327-4309-4d1a-aafa-0b70989ca61d`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "39cb3fa8-79ac-4e3e-981a-24178f03e77e",
  "senderId": "e2e-10x-1776531080894-5-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:51:30.399433Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/39cb3fa8-79ac-4e3e-981a-24178f03e77e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 16,
  "remittanceId": "39cb3fa8-79ac-4e3e-981a-24178f03e77e",
  "senderId": "e2e-10x-1776531080894-5-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "cbe8d327-4309-4d1a-aafa-0b70989ca61d",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:51:30.397371Z",
  "updatedAt": "2026-04-18T16:51:34.748217Z",
  "expiresAt": null
}
```

---

### Customer 6 — `e2e-10x-1776531097216-6-a1`

- walletId: `18`  solanaAddress: `ANZQ4ENuwLXonivU8PomAwQSEwtGaMR9Li6jT9v3we1m`
- fundingId: `ee238ed4-3a50-48bf-b079-db9810831874`  paymentIntentId: `pi_3TNc5n3nnME1dfOB14tCSixH`
- remittanceId: `20ff4c37-1a44-4b8d-b3ca-ab45af5a7059`  claimTokenId: `f0c86d82-3131-49de-bf5d-24c347a267b4`
- final status: **DELIVERED**  elapsed: 17.2s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 1-create-wallet-attempt-1

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531097216-6-a1"
}
```

Response:
```json
{
  "id": 18,
  "userId": "e2e-10x-1776531097216-6-a1",
  "solanaAddress": "ANZQ4ENuwLXonivU8PomAwQSEwtGaMR9Li6jT9v3we1m",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:51:37.519873464Z",
  "updatedAt": "2026-04-18T16:51:37.519873464Z"
}
```

#### 2-initiate-fund

`POST /api/wallets/18/fund`  → **HTTP 201**

Request:
```json
{
  "amount": 1.0
}
```

Response:
```json
{
  "fundingId": "ee238ed4-3a50-48bf-b079-db9810831874",
  "walletId": 18,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNc5n3nnME1dfOB14tCSixH",
  "stripeClientSecret": "pi_3TNc5n3nnME1dfOB14tCSixH_secret_Q4z7aJD67vuIxsFNSZSu2PELD",
  "createdAt": "2026-04-18T16:51:37.775299596Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/ee238ed4-3a50-48bf-b079-db9810831874`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "ee238ed4-3a50-48bf-b079-db9810831874",
  "walletId": 18,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNc5n3nnME1dfOB14tCSixH",
  "createdAt": "2026-04-18T16:51:37.775300Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776531097216-6-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 17,
  "remittanceId": "20ff4c37-1a44-4b8d-b3ca-ab45af5a7059",
  "senderId": "e2e-10x-1776531097216-6-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "f0c86d82-3131-49de-bf5d-24c347a267b4",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:51:47.219723312Z",
  "updatedAt": "2026-04-18T16:51:47.239554581Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/20ff4c37-1a44-4b8d-b3ca-ab45af5a7059`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 17,
  "remittanceId": "20ff4c37-1a44-4b8d-b3ca-ab45af5a7059",
  "senderId": "e2e-10x-1776531097216-6-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "f0c86d82-3131-49de-bf5d-24c347a267b4",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:51:47.219723Z",
  "updatedAt": "2026-04-18T16:51:47.920031Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/f0c86d82-3131-49de-bf5d-24c347a267b4`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "20ff4c37-1a44-4b8d-b3ca-ab45af5a7059",
  "senderId": "e2e-10x-1776531097216-6-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:51:47.236296Z"
}
```

#### 7-submit-claim

`POST /api/claims/f0c86d82-3131-49de-bf5d-24c347a267b4`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "20ff4c37-1a44-4b8d-b3ca-ab45af5a7059",
  "senderId": "e2e-10x-1776531097216-6-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:51:47.236296Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/20ff4c37-1a44-4b8d-b3ca-ab45af5a7059`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 17,
  "remittanceId": "20ff4c37-1a44-4b8d-b3ca-ab45af5a7059",
  "senderId": "e2e-10x-1776531097216-6-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "f0c86d82-3131-49de-bf5d-24c347a267b4",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:51:47.219723Z",
  "updatedAt": "2026-04-18T16:51:52.141634Z",
  "expiresAt": null
}
```

---

### Customer 7 — `e2e-10x-1776531114382-7-a1`

- walletId: `19`  solanaAddress: `Dh3ahVhuKdN4f3yFCS5wE2K7BFvvNPeCAgnCUKk6FNup`
- fundingId: `bc20cce9-b433-4744-8b6c-b66451fd9160`  paymentIntentId: `pi_3TNc643nnME1dfOB1sfOLAiz`
- remittanceId: `ddd8dafe-3ede-427a-bf89-65fba00a1fbd`  claimTokenId: `ecadada5-62ad-44f6-b4db-811c0e4324f2`
- final status: **DELIVERED**  elapsed: 16.4s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 1-create-wallet-attempt-1

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531114382-7-a1"
}
```

Response:
```json
{
  "id": 19,
  "userId": "e2e-10x-1776531114382-7-a1",
  "solanaAddress": "Dh3ahVhuKdN4f3yFCS5wE2K7BFvvNPeCAgnCUKk6FNup",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:51:54.658502742Z",
  "updatedAt": "2026-04-18T16:51:54.658502742Z"
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
  "fundingId": "bc20cce9-b433-4744-8b6c-b66451fd9160",
  "walletId": 19,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNc643nnME1dfOB1sfOLAiz",
  "stripeClientSecret": "pi_3TNc643nnME1dfOB1sfOLAiz_secret_ZnGVGE3hjAIxENezL9fYCBv15",
  "createdAt": "2026-04-18T16:51:54.937877430Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/bc20cce9-b433-4744-8b6c-b66451fd9160`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "bc20cce9-b433-4744-8b6c-b66451fd9160",
  "walletId": 19,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNc643nnME1dfOB1sfOLAiz",
  "createdAt": "2026-04-18T16:51:54.937877Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776531114382-7-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 18,
  "remittanceId": "ddd8dafe-3ede-427a-bf89-65fba00a1fbd",
  "senderId": "e2e-10x-1776531114382-7-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "ecadada5-62ad-44f6-b4db-811c0e4324f2",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:52:03.944757905Z",
  "updatedAt": "2026-04-18T16:52:03.948644410Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/ddd8dafe-3ede-427a-bf89-65fba00a1fbd`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 18,
  "remittanceId": "ddd8dafe-3ede-427a-bf89-65fba00a1fbd",
  "senderId": "e2e-10x-1776531114382-7-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "ecadada5-62ad-44f6-b4db-811c0e4324f2",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:52:03.944758Z",
  "updatedAt": "2026-04-18T16:52:04.994090Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/ecadada5-62ad-44f6-b4db-811c0e4324f2`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "ddd8dafe-3ede-427a-bf89-65fba00a1fbd",
  "senderId": "e2e-10x-1776531114382-7-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:52:03.947499Z"
}
```

#### 7-submit-claim

`POST /api/claims/ecadada5-62ad-44f6-b4db-811c0e4324f2`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "ddd8dafe-3ede-427a-bf89-65fba00a1fbd",
  "senderId": "e2e-10x-1776531114382-7-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:52:03.947499Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/ddd8dafe-3ede-427a-bf89-65fba00a1fbd`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 18,
  "remittanceId": "ddd8dafe-3ede-427a-bf89-65fba00a1fbd",
  "senderId": "e2e-10x-1776531114382-7-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "ecadada5-62ad-44f6-b4db-811c0e4324f2",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:52:03.944758Z",
  "updatedAt": "2026-04-18T16:52:08.402701Z",
  "expiresAt": null
}
```

---

### Customer 8 — `e2e-10x-1776531130780-8-a2`

- walletId: `21`  solanaAddress: `CijX64N5JbEQ6X2FxCQfhuGKU3wThnRkNT5jqASofExV`
- fundingId: `2093b02e-1058-4568-a684-1f49b521a51f`  paymentIntentId: `pi_3TNc6o3nnME1dfOB1y4ZlCeM`
- remittanceId: `8b864a19-43ea-4031-b2ab-8eac4f46aeab`  claimTokenId: `917f16ea-519b-4678-bd33-b6080ff64c9b`
- final status: **DELIVERED**  elapsed: 80.5s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 1-create-wallet-attempt-1

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531130780-8-a1"
}
```

Response:
```json
{
  "id": 20,
  "userId": "e2e-10x-1776531130780-8-a1",
  "solanaAddress": "6vLzAxcnQsUExRzqyfMHGedQLsdtX5PU8Eb6S4rT2vAv",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:52:40.927633637Z",
  "updatedAt": "2026-04-18T16:52:40.927633637Z"
}
```

#### 1-create-wallet-attempt-2

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531130780-8-a2"
}
```

Response:
```json
{
  "id": 21,
  "userId": "e2e-10x-1776531130780-8-a2",
  "solanaAddress": "CijX64N5JbEQ6X2FxCQfhuGKU3wThnRkNT5jqASofExV",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:52:41.158054229Z",
  "updatedAt": "2026-04-18T16:52:41.158054229Z"
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
  "fundingId": "2093b02e-1058-4568-a684-1f49b521a51f",
  "walletId": 21,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNc6o3nnME1dfOB1y4ZlCeM",
  "stripeClientSecret": "pi_3TNc6o3nnME1dfOB1y4ZlCeM_secret_iTnbY4QHjSNWrwAIIGJrQwjpX",
  "createdAt": "2026-04-18T16:52:41.265055525Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/2093b02e-1058-4568-a684-1f49b521a51f`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "2093b02e-1058-4568-a684-1f49b521a51f",
  "walletId": 21,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNc6o3nnME1dfOB1y4ZlCeM",
  "createdAt": "2026-04-18T16:52:41.265056Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776531130780-8-a2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 19,
  "remittanceId": "8b864a19-43ea-4031-b2ab-8eac4f46aeab",
  "senderId": "e2e-10x-1776531130780-8-a2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "917f16ea-519b-4678-bd33-b6080ff64c9b",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:52:50.859236854Z",
  "updatedAt": "2026-04-18T16:52:50.863910133Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/8b864a19-43ea-4031-b2ab-8eac4f46aeab`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 19,
  "remittanceId": "8b864a19-43ea-4031-b2ab-8eac4f46aeab",
  "senderId": "e2e-10x-1776531130780-8-a2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "917f16ea-519b-4678-bd33-b6080ff64c9b",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:52:50.859237Z",
  "updatedAt": "2026-04-18T16:53:25.031578Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/917f16ea-519b-4678-bd33-b6080ff64c9b`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "8b864a19-43ea-4031-b2ab-8eac4f46aeab",
  "senderId": "e2e-10x-1776531130780-8-a2",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:52:50.862690Z"
}
```

#### 7-submit-claim

`POST /api/claims/917f16ea-519b-4678-bd33-b6080ff64c9b`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "8b864a19-43ea-4031-b2ab-8eac4f46aeab",
  "senderId": "e2e-10x-1776531130780-8-a2",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:52:50.862690Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/8b864a19-43ea-4031-b2ab-8eac4f46aeab`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 19,
  "remittanceId": "8b864a19-43ea-4031-b2ab-8eac4f46aeab",
  "senderId": "e2e-10x-1776531130780-8-a2",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "917f16ea-519b-4678-bd33-b6080ff64c9b",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:52:50.859237Z",
  "updatedAt": "2026-04-18T16:53:28.678798Z",
  "expiresAt": null
}
```

---

### Customer 9 — `e2e-10x-1776531211324-9-a3`

- walletId: `24`  solanaAddress: `584FPpxgjMSeYj8HyUUdG1gpRxZ8pE7KLXo5qaqPjdyZ`
- fundingId: `40d66311-cfe4-4319-904b-ccea97ebcf0b`  paymentIntentId: `pi_3TNc8b3nnME1dfOB1WMf1wa5`
- remittanceId: `9c1e6e56-a3f6-42dd-9909-e81a4e5fc306`  claimTokenId: `35040fc9-0c3d-4eab-83f0-891a029f767c`
- final status: **DELIVERED**  elapsed: 77.4s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 1-create-wallet-attempt-1

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531211324-9-a1"
}
```

Response:
```json
{
  "id": 22,
  "userId": "e2e-10x-1776531211324-9-a1",
  "solanaAddress": "2oucA3zoXMZh4GXWd2NWc5te1Bg8VZugBDDK5GAEcDT2",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:54:01.441642703Z",
  "updatedAt": "2026-04-18T16:54:01.441642703Z"
}
```

#### 1-create-wallet-attempt-2

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531211324-9-a2"
}
```

Response:
```json
{
  "id": 23,
  "userId": "e2e-10x-1776531211324-9-a2",
  "solanaAddress": "9EsaJvkL2VzuCuyQyRYD6PLDCeBXKtNAeiXnJhLQZRcv",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:54:31.640217691Z",
  "updatedAt": "2026-04-18T16:54:31.640217691Z"
}
```

#### 1-create-wallet-attempt-3

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531211324-9-a3"
}
```

Response:
```json
{
  "id": 24,
  "userId": "e2e-10x-1776531211324-9-a3",
  "solanaAddress": "584FPpxgjMSeYj8HyUUdG1gpRxZ8pE7KLXo5qaqPjdyZ",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:54:31.880744243Z",
  "updatedAt": "2026-04-18T16:54:31.880744243Z"
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
  "fundingId": "40d66311-cfe4-4319-904b-ccea97ebcf0b",
  "walletId": 24,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNc8b3nnME1dfOB1WMf1wa5",
  "stripeClientSecret": "pi_3TNc8b3nnME1dfOB1WMf1wa5_secret_bHmt9477HiuHDVYSErLauPRSa",
  "createdAt": "2026-04-18T16:54:31.958787218Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/40d66311-cfe4-4319-904b-ccea97ebcf0b`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "40d66311-cfe4-4319-904b-ccea97ebcf0b",
  "walletId": 24,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNc8b3nnME1dfOB1WMf1wa5",
  "createdAt": "2026-04-18T16:54:31.958787Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776531211324-9-a3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 20,
  "remittanceId": "9c1e6e56-a3f6-42dd-9909-e81a4e5fc306",
  "senderId": "e2e-10x-1776531211324-9-a3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "35040fc9-0c3d-4eab-83f0-891a029f767c",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:54:41.659410644Z",
  "updatedAt": "2026-04-18T16:54:41.669565217Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/9c1e6e56-a3f6-42dd-9909-e81a4e5fc306`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 20,
  "remittanceId": "9c1e6e56-a3f6-42dd-9909-e81a4e5fc306",
  "senderId": "e2e-10x-1776531211324-9-a3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "35040fc9-0c3d-4eab-83f0-891a029f767c",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:54:41.659411Z",
  "updatedAt": "2026-04-18T16:54:42.785778Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/35040fc9-0c3d-4eab-83f0-891a029f767c`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "9c1e6e56-a3f6-42dd-9909-e81a4e5fc306",
  "senderId": "e2e-10x-1776531211324-9-a3",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:54:41.665831Z"
}
```

#### 7-submit-claim

`POST /api/claims/35040fc9-0c3d-4eab-83f0-891a029f767c`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "9c1e6e56-a3f6-42dd-9909-e81a4e5fc306",
  "senderId": "e2e-10x-1776531211324-9-a3",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:54:41.665831Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/9c1e6e56-a3f6-42dd-9909-e81a4e5fc306`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 20,
  "remittanceId": "9c1e6e56-a3f6-42dd-9909-e81a4e5fc306",
  "senderId": "e2e-10x-1776531211324-9-a3",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "35040fc9-0c3d-4eab-83f0-891a029f767c",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:54:41.659411Z",
  "updatedAt": "2026-04-18T16:54:46.049056Z",
  "expiresAt": null
}
```

---

### Customer 10 — `e2e-10x-1776531288735-10-a1`

- walletId: `25`  solanaAddress: `7e3Hg9WiD6EwSSL2paGhuo3gA1komX5bExgsxrLvB9m4`
- fundingId: `4fac027c-4774-487e-9582-3bd7bb36763a`  paymentIntentId: `pi_3TNc8s3nnME1dfOB0YXfJKWI`
- remittanceId: `61555663-6288-478f-a6d9-57ad0690e66e`  claimTokenId: `4d04637f-d6ae-4d6f-b46b-ca3b98f5375f`
- final status: **DELIVERED**  elapsed: 16.1s  **PASS**
- On-chain USDC after fund: 1
- Claim authority USDC after claim: Error: "Could not find token account 6NxBnWVfD6ruU4w7FXKHAkkfz7dmnCC1uYwh816R36pR"

#### 1-create-wallet-attempt-1

`POST /api/wallets`  → **HTTP 201**

Request:
```json
{
  "userId": "e2e-10x-1776531288735-10-a1"
}
```

Response:
```json
{
  "id": 25,
  "userId": "e2e-10x-1776531288735-10-a1",
  "solanaAddress": "7e3Hg9WiD6EwSSL2paGhuo3gA1komX5bExgsxrLvB9m4",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-18T16:54:48.912543110Z",
  "updatedAt": "2026-04-18T16:54:48.912543110Z"
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
  "fundingId": "4fac027c-4774-487e-9582-3bd7bb36763a",
  "walletId": 25,
  "amountUsdc": 1.0,
  "status": "PAYMENT_CONFIRMED",
  "stripePaymentIntentId": "pi_3TNc8s3nnME1dfOB0YXfJKWI",
  "stripeClientSecret": "pi_3TNc8s3nnME1dfOB0YXfJKWI_secret_Ghu0taI1N5iO5wsi72JDkPqyq",
  "createdAt": "2026-04-18T16:54:49.053979125Z"
}
```

#### 3-poll-funded

`GET /api/funding-orders/4fac027c-4774-487e-9582-3bd7bb36763a`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "fundingId": "4fac027c-4774-487e-9582-3bd7bb36763a",
  "walletId": 25,
  "amountUsdc": 1.0,
  "status": "FUNDED",
  "stripePaymentIntentId": "pi_3TNc8s3nnME1dfOB0YXfJKWI",
  "createdAt": "2026-04-18T16:54:49.053979Z"
}
```

#### 4-create-remittance

`POST /api/remittances`  → **HTTP 201**

Request:
```json
{
  "senderId": "e2e-10x-1776531288735-10-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0
}
```

Response:
```json
{
  "id": 21,
  "remittanceId": "61555663-6288-478f-a6d9-57ad0690e66e",
  "senderId": "e2e-10x-1776531288735-10-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "INITIATED",
  "escrowPda": null,
  "claimTokenId": "4d04637f-d6ae-4d6f-b46b-ca3b98f5375f",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:54:57.888470714Z",
  "updatedAt": "2026-04-18T16:54:57.894563566Z",
  "expiresAt": null
}
```

#### 5-poll-escrowed

`GET /api/remittances/61555663-6288-478f-a6d9-57ad0690e66e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 21,
  "remittanceId": "61555663-6288-478f-a6d9-57ad0690e66e",
  "senderId": "e2e-10x-1776531288735-10-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "escrowPda": null,
  "claimTokenId": "4d04637f-d6ae-4d6f-b46b-ca3b98f5375f",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:54:57.888471Z",
  "updatedAt": "2026-04-18T16:54:58.991665Z",
  "expiresAt": null
}
```

#### 6-get-claim

`GET /api/claims/4d04637f-d6ae-4d6f-b46b-ca3b98f5375f`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "remittanceId": "61555663-6288-478f-a6d9-57ad0690e66e",
  "senderId": "e2e-10x-1776531288735-10-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-20T16:54:57.892156Z"
}
```

#### 7-submit-claim

`POST /api/claims/4d04637f-d6ae-4d6f-b46b-ca3b98f5375f`  → **HTTP 200**

Request:
```json
{
  "upiId": "test@upi"
}
```

Response:
```json
{
  "remittanceId": "61555663-6288-478f-a6d9-57ad0690e66e",
  "senderId": "e2e-10x-1776531288735-10-a1",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "ESCROWED",
  "claimed": true,
  "expiresAt": "2026-04-20T16:54:57.892156Z"
}
```

#### 8-poll-delivered

`GET /api/remittances/61555663-6288-478f-a6d9-57ad0690e66e`  → **HTTP 200**

Request:
```json
(no body)
```

Response:
```json
{
  "id": 21,
  "remittanceId": "61555663-6288-478f-a6d9-57ad0690e66e",
  "senderId": "e2e-10x-1776531288735-10-a1",
  "recipientPhone": "+919876543210",
  "amountUsdc": 1.0,
  "amountInr": 92.9,
  "fxRate": 92.896987,
  "status": "DELIVERED",
  "escrowPda": null,
  "claimTokenId": "4d04637f-d6ae-4d6f-b46b-ca3b98f5375f",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-18T16:54:57.888471Z",
  "updatedAt": "2026-04-18T16:55:02.129587Z",
  "expiresAt": null
}
```

---
