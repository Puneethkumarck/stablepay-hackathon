# E2E Devnet Test Log — 2026-04-16

Full end-to-end test against Solana devnet with real MPC signing and on-chain escrow.

## Environment

| Component | Value |
|-----------|-------|
| Escrow Program | `7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij` |
| Test USDC Mint | `CAUBK3crVXviMdpRboXwiM4Havto3MqPtWCN2eimyGyq` |
| Deployer Wallet | `58gFSCTWosoJuVuzG8Vt5ZnfvJjeyx5PPZHhXMXr3ZSv` |
| Claim Authority | `3LZh792tEakavG2FJPJKocXUZSfBgmiLtapj5hNMTZkr` |
| MPC Wallet (sender) | `DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q` |
| Recipient Token Account | `2KKehH5ervdM9odMBeZ6pi6PkBc8LpWc4WGWkaDngTAv` |
| Solana RPC | `https://api.devnet.solana.com` |
| Backend Profile | `sandbox` |
| Docker Compose | PostgreSQL, Redis, Temporal, 2x MPC sidecars, backend |

---

## Step 1: Create Wallet (MPC 2-of-2 DKG)

**Request:**
```
POST http://localhost:8080/api/wallets
Content-Type: application/json

{"userId": "e2e-devnet-1776348650"}
```

**Response:** `200 OK`
```json
{
    "id": 4,
    "userId": "e2e-devnet-1776348650",
    "solanaAddress": "DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q",
    "availableBalance": 0,
    "totalBalance": 0,
    "createdAt": "2026-04-16T14:10:51.348965183Z",
    "updatedAt": "2026-04-16T14:10:51.348965183Z"
}
```

**Backend logs:**
```
MPC config: partyId=0, threshold=2, totalParties=2, peers={1=mpc-sidecar-1:7000}, peerSidecars=1
Starting MPC key generation ceremony (parties: 2)
Triggering peer sidecar (party 1) for ceremony
Peer keygen response: status=STATUS_COMPLETED, keyShareLen=...
MPC key generation completed: address=DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q
```

---

## Step 2: Fund MPC Wallet (on-chain)

SOL airdrop was rate-limited. Transferred SOL from deployer wallet instead.

```bash
solana transfer --url devnet DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q 1 --allow-unfunded-recipient
# Signature: 3yzRvWhXds3G2QTh2Ebz5zcA2761ZZxvyZq351RbsS7QmfR1we4UU5uhWjG9W9vmvHypfawLzGY9PhM9HMK5QnjU
```

Created test USDC mint and minted 100 tokens:

```bash
spl-token create-token --decimals 6 --url devnet
# Token: CAUBK3crVXviMdpRboXwiM4Havto3MqPtWCN2eimyGyq

spl-token create-account CAUBK3crVXviMdpRboXwiM4Havto3MqPtWCN2eimyGyq \
  --owner DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q --url devnet
# Account: GuDuFKeXWbcyE6FwdJihrmxzGvYeRNFXx3Sa6N43XazV

spl-token mint CAUBK3crVXviMdpRboXwiM4Havto3MqPtWCN2eimyGyq 100 \
  --recipient-owner DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q --url devnet
# Minted 100 tokens
```

**Final on-chain state:**
- SOL balance: 1 SOL
- USDC balance: 100 tokens

---

## Step 3: Fund Wallet (API — updates DB balance)

**Request:**
```
POST http://localhost:8080/api/wallets/4/fund
Content-Type: application/json

{"amount": 25.00}
```

**Response:** `200 OK`
```json
{
    "id": 4,
    "userId": "e2e-devnet-1776348650",
    "solanaAddress": "DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q",
    "availableBalance": 25.0,
    "totalBalance": 25.0,
    "createdAt": "2026-04-16T14:10:51.348965Z",
    "updatedAt": "2026-04-16T14:13:00.196557573Z"
}
```

---

