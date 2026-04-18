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
| Claim Authority ATA | `2KKehH5ervdM9odMBeZ6pi6PkBc8LpWc4WGWkaDngTAv` |
| Solana RPC | `https://api.devnet.solana.com` |
| Backend Profile | `sandbox` |
| Docker Compose | PostgreSQL, Redis, Temporal, 2x MPC sidecars, backend |

---

## Test Run 1: Claim Bug Discovery

### Step 1: Create Wallet (MPC 2-of-2 DKG)

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

### Step 2: Fund MPC Wallet (on-chain)

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

### Step 3: Fund Wallet (API — updates DB balance)

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

### Step 4: Get FX Rate

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

### Step 5: Create Remittance ($5 USDC)

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

### Step 6: Escrow Deposit (automatic via Temporal — SUCCESS)

**Backend logs:**
```
Depositing escrow for remittance 828ba0eb amount 5.00 USDC
Starting MPC signing ceremony: 221b65aa-7a58-4aac-a750-2705b1426c03 (parties: 2)
MPC signing completed for ceremony 221b65aa
Deposit transaction for remittance 828ba0eb signed via MPC (460 bytes)
Escrow deposit submitted with signature 1gZ4uStXXkTv4fno2jJUHtrBfKSKcRwogbHeqxcjBPYfgcvAQ1zrsCtaNufLpqckcCYChcsHFMuxnhZb28Ca1aV
Remittance 828ba0eb status updated: INITIATED → ESCROWED
```

**On-chain verification:**
```
$ solana confirm 1gZ4uStXXkTv4fno2jJUHtrBfKSKcRwogbHeqxcjBPYfgcvAQ1zrsCtaNufLpqckcCYChcsHFMuxnhZb28Ca1aV --url devnet
Finalized
```

**Poll remittance status:**

```
GET http://localhost:8080/api/remittances/828ba0eb-4efb-4d3e-b523-f2d0f22a6c6a
```

```json
{
    "status": "ESCROWED"
}
```

### Step 7: Get Claim Details

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

### Step 8: Submit Claim (CLAIM TX FAILED ON-CHAIN)

**Request:**
```
POST http://localhost:8080/api/claims/a1ed3f58-4ca8-42de-a0e2-8fe44904cfb5
Content-Type: application/json

{"upiId": "test@upi"}
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

**Claim transaction — FAILED on-chain:**
```
$ solana confirm 27N4KAjf1nt9hkcvQR2aJQVfFByuiEM8TiBGLnCjW7BW8UFhkxCExQGbZmvqnV33WyRcuMEaCZ3YTqm7YmenqZRj --url devnet -v

Transaction executed in slot 455941954:
  Instruction 0
    Account 0: 3LZh792t... (claim_authority — signer)
    Account 1: Ek1sQejo... (escrow PDA)
    Account 2: 76Uwgqc...  (vault PDA)
    Account 3: 3LZh792t... (WRONG — claim_authority instead of recipient_token!)
    Account 4: DQoGcVse... (sender wallet)
    Account 5: TokenkegQ.. (token_program)
  Status: Error processing Instruction 0: custom program error: 0xbbf
  Log Messages:
    Program log: AnchorError caused by account: recipient_token.
                 Error Code: AccountOwnedByWrongProgram. Error Number: 3007.
    Program log: Left:  11111111111111111111111111111111
    Program log: Right: TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA
```

### Root Cause Analysis

`TemporalRemittanceClaimSignaler.resolveClaimDestination()` returned the claim authority's
**raw public key** (`3LZh792t...`) instead of its **Associated Token Account** for the USDC mint.

The Anchor Claim struct expects `recipient_token: Account<'info, TokenAccount>` — owned by the
Token Program. A system account (owned by `11111111...`) was passed instead.

**What was passed vs what was expected:**

| Position | Anchor Field | Expected | Actual (bug) |
|----------|-------------|----------|--------------|
| 0 | `claim_authority` | `3LZh792t...` | `3LZh792t...` (correct) |
| 1 | `escrow` | escrow PDA | escrow PDA (correct) |
| 2 | `vault` | vault PDA | vault PDA (correct) |
| 3 | `recipient_token` | claim authority **ATA** | claim authority **pubkey** (WRONG) |
| 4 | `sender` | sender wallet | sender wallet (correct) |
| 5 | `token_program` | Token Program | Token Program (correct) |

### Fix Applied

1. `TemporalRemittanceClaimSignaler`: Derive claim authority's ATA via
   `PublicKey.findProgramDerivedAddress(claimAuthority, usdcMint)` instead of raw public key
2. `SolanaTransactionServiceAdapter.claimEscrow()`: Check if ATA exists on-chain via
   `getAccountInfo()`; if not, prepend `CreateAssociatedTokenAccountInstruction` to the transaction
3. Multi-instruction claim transaction: `[CreateATA (if needed), Claim]`

---

## Test Run 2: After Fix (ALL PASS)

Reused the same MPC wallet (`DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q`) with existing
on-chain USDC balance (95 USDC remaining after the first 5 USDC deposit).

### Step 1: Create Remittance ($3 USDC)

**Request:**
```
POST http://localhost:8080/api/remittances
Content-Type: application/json

{
    "senderId": "e2e-devnet-1776348650",
    "recipientPhone": "+919876543210",
    "amountUsdc": 3.00
}
```

**Response:** `200 OK`
```json
{
    "id": 6,
    "remittanceId": "af5438a7-400b-4c08-90e4-522f8fd57f29",
    "senderId": "e2e-devnet-1776348650",
    "recipientPhone": "+919876543210",
    "amountUsdc": 3.0,
    "amountInr": 280.32,
    "fxRate": 93.441485,
    "status": "INITIATED",
    "escrowPda": null,
    "claimTokenId": "ea7f0dbd-a4a7-41a7-b83e-51bd2254b167",
    "smsNotificationFailed": false,
    "createdAt": "2026-04-16T14:41:34.337174693Z",
    "updatedAt": "2026-04-16T14:41:34.344073230Z",
    "expiresAt": null
}
```

### Step 2: Escrow Deposit (automatic via Temporal — SUCCESS)

**Backend logs:**
```
Depositing escrow for remittance af5438a7 amount 3.00 USDC
Starting MPC signing ceremony (parties: 2)
MPC signing completed
Escrow deposit submitted with signature 4wcFXRXQmsRqVgQAMvQARzdvj9es65AYjv85eNVF1Q22spbXt1SFDk3PGMWbuvnDYE6E4WXoZDHChm4cPd7L9noK
Remittance af5438a7 status updated: INITIATED → ESCROWED
Claim SMS sent for remittanceId=af5438a7
```

**On-chain verification:**
```
$ solana confirm 4wcFXRXQmsRqVgQAMvQARzdvj9es65AYjv85eNVF1Q22spbXt1SFDk3PGMWbuvnDYE6E4WXoZDHChm4cPd7L9noK --url devnet
Finalized
```

**Poll status:**
```
GET http://localhost:8080/api/remittances/af5438a7-400b-4c08-90e4-522f8fd57f29
```

```json
{
    "status": "ESCROWED"
}
```

### Step 3: Submit Claim

**Request:**
```
POST http://localhost:8080/api/claims/ea7f0dbd-a4a7-41a7-b83e-51bd2254b167
Content-Type: application/json

{"upiId": "test@upi"}
```

**Response:** `200 OK`
```json
{
    "remittanceId": "af5438a7-400b-4c08-90e4-522f8fd57f29",
    "senderId": "e2e-devnet-1776348650",
    "amountUsdc": 3.0,
    "amountInr": 280.32,
    "fxRate": 93.441485,
    "status": "ESCROWED",
    "claimed": true,
    "expiresAt": "2026-04-18T14:41:34.341707Z"
}
```