## Step 4: Get FX Rate

**Request:**
```
GET http://localhost:8080/api/fx/USD-INR
```

**Response:** `200 OK`
```json
{
    "rate": 93.441485,
    "source": "open.er-api.com",
    "timestamp": "2026-04-16T14:12:39.117223708Z",
    "expiresAt": "2026-04-16T14:13:39.117223708Z"
}
```

---

## Step 5: Create Remittance

**Request:**
```
POST http://localhost:8080/api/remittances
Content-Type: application/json

{
    "senderId": "e2e-devnet-1776348650",
    "recipientPhone": "+919876543210",
    "amountUsdc": 5.00
}
```

**Response:** `200 OK`
```json
{
    "id": 4,
    "remittanceId": "828ba0eb-4efb-4d3e-b523-f2d0f22a6c6a",
    "senderId": "e2e-devnet-1776348650",
    "recipientPhone": "+919876543210",
    "amountUsdc": 5.0,
    "amountInr": 467.21,
    "fxRate": 93.441485,
    "status": "INITIATED",
    "escrowPda": null,
    "claimTokenId": "a1ed3f58-4ca8-42de-a0e2-8fe44904cfb5",
    "smsNotificationFailed": false,
    "createdAt": "2026-04-16T14:13:09.313376400Z",
    "updatedAt": "2026-04-16T14:13:09.342901779Z",
    "expiresAt": null
}
```

---

## Step 6: Escrow Deposit (automatic — Temporal workflow)

The Temporal workflow triggers automatically after remittance creation.

**Backend logs:**
```
Depositing escrow for remittance 828ba0eb amount 5.00 USDC
Submitting escrow deposit for remittance 828ba0eb amount 5.00 USDC
Starting MPC signing ceremony: 221b65aa-7a58-4aac-a750-2705b1426c03 (parties: 2)
MPC signing completed for ceremony 221b65aa
Deposit transaction for remittance 828ba0eb signed via MPC (460 bytes)
Escrow deposit submitted with signature 1gZ4uStXXkTv4fno2jJUHtrBfKSKcRwogbHeqxcjBPYfgcvAQ1zrsCtaNufLpqckcCYChcsHFMuxnhZb28Ca1aV
Remittance 828ba0eb status updated: INITIATED → ESCROWED
```

**On-chain verification:**
```
solana confirm 1gZ4uStXXkTv4fno2jJUHtrBfKSKcRwogbHeqxcjBPYfgcvAQ1zrsCtaNufLpqckcCYChcsHFMuxnhZb28Ca1aV --url devnet
# Result: Finalized
```

**Poll remittance status:**

**Request:**
```
GET http://localhost:8080/api/remittances/828ba0eb-4efb-4d3e-b523-f2d0f22a6c6a
```

**Response:** `200 OK`
```json
{
    "id": 4,
    "remittanceId": "828ba0eb-4efb-4d3e-b523-f2d0f22a6c6a",
    "senderId": "e2e-devnet-1776348650",
    "recipientPhone": "+919876543210",
    "amountUsdc": 5.0,
    "amountInr": 467.21,
    "fxRate": 93.441485,
    "status": "ESCROWED",
    "escrowPda": null,
    "claimTokenId": "a1ed3f58-4ca8-42de-a0e2-8fe44904cfb5",
    "smsNotificationFailed": false,
    "createdAt": "2026-04-16T14:13:09.313376Z",
    "updatedAt": "2026-04-16T14:13:09.342902Z",
    "expiresAt": null
}
```

---

## Step 7: Get Claim Details

**Request:**
```
GET http://localhost:8080/api/claims/a1ed3f58-4ca8-42de-a0e2-8fe44904cfb5
```