**Backend logs (claim processing):**
```
Claim signal sent for workflowId=stablepay-remittance-af5438a7
Claim signal received for remittanceId=af5438a7, claimToken=ea7f0dbd
Processing claim for remittanceId=af5438a7
Resolved claim authority ATA: 2KKehH5ervdM9odMBeZ6pi6PkBc8LpWc4WGWkaDngTAv
Submitting escrow claim for remittance af5438a7
Escrow claim submitted with signature 61uQZbjb6YPxXC5Gcp46dGTLQ7VwwE49YSRoDgg5HDKtM4wX4KwUui14aF16458G6DJtNBcGdJSUq2Zrr9Va1WDf
Escrow released for remittance af5438a7
Remittance af5438a7 status updated: ESCROWED → CLAIMED
Simulating INR disbursement: 3.00 USDC to UPI tes**** for remittance af5438a7
INR disbursement simulated successfully
Remittance af5438a7 status updated: CLAIMED → DELIVERED
Remittance af5438a7 delivered successfully
```

### Step 4: Claim Transaction — SUCCESS ON-CHAIN

```
$ solana confirm 61uQZbjb6YPxXC5Gcp46dGTLQ7VwwE49YSRoDgg5HDKtM4wX4KwUui14aF16458G6DJtNBcGdJSUq2Zrr9Va1WDf --url devnet -v

Transaction executed in slot 455946295:
  Version: 0
  Signature 0: 61uQZbjb6YPxXC5Gcp46dGTLQ7VwwE49YSRoDgg5HDKtM4wX4KwUui14aF16458G6DJtNBcGdJSUq2Zrr9Va1WDf
  Account 0: srw- 3LZh792tEakavG2FJPJKocXUZSfBgmiLtapj5hNMTZkr (fee payer)
  Account 1: -rw- DQoGcVseQ2gquehjH4gyuZDjdThGNQcLYNcxWRBpuz6q
  Account 2: -rw- FaRgcuRbkb35Hw6XpMwEbDVPa44xLfzhHjsusYpaGnWo
  Account 3: -rw- 2KKehH5ervdM9odMBeZ6pi6PkBc8LpWc4WGWkaDngTAv
  Account 4: -rw- 4f5fxvV4vEDJV8wEXrKbXZyXBZoV2U8vVkrRMhuxTKeX
  Account 5: -r-- TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA
  Account 6: -r-x 7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij
  Instruction 0
    Program:   7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij (6)
    Account 0: 3LZh792t... (claim_authority — signer)
    Account 1: 4f5fxvV4... (escrow PDA)
    Account 2: FaRgcuRb... (vault PDA)
    Account 3: 2KKehH5e... (recipient_token — claim authority ATA — CORRECT!)
    Account 4: DQoGcVse... (sender wallet)
    Account 5: TokenkegQ.. (token_program)
    Data: [62, 198, 214, 193, 213, 159, 108, 210]
  Status: Ok
    Fee: 0.000005 SOL
    Account 1 balance: 0.991986 -> 0.995988 SOL (sender received rent back)
    Account 2 balance: 0.00203928 -> 0 SOL (escrow closed)
    Account 4 balance: 0.00196272 -> 0 SOL (vault closed)
  Compute Units Consumed: 11593
  Log Messages:
    Program 7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij invoke [1]
    Program log: Instruction: Claim
    Program TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA invoke [2]
    Program TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA consumed 76 of 191434 compute units
    Program TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA success
    Program TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA invoke [2]
    Program TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA consumed 118 of 189034 compute units
    Program TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA success
    Program 7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij consumed 11593 of 200000 compute units
    Program 7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij success

Finalized
```

### Step 5: Final Status — DELIVERED

**Request:**
```
GET http://localhost:8080/api/remittances/af5438a7-400b-4c08-90e4-522f8fd57f29
```

**Response:** `200 OK`
```json
{
    "id": 6,
    "remittanceId": "af5438a7-400b-4c08-90e4-522f8fd57f29",
    "senderId": "e2e-devnet-1776348650",
    "recipientPhone": "+919876543210",
    "amountUsdc": 3.0,
    "amountInr": 280.32,
    "fxRate": 93.441485,
    "status": "DELIVERED",
    "escrowPda": null,
    "claimTokenId": "ea7f0dbd-a4a7-41a7-b83e-51bd2254b167",
    "smsNotificationFailed": false,
    "createdAt": "2026-04-16T14:41:34.337175Z",
    "updatedAt": "2026-04-16T14:42:06.200243Z",
    "expiresAt": null
}
```

### Final On-Chain Balances

```
Sender USDC:          92  (100 - 5 deposit1 - 3 deposit2)
Claim Authority USDC:  3  (received from second escrow claim)
```

---

## Summary

### Test Run 1 (before fix)

| Step | API | On-chain | Status |
|------|-----|----------|--------|
| Create wallet (MPC DKG) | 200 OK | — | PASS |
| Fund wallet (SOL + USDC) | — | Finalized | PASS |
| Fund via API | 200 OK | — | PASS |
| FX rate | 200 OK | — | PASS |
| Create remittance ($5) | 200 OK | — | PASS |
| Escrow deposit (MPC-signed) | — | **Finalized** | PASS |
| Get claim details | 200 OK | — | PASS |
| Submit claim | 200 OK | **Error 0xbbf** | **FAIL** |
| Final status | DELIVERED | — | PASS (workflow) |

### Test Run 2 (after fix)

| Step | API | On-chain | Status |
|------|-----|----------|--------|
| Create remittance ($3) | 200 OK | — | PASS |
| Escrow deposit (MPC-signed) | — | **Finalized** | PASS |
| Submit claim | 200 OK | **Finalized (Ok)** | **PASS** |
| Final status | DELIVERED | — | PASS |

### Devnet Transactions

| Transaction | Signature | Status |
|------------|-----------|--------|
| Deposit #1 (5 USDC) | `1gZ4uStXXkTv4fno2jJUHtrBfKSKcRwogbHeqxcjBPYfgcvAQ1zrsCtaNufLpqckcCYChcsHFMuxnhZb28Ca1aV` | Finalized |
| Claim #1 (bug) | `27N4KAjf1nt9hkcvQR2aJQVfFByuiEM8TiBGLnCjW7BW8UFhkxCExQGbZmvqnV33WyRcuMEaCZ3YTqm7YmenqZRj` | Failed (0xbbf) |
| Deposit #2 (3 USDC) | `4wcFXRXQmsRqVgQAMvQARzdvj9es65AYjv85eNVF1Q22spbXt1SFDk3PGMWbuvnDYE6E4WXoZDHChm4cPd7L9noK` | Finalized |
| Claim #2 (fixed) | `61uQZbjb6YPxXC5Gcp46dGTLQ7VwwE49YSRoDgg5HDKtM4wX4KwUui14aF16458G6DJtNBcGdJSUq2Zrr9Va1WDf` | **Finalized (Ok)** |

---

## Stripe On-Ramp E2E (STA-82)

Validates the full Stripe on-ramp funding flow on devnet:
Stripe test payment → webhook → Temporal `WalletFundingWorkflow` →
real on-chain USDC + SOL transfers → DB balance credit.

### Prerequisites

- Stripe test-mode keys in `.env`: `STRIPE_API_KEY=sk_test_…`, `STRIPE_WEBHOOK_SECRET=whsec_…`
- Treasury devnet wallet funded with USDC + SOL, `TREASURY_PRIVATE_KEY` in `.env`
- Stripe CLI installed, authenticated against the test account
- Docker stack running: `make up`
- Webhook forwarder: `make stripe-listen` (keep this terminal open)

### Happy Path — Fund → Workflow → On-chain credit