**Response:** `200 OK`
```json
{
    "remittanceId": "828ba0eb-4efb-4d3e-b523-f2d0f22a6c6a",
    "senderId": "e2e-devnet-1776348650",
    "amountUsdc": 5.0,
    "amountInr": 467.21,
    "fxRate": 93.441485,
    "status": "ESCROWED",
    "claimed": false,
    "expiresAt": "2026-04-18T14:13:09.338566Z"
}
```

---

## Step 8: Submit Claim

Created recipient token account first:
```bash
spl-token create-account CAUBK3crVXviMdpRboXwiM4Havto3MqPtWCN2eimyGyq \
  --owner 3LZh792tEakavG2FJPJKocXUZSfBgmiLtapj5hNMTZkr --url devnet
# Account: 2KKehH5ervdM9odMBeZ6pi6PkBc8LpWc4WGWkaDngTAv
```

**Request:**
```
POST http://localhost:8080/api/claims/a1ed3f58-4ca8-42de-a0e2-8fe44904cfb5
Content-Type: application/json

{
    "upiId": "test@upi",
    "destinationAddress": "2KKehH5ervdM9odMBeZ6pi6PkBc8LpWc4WGWkaDngTAv"
}
```

**Response:** `200 OK`
```json
{
    "remittanceId": "828ba0eb-4efb-4d3e-b523-f2d0f22a6c6a",
    "senderId": "e2e-devnet-1776348650",
    "amountUsdc": 5.0,
    "amountInr": 467.21,
    "fxRate": 93.441485,
    "status": "ESCROWED",
    "claimed": true,
    "expiresAt": "2026-04-18T14:13:09.338566Z"
}
```

**Backend logs (claim processing):**
```
Claim signal sent for workflowId=stablepay-remittance-828ba0eb
Claim signal received for remittanceId=828ba0eb, claimToken=a1ed3f58
Processing claim for remittanceId=828ba0eb
Submitting escrow claim for remittance 828ba0eb
Escrow claim submitted with signature 27N4KAjf1nt9hkcvQR2aJQVfFByuiEM8TiBGLnCjW7BW8UFhkxCExQGbZmvqnV33WyRcuMEaCZ3YTqm7YmenqZRj
Remittance 828ba0eb status updated: ESCROWED → CLAIMED
Simulating INR disbursement: 5.00 USDC to UPI tes**** for remittance 828ba0eb
INR disbursement simulated successfully
Remittance 828ba0eb status updated: CLAIMED → DELIVERED
Remittance 828ba0eb delivered successfully
```

---

## Step 9: Final Status — DELIVERED

**Request:**
```
GET http://localhost:8080/api/remittances/828ba0eb-4efb-4d3e-b523-f2d0f22a6c6a
```

**Response:** `200 OK`
```json
{
    "id": 4,
    "remittanceId": "828ba0eb-4efb-4d3e-b523-f2d0f22a6c6a",
    "senderId": "e2e-devnet-1776348650",
    "recipientPhone": "+919876543210",
    "amountUsdc": 5.0,
    "amountInr": 467.21,
    "fxRate": 93.441485,
    "status": "DELIVERED",
    "escrowPda": null,
    "claimTokenId": "a1ed3f58-4ca8-42de-a0e2-8fe44904cfb5",
    "smsNotificationFailed": false,
    "createdAt": "2026-04-16T14:13:09.313376Z",
    "updatedAt": "2026-04-16T14:14:43.557637Z",
    "expiresAt": null
}
```

---

## On-Chain Transaction Verification

### Deposit Transaction (SUCCESS — Finalized)

```
Signature: 1gZ4uStXXkTv4fno2jJUHtrBfKSKcRwogbHeqxcjBPYfgcvAQ1zrsCtaNufLpqckcCYChcsHFMuxnhZb28Ca1aV
Status: Finalized
Sender token balance after deposit: 95 USDC (100 - 5)
```

### Claim Transaction (FAILED on-chain)

```
Signature: 27N4KAjf1nt9hkcvQR2aJQVfFByuiEM8TiBGLnCjW7BW8UFhkxCExQGbZmvqnV33WyRcuMEaCZ3YTqm7YmenqZRj
Status: Error processing Instruction 0: custom program error: 0xbbf
```