1. Create MPC wallet: `POST /api/wallets` → capture `{walletId, solanaAddress}`
2. Initiate funding: `POST /api/wallets/{walletId}/fund` body `{"amount": 25.00}`
   → expect `201` + `FundingOrderResponse` with `status=PAYMENT_CONFIRMED`,
   `stripePaymentIntentId`, `stripeClientSecret` — capture `fundingId`
3. Confirm the PI via Stripe CLI in test mode (auto-confirm is already wired in
   `StripePaymentAdapter` when `stripe.auto-confirm=true`) — `stripe listen`
   forwards `payment_intent.succeeded` back to `/webhooks/stripe`
4. Poll `GET /api/funding-orders/{fundingId}` — expect status transition
   `PAYMENT_CONFIRMED → FUNDED` within ~15s (workflow executes 5 activities)
5. Verify sender ATA on devnet holds 25.000000 USDC (Solana Explorer)
6. Verify sender has enough SOL (auto top-up ≥ 0.01 SOL if below threshold)
7. Verify DB: wallet `available_balance` = 25, `funding_orders.status = FUNDED`

Expected workflow signature: `checkTreasuryBalance → ensureSolBalance →
createAtaIfNeeded → transferUsdc → finalizeFunding`.

### Refund Path (Stripe-only — SPL stays in sender ATA)

1. Create a fresh wallet, fund it `10.00` and wait for FUNDED
2. `POST /api/funding-orders/{fundingId}/refund` → expect `200` + status `REFUNDED`
3. Verify Stripe dashboard: refund visible, original PI shows refunded amount
4. Verify DB: wallet balance decremented by 10.00, order status `REFUNDED`
5. **Invariant:** sender ATA still holds 10 USDC on-chain — this is by design
   (rev-5 spec appendix #20). The refund returns fiat via Stripe; on-chain SPL
   is not clawed back.

### Refund Rejection Path (SP-0025 balance gate)

1. Create a third wallet, fund it `10.00`, wait for FUNDED
2. Create a remittance that consumes the USDC
   (`POST /api/remittances` body `{amountUsdc: 10.00, ...}`) — this moves USDC
   to escrow and decrements wallet balance
3. `POST /api/funding-orders/{fundingId}/refund` → expect `409 SP-0025`
   (`InsufficientBalanceForRefundException`)
4. Verify DB: order status unchanged (still `FUNDED`), wallet balance unchanged,
   no Stripe refund attempted

### Completion Checklist

| Scenario | API | Temporal | On-chain | Stripe | Status |
|----------|-----|----------|----------|--------|--------|
| Happy path fund | 201/200 | 5 activities | 10 USDC + 0.01 SOL credited | PI succeeded | **PASS** |
| Refund path | 200 REFUNDED | n/a | SPL retained in ATA (10 USDC) | Refund created | **PASS** |
| Refund reject (SP-0025) | 409 SP-0025 | n/a | No-op | No refund call | **PASS** |

### Real run — 2026-04-18

**Environment**
- Treasury wallet: `58gFSCTWosoJuVuzG8Vt5ZnfvJjeyx5PPZHhXMXr3ZSv` (deployer, 20 USDC + 1.5 SOL from Circle faucet)
- USDC mint: `4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU` (devnet Circle USDC)
- Stripe test key: `sk_test_51TCRge…lsN00mxYvHSvf`
- Webhook secret: from `stripe listen` (whsec_2103ca6a…)

**Scenario 1 — Happy path** (fundingId `79ee7b33-a9cd-41c4-9801-2ddec1ca76c0`, wallet 7 `HkgPMoLx8WvEiqfSkVU6xWBZQtYNmnVwtyRYPxeCSeGb`)

| Step | Result |
|------|--------|
| `POST /api/wallets/7/fund {amount: 10.00}` | `201` `status=PAYMENT_CONFIRMED` `pi_3TNb4Y3nnME1dfOB0cfWtKqh` |
| `payment_intent.succeeded` webhook | `200` `evt_3TNb4Y…0n5Zt4f4` |
| Temporal `WalletFundingWorkflow` | 5 activities completed |
| SOL top-up tx | `2BMDMyWDAxDW7dwXJusv5B5E1cZUyN7kyTfdi7EiJu9hLmy8Z2xjnSmBJ9koA1xTesYVEiZqtWent72REEbLQqVi` |
| ATA create tx | `2Svfo1rstxQFCTdfZMGFYwXrXMR4yPyfRoB64jkNH8ZToJj5PbyvrdNZm3DafnUUKMHGyoht697f8g4D5i35bduf` |
| USDC transfer tx | `2ysGHgqiMJUkzQCMjDnvMHzE7ZBhGgzNsm3xyV7RQLNeA6qrtyUVk3BAgeVJooEDgNj6mfkF7BHj38i8ovwYYp8K` |
| On-chain after | ATA holds 10.000000 USDC, SOL=0.01 |
| DB after | wallet 7 available=10.000000 total=10.000000, order status `FUNDED` |

**Scenario 2 — Refund path** (same fundingId)

| Step | Result |
|------|--------|
| `POST /api/funding-orders/79ee…/refund` | `200` `status=REFUNDED` |
| Stripe webhooks | `refund.created`, `charge.refunded`, `refund.updated` all `200` |
| On-chain USDC (invariant) | still 10.000000 (no claw-back — rev-5 §20) |
| DB after | wallet 7 available=0 total=0, order status `REFUNDED` |

**Scenario 3 — Refund reject** (fundingId `b73c695f-56ce-4484-8b66-9e5e8b615b25`, wallet 8 `F3samNB4bcqbBkUxUNhZsUgPvvPSGtDkCHJgXCtH32aF`)

| Step | Result |
|------|--------|
| Fund 10 USDC, wait `FUNDED` | `pi_3TNb7n3nnME1dfOB1D47k8Rd`, on-chain 10 USDC |
| `POST /api/remittances {amountUsdc: 10}` | `201` remittance `852302df-ce7c-4794-895b-839213a4be9b`, eventually `ESCROWED` |
| Wallet after remittance | `available_balance=0`, `total_balance=10` |
| `POST /api/funding-orders/b73c…/refund` | **`409`** `SP-0025` `"Insufficient balance for refund. Requested: 10.000000, Available: 0.000000"` |
| Stripe refund call | **none made** (no `refund.created` event) |
| DB after | order status unchanged `FUNDED`, wallet unchanged |

### Fixes applied during E2E to make it pass

1. **`docker-compose.yml`** — `SPRING_PROFILES_ACTIVE: docker` → `sandbox` (the `docker` profile never existed; backend fell back to defaults and tried `localhost:5432`).
2. **`docker-compose.yml`** — added `STRIPE_TEST_MODE`, `STRIPE_AUTO_CONFIRM`, `STRIPE_TEST_PAYMENT_METHOD` env var wiring (STA-81 only added the 3 secrets, missed the toggles).
3. **`StripePaymentAdapter.buildPaymentIntentParams`** — add `.addPaymentMethodType("card")` when auto-confirming. Without it, the Stripe account's dashboard-enabled redirect-based methods force a `return_url` requirement and auto-confirm fails.
4. **`WalletJpaRepository` / `WalletRepository` / `WalletRepositoryAdapter`** — split `findById` (unlocked) from `findForUpdateById` (`PESSIMISTIC_WRITE`). `CompleteFundingHandler` runs in a `readOnly=true` transaction and only reads `solanaAddress`; the previous global `@Lock` on `findById` caused `ERROR: cannot execute SELECT FOR NO KEY UPDATE in a read-only transaction` and broke every webhook dispatch. `FinalizeFundingHandler` and `RefundFundingHandler` switched to `findForUpdateById` to keep their lock.