**Detailed error from `solana confirm -v`:**
```
Account 0: srw- 3LZh792tEakavG2FJPJKocXUZSfBgmiLtapj5hNMTZkr (fee payer / claim_authority)
Account 1: -rw- DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q (sender wallet)
Account 2: -rw- Ek1sQejoGfpaYzEkN6QdJtqR61UrUHuXzNtnmWCpNqx9 (escrow PDA)
Account 3: -rw- 76UwgqcQB4PzhHJYe1ZHe9MQw7AkFmfWPPJhrh6v7fjz (vault PDA)
Account 4: -r-- TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA (token program)
Account 5: -r-x 7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij (escrow program)

Instruction accounts passed:
  Account 0: 3LZh792t... (claim_authority — signer)
  Account 1: Ek1sQejo... (escrow PDA)
  Account 2: 76Uwgqc...  (vault PDA)
  Account 3: 3LZh792t... (WRONG — passed claim_authority as recipient_token!)
  Account 4: DQoGcVse... (sender wallet)
  Account 5: TokenkegQ.. (token_program)

Log Messages:
  Program 7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij invoke [1]
  Program log: Instruction: Claim
  Program log: AnchorError caused by account: recipient_token.
               Error Code: AccountOwnedByWrongProgram. Error Number: 3007.
               Error Message: The given account is owned by a different program than expected.
  Program log: Left:  11111111111111111111111111111111
  Program log: Right: TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA
```

**Root cause:** The `recipient_token` account (instruction Account 3) received the claim authority
address (`3LZh792t...`) instead of the actual recipient token account (`2KKehH5e...`). The claim
authority is a system account (owned by `11111111...`), but Anchor expects a Token Program account
(owned by `TokenkegQ...`). The claim instruction builder is passing the wrong account in position 3.

**Anchor Claim struct account order:**
1. `claim_authority` — Signer
2. `escrow` — Account (mut, close=sender)
3. `vault` — Account (mut, token)
4. `recipient_token` — Account (mut, token) ← **should be 2KKehH5e...**
5. `sender` — SystemAccount (mut)
6. `token_program` — Program

**What the Java code passed:**
1. `claim_authority` (3LZh792t...) ✓
2. `escrow` (Ek1sQejo...) ✓
3. `vault` (76Uwgqc...) ✓
4. `claim_authority` (3LZh792t...) ✗ — WRONG, should be recipient token account
5. `sender` (DQoGcVse...) ✓
6. `token_program` ✓

The `destinationTokenAccount` passed to `buildClaimInstruction` was the claim authority address
rather than the actual recipient SPL token account. The issue is in how the claim submission
flow resolves the destination address — it's passing the wallet address instead of the
associated token account.

---

## Summary

| Step | Status | On-chain |
|------|--------|----------|
| 1. Create wallet (MPC DKG) | PASS | — |
| 2. Fund wallet (SOL + USDC) | PASS | Finalized |
| 3. Fund via API | PASS | — |
| 4. FX rate | PASS | — |
| 5. Create remittance | PASS | — |
| 6. Escrow deposit (MPC-signed) | PASS | **Finalized** |
| 7. Get claim details | PASS | — |
| 8. Submit claim | PASS (API) / **FAIL (on-chain)** | Error 0xbbf |
| 9. Final status | DELIVERED (workflow) | — |

**What works end-to-end on devnet:**
- MPC 2-of-2 distributed key generation
- MPC 2-of-2 threshold signing
- Escrow deposit transaction (real on-chain, finalized)
- Temporal workflow orchestration (full lifecycle)
- FX rate from live API
- Claim signal via Temporal

**What needs fixing:**
- Claim instruction passes wrong account for `recipient_token` — needs the SPL token account,
  not the wallet address
