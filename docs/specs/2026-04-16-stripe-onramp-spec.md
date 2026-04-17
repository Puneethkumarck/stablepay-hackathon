---
title: Stripe On-Ramp Integration for Wallet Funding
status: approved
created: 2026-04-16
updated: 2026-04-16
issue: STA-72
revision: 5 (fourth review pass — Option C refund, webhook lookup by funding_id, merged finalize activity, tighter amount validation)
---

# Stripe On-Ramp Integration — Spec

## 1. Objective

Replace the treasury stub with a real fiat-to-USDC funding flow:

1. Sender calls `POST /api/wallets/{id}/fund` with a USD amount
2. Backend creates a Stripe PaymentIntent (auto-confirmed in test mode via configurable props)
3. Stripe webhook `payment_intent.succeeded` triggers a Temporal `WalletFundingWorkflow`
4. Workflow ensures the sender's MPC wallet has SOL for fees, creates an ATA if needed, and transfers USDC from the treasury wallet on-chain
5. Wallet balance is updated in PostgreSQL

This closes the gap where funding is currently a DB-only operation with no Stripe payment and no on-chain USDC transfer.

## 2. User Stories

### US-1: Fund wallet via Stripe
**As a** sender,
**I want to** fund my wallet with USD via the API,
**So that** I receive USDC in my MPC wallet and can send remittances.

**Acceptance criteria:**
- `POST /api/wallets/{id}/fund {"amount": 25.00}` creates a Stripe PaymentIntent and returns a `FundingOrderResponse`
- If PaymentIntent creation succeeds: FundingOrder saved as `PAYMENT_CONFIRMED`, response status `PAYMENT_CONFIRMED`
- If PaymentIntent creation fails: FundingOrder saved as `FAILED`, error SP-0021 returned
- In test mode (configurable), PaymentIntent auto-confirms with test payment method
- Stripe webhook `payment_intent.succeeded` starts `WalletFundingWorkflow`
- Workflow transfers USDC from treasury ATA to sender ATA on Solana devnet
- Wallet `available_balance` and `total_balance` updated in DB
- FundingOrder transitions: PAYMENT_CONFIRMED → FUNDED
- Subsequent `GET /api/funding-orders/{fundingId}` shows current status
- Amount must be between 1.00 and 10,000.00 USDC with max 6 decimal places

### US-2: Refund funding order
**As a** sender,
**I want to** refund a funding order,
**So that** my fiat is refunded via Stripe.

**Acceptance criteria:**
- `POST /api/funding-orders/{fundingId}/refund` initiates a refund
- Validates sender still holds the USDC on-chain: sender ATA balance >= refund amount AND DB `available_balance` >= refund amount
- If insufficient USDC (already spent on remittances): rejects with SP-0025 `InsufficientBalanceForRefundException`
- Stripe refund executed via `PaymentGateway.refund`
- On Stripe success: FundingOrder transitions FUNDED → REFUND_INITIATED → REFUNDED; wallet `available_balance` and `total_balance` decremented in the same transaction as the REFUNDED status flip
- On Stripe failure: FundingOrder transitions FUNDED → REFUND_INITIATED → REFUND_FAILED (manual resolution)
- **On-chain USDC is NOT returned to treasury.** Sender's ATA is owned by sender's MPC wallet, so only the MPC key can authorize a debit. Hackathon accepts that devnet test-USDC remains in the sender's ATA post-refund; the on-chain balance check acts as a gate to prevent refunding USDC already spent on remittances. See §9.3 and appendix #20.

### US-3: Webhook idempotency and failure handling
**As the** system,
**I want** duplicate webhooks to be safely ignored and payment failures to be recorded,
**So that** a retry from Stripe doesn't double-fund a wallet and failed payments are tracked.

**Acceptance criteria:**
- If FundingOrder is already `FUNDED`, webhook returns 200 without re-processing
- If FundingOrder is already `FAILED`, webhook returns 200 without re-processing
- FundingOrder status acts as the idempotency guard
- `payment_intent.succeeded`: transitions PAYMENT_CONFIRMED → starts workflow → eventually FUNDED
- `payment_intent.payment_failed`: transitions PAYMENT_CONFIRMED → FAILED, no workflow started
- Unknown event types: logged and ignored, return 200 (prevent Stripe retries)
- Webhook always returns 200 — errors logged internally, never surfaced to Stripe

### US-4: SOL and ATA provisioning
**As the** system,
**I want** the funding workflow to ensure the sender has SOL and a USDC ATA,
**So that** the sender can transact immediately after funding without manual setup.

**Acceptance criteria:**
- If sender SOL balance < 0.005, transfer 0.01 SOL from treasury
- If sender USDC ATA does not exist, create it (treasury pays rent)
- Both checks happen before the USDC transfer

### US-5: Query funding order status
**As a** sender,
**I want to** check the status of my funding order,
**So that** I know when USDC is available in my wallet.

**Acceptance criteria:**
- `GET /api/funding-orders/{fundingId}` returns current FundingOrder status
- Returns 404 (SP-0020) if not found

## 3. Architecture

### 3.1 Domain Model

```
domain/funding/
├── model/
│   ├── FundingOrder.java          # Immutable record with @Builder
│   └── FundingStatus.java         # Enum: PAYMENT_CONFIRMED, FUNDED, FAILED,
│                                  #        REFUND_INITIATED, REFUNDED, REFUND_FAILED
├── handler/
│   ├── InitiateFundingHandler.java    # Creates FundingOrder + Stripe PaymentIntent
│   ├── CompleteFundingHandler.java    # Called by webhook → starts Temporal workflow
│   ├── FailFundingHandler.java        # Called by webhook on payment_failed
│   ├── GetFundingOrderHandler.java    # Query funding order by ID
│   └── RefundFundingHandler.java      # Refund USDC + Stripe refund
├── port/
│   ├── PaymentGateway.java            # Abstracts Stripe (initiatePayment, refund)
│   ├── FundingOrderRepository.java    # CRUD for FundingOrder
│   │     Methods:
│   │       save(FundingOrder) → FundingOrder
│   │       findByFundingId(UUID) → Optional<FundingOrder>
│   │       findByStripePaymentIntentId(String) → Optional<FundingOrder>
│   │       findByWalletIdAndStatusIn(Long, List<FundingStatus>) → List<FundingOrder>
│   └── FundingWorkflowStarter.java    # Starts WalletFundingWorkflow (abstracts Temporal)
└── exception/
    ├── FundingOrderNotFoundException.java
    ├── FundingFailedException.java
    ├── FundingAlreadyCompletedException.java
    ├── RefundNotAllowedException.java
    └── InsufficientBalanceForRefundException.java
```

### 3.2 Infrastructure

```
infrastructure/stripe/
├── StripePaymentAdapter.java      # Implements PaymentGateway via Stripe Java SDK
├── StripeProperties.java          # apiKey, webhookSecret, testMode, autoConfirm,
│                                  # testPaymentMethod — all configurable via props
└── StripeConfig.java              # @Configuration: StripeClient bean

infrastructure/temporal/
├── WalletFundingWorkflow.java         # Workflow interface
├── WalletFundingWorkflowImpl.java     # Orchestrates 6 activities
├── WalletFundingActivities.java       # Activity interface
├── WalletFundingActivitiesImpl.java   # Activity implementations
└── TemporalFundingWorkflowStarter.java # Implements FundingWorkflowStarter

infrastructure/db/funding/
├── FundingOrderEntity.java
├── FundingOrderEntityMapper.java      # MapStruct
├── FundingOrderJpaRepository.java
└── FundingOrderRepositoryAdapter.java

infrastructure/solana/
└── TreasuryServiceAdapter.java        # REPLACE stub with real:
    ├── transferUsdc()                 #   SPL Transfer: treasury ATA → sender ATA
    ├── transferSol()                  #   SOL Transfer: treasury → sender
    ├── getUsdcBalance()               #   Query on-chain token balance
    ├── getSolBalance()                #   Query on-chain SOL balance
    └── createAtaIfNeeded()            #   Create ATA with CreateAssociatedTokenAccountInstruction
```

### 3.3 Application Layer

```
application/controller/webhook/
└── StripeWebhookController.java       # POST /webhooks/stripe
                                       # Raw body + Stripe-Signature header
                                       # Verifies signature, parses event, calls handler

application/controller/funding/
├── FundingController.java             # POST /api/wallets/{id}/fund
│                                      # GET /api/funding-orders/{fundingId}
│                                      # POST /api/funding-orders/{fundingId}/refund
└── mapper/
    └── FundingApiMapper.java          # MapStruct: domain → response DTO

application/dto/
├── FundingOrderResponse.java          # fundingId, walletId, amountUsdc, status,
│                                      # stripePaymentIntentId, stripeClientSecret
└── FundWalletRequest.java             # amount (existing — add min/max/scale validation)
```

### 3.4 State Machine

```
                              PAYMENT_CONFIRMED ──────────────► FUNDED
                                │                                 │
                                └──► FAILED                       ├──► REFUND_INITIATED ──► REFUNDED
                                                                  │              │
                                                                  │              └──► REFUND_FAILED
                                                                  │
                              (InitiateFundingHandler sets        │
                               PAYMENT_CONFIRMED on success,     │
                               FAILED on Stripe API error)       │
```

| From | Trigger | To |
|---|---|---|
| — | Stripe PaymentIntent created successfully | PAYMENT_CONFIRMED |
| — | Stripe PaymentIntent creation failed | FAILED |
| PAYMENT_CONFIRMED | Webhook `payment_intent.succeeded` + workflow completes | FUNDED |
| PAYMENT_CONFIRMED | Webhook `payment_intent.payment_failed` | FAILED |
| PAYMENT_CONFIRMED | Workflow fails (on-chain transfer failed after retries) | FAILED |
| FUNDED | Refund requested | REFUND_INITIATED |
| REFUND_INITIATED | USDC returned + Stripe refund completed | REFUNDED |
| REFUND_INITIATED | USDC return or Stripe refund failed | REFUND_FAILED |

**Ordering in `InitiateFundingHandler`:**
1. Save FundingOrder with status `PAYMENT_CONFIRMED` to DB **first** (fundingId is the durable key)
2. Then call Stripe to create PaymentIntent with `metadata.funding_id = fundingId.toString()` and `metadata.wallet_id = walletId.toString()`
3. Update FundingOrder with `stripePaymentIntentId`
4. If Stripe call fails: transition to `FAILED`

This ordering is critical because with `confirm: true`, Stripe may fire the webhook **before** the fund endpoint returns. **The webhook looks up FundingOrder by `metadata.funding_id` (echoed back by Stripe in the event), NOT by PaymentIntent ID** — because PI ID isn't persisted until step 3. Looking up by funding_id guarantees the record is found regardless of timing.

**Concurrent fund requests:** the duplicate-order check (`findByWalletIdAndStatusIn(walletId, [PAYMENT_CONFIRMED])`) is not atomic with the insert. Two simultaneous fund calls for the same wallet can both pass the check. The DB backstop is a partial unique index `UNIQUE(wallet_id) WHERE status = 'PAYMENT_CONFIRMED'` (see §6). The handler catches `DataIntegrityViolationException` and maps it to SP-0022.

**USD to USDC conversion:** 1 USD = 1 USDC (hackathon assumption). Stripe amount in cents = `amountUsdc × 100`. Amount is validated to **2 decimal places max** (Stripe requires whole cents); sub-cent amounts rejected at the API layer by `@Digits(integer=5, fraction=2)`. Conversion done in `StripePaymentAdapter` via `amount.movePointRight(2).longValueExact()`.

## 4. API Contracts

### 4.1 Fund Wallet

```
POST /api/wallets/{id}/fund
Content-Type: application/json

{"amount": 25.00}
```

**Validation:**
- `amount`: required, positive, min 1.00, max 10000.00, **max 2 decimal places** (Stripe requires whole cents; sub-cent amounts rejected as `@Digits(integer=5, fraction=2)` violation → SP-0003)

**Response 201:**
```json
{
    "fundingId": "a1b2c3d4-uuid",
    "walletId": 4,
    "amountUsdc": 25.00,
    "status": "PAYMENT_CONFIRMED",
    "stripePaymentIntentId": "pi_3Mn...",
    "stripeClientSecret": "pi_3Mn..._secret_...",
    "createdAt": "2026-04-16T15:00:00Z"
}
```

**Error responses:**
| HTTP | Code | Condition |
|---|---|---|
| 404 | SP-0006 | Wallet not found |
| 400 | SP-0003 | Amount validation failed |
| 502 | SP-0021 | Stripe PaymentIntent creation failed |

### 4.2 Get Funding Order Status

```
GET /api/funding-orders/{fundingId}
```

**Response 200:**
```json
{
    "fundingId": "a1b2c3d4-uuid",
    "walletId": 4,
    "amountUsdc": 25.00,
    "status": "FUNDED",
    "stripePaymentIntentId": "pi_3Mn...",
    "createdAt": "2026-04-16T15:00:00Z"
}
```

**Note:** `stripeClientSecret` is NOT returned on GET — only on the initial POST response. It is sensitive and should not be re-exposed.

### 4.3 Refund Funding Order

```
POST /api/funding-orders/{fundingId}/refund
```

**Response 200:**
```json
{
    "fundingId": "a1b2c3d4-uuid",
    "walletId": 4,
    "amountUsdc": 25.00,
    "status": "REFUND_INITIATED",
    "stripePaymentIntentId": "pi_3Mn...",
    "createdAt": "2026-04-16T15:00:00Z"
}
```

**Error responses:**
| HTTP | Code | Condition |
|---|---|---|
| 404 | SP-0020 | Funding order not found |
| 409 | SP-0023 | Refund on non-FUNDED order |
| 400 | SP-0025 | Insufficient USDC balance (already spent on remittances) |
| 502 | SP-0024 | Stripe refund or on-chain return failed |

### 4.4 Stripe Webhook

```
POST /webhooks/stripe
Stripe-Signature: t=...,v1=...
Content-Type: application/json

{raw Stripe event body}
```

**Response:** Always 200. Internal errors logged but never surfaced to Stripe.

**Implementation note:** The controller MUST read the raw request body (`HttpServletRequest.getInputStream()` or `@RequestBody String payload`) — not a typed DTO. Stripe's `constructEvent(payload, sigHeader, secret)` requires the exact bytes for HMAC signature verification. Jackson deserialization would alter the payload and break the signature.

**Handled event types:**
| Event | Action |
|---|---|
| `payment_intent.succeeded` | Lookup FundingOrder by `metadata.funding_id`, transition to FUNDED via workflow |
| `payment_intent.payment_failed` | Lookup FundingOrder by `metadata.funding_id`, transition to FAILED |
| Any other | Log at DEBUG level, ignore |

## 5. Temporal Workflows

### 5.1 WalletFundingWorkflow

**Task queue:** `stablepay-wallet-funding`

```
WalletFundingWorkflow.execute(WalletFundingRequest)
    │
    ├─ Activity 1: checkTreasuryBalance          (10s timeout, 1 attempt)
    │   ├─ Query treasury USDC balance via RPC
    │   └─ If < requested amount → fail workflow (transition to FAILED)
    │
    ├─ Activity 2: ensureSolBalance              (30s timeout, 3 retries)
    │   ├─ Query sender SOL balance via RPC
    │   └─ If < 0.005 SOL → transfer 0.01 SOL from treasury
    │
    ├─ Activity 3: createAtaIfNeeded             (30s timeout, 3 retries)
    │   ├─ Check if sender USDC ATA exists via getAccountInfo
    │   └─ If not → CreateAssociatedTokenAccountInstruction (treasury pays)
    │
    ├─ Activity 4: transferUsdc                  (60s timeout, 3 retries)
    │   ├─ Build SPL Transfer instruction
    │   ├─ Sign with treasury keypair
    │   └─ Submit to Solana RPC
    │
    └─ Activity 5: finalizeFunding                (15s timeout, 3 retries, @Transactional)
        ├─ Load wallet with pessimistic lock (SELECT ... FOR UPDATE)
        ├─ Load FundingOrder by fundingId
        ├─ Idempotency guard: if FundingOrder.status == FUNDED → return (no-op)
        ├─ DB: available_balance += amount, total_balance += amount
        ├─ FundingOrder.status = FUNDED, updated_at = now()
        └─ Commit (status flip + balance increment in ONE transaction)
```

**Failure handling:**
- If any activity exhausts retries, the workflow fails
- `CompleteFundingHandler` catches workflow failure and transitions FundingOrder to `FAILED`
- The on-chain state is the source of truth — if USDC was transferred but DB update failed, Temporal retries `finalizeFunding`

**Why activities 5 and 6 are merged (rev 5 fix):** previously `updateWalletBalance` and `updateFundingOrderStatus` were separate activities with a status-based idempotency guard. That guard was broken: status stayed `PAYMENT_CONFIRMED` until the *second* activity, so a retry of activity 5 (commit succeeded, ack failed) would re-read `PAYMENT_CONFIRMED` and increment again. Merging them makes the status flip and the increment atomic; the first successful commit sets `FUNDED`, and any retry finds `FUNDED` on re-read and short-circuits.

**Input:**
```java
@Builder(toBuilder = true)
public record WalletFundingRequest(
    UUID fundingId,
    Long walletId,
    String senderSolanaAddress,
    BigDecimal amountUsdc
) {}
```

## 6. Database Migration

### V5__create_funding_orders.sql

```sql
CREATE TABLE funding_orders (
    id                         BIGSERIAL PRIMARY KEY,
    funding_id                 UUID UNIQUE NOT NULL,
    wallet_id                  BIGINT NOT NULL REFERENCES wallets(id),
    amount_usdc                NUMERIC(19, 6) NOT NULL,
    stripe_payment_intent_id   VARCHAR(255) UNIQUE,
    status                     VARCHAR(50) NOT NULL DEFAULT 'PAYMENT_CONFIRMED',
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ
);

CREATE INDEX idx_funding_orders_wallet_id ON funding_orders(wallet_id);
CREATE INDEX idx_funding_orders_stripe_pi ON funding_orders(stripe_payment_intent_id);
CREATE INDEX idx_funding_orders_status ON funding_orders(status);

-- At most one in-flight PAYMENT_CONFIRMED order per wallet. DB-level backstop
-- for the non-atomic read-then-insert in InitiateFundingHandler. A race past
-- the handler check surfaces as DataIntegrityViolationException, which is
-- translated to SP-0022 FundingAlreadyInProgressException.
CREATE UNIQUE INDEX idx_funding_orders_one_active_per_wallet
    ON funding_orders(wallet_id)
    WHERE status = 'PAYMENT_CONFIRMED';
```

**Note:** `stripe_client_secret` is NOT persisted. It is returned only in the initial POST response and discarded. Storing it would be a security risk — it allows anyone to confirm the payment.

## 7. Configuration

### application.yml (new properties)

```yaml
stablepay:
  stripe:
    api-key: ${STRIPE_API_KEY:}
    webhook-secret: ${STRIPE_WEBHOOK_SECRET:}
    test-mode: ${STRIPE_TEST_MODE:true}
    auto-confirm: ${STRIPE_AUTO_CONFIRM:true}
    test-payment-method: ${STRIPE_TEST_PAYMENT_METHOD:pm_card_visa}
    currency: ${STRIPE_CURRENCY:usd}
  treasury:
    private-key: ${TREASURY_PRIVATE_KEY:}
```

### .env.example additions

```
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
TREASURY_PRIVATE_KEY=<base58 secret key>
```

### docker-compose.yml additions

```yaml
STRIPE_API_KEY: ${STRIPE_API_KEY}
STRIPE_WEBHOOK_SECRET: ${STRIPE_WEBHOOK_SECRET}
TREASURY_PRIVATE_KEY: ${TREASURY_PRIVATE_KEY}
```

### build.gradle.kts addition

```kotlin
implementation("com.stripe:stripe-java:28.2.0")
```

## 8. Environment Setup (Devnet)

```bash
# 1. Generate treasury keypair
solana-keygen new -o treasury-keypair.json

# 2. Fund treasury with SOL (for tx fees + user SOL top-ups)
solana airdrop 5 <treasury-address> --url devnet

# 3. Create treasury USDC ATA and mint test tokens
spl-token create-account <USDC_MINT> --owner <treasury-address> --url devnet
spl-token mint <USDC_MINT> 10000 --recipient-owner <treasury-address> --url devnet

# 4. Export treasury private key as base58
# Add to .env as TREASURY_PRIVATE_KEY=<base58>

# 5. Start Stripe CLI for webhook forwarding
stripe listen --forward-to localhost:8080/webhooks/stripe
# Copy whsec_... to .env as STRIPE_WEBHOOK_SECRET
```

## 9. Sequence Diagrams

### 9.1 Happy Path: Fund → Webhook → USDC Transfer

```
Sender              API              Stripe           Webhook          Temporal          Solana
  │                  │                 │                 │                │                │
  │ POST /fund       │                 │                 │                │                │
  │ {amount: 25}     │                 │                 │                │                │
  │─────────────────►│                 │                 │                │                │
  │                  │ PaymentIntent    │                 │                │                │
  │                  │ create(2500,usd) │                 │                │                │
  │                  │────────────────►│                 │                │                │
  │                  │   pi_... + secret│                 │                │                │
  │                  │◄────────────────│                 │                │                │
  │                  │                 │                 │                │                │
  │                  │ Save FundingOrder│                 │                │                │
  │                  │ (PAYMENT_CONFIRMED)               │                │                │
  │  201 {fundingId, │                 │                 │                │                │
  │   status:CONFIRMED}               │                 │                │                │
  │◄─────────────────│                 │                 │                │                │
  │                  │                 │                 │                │                │
  │                  │                 │  pi.succeeded    │                │                │
  │                  │                 │────────────────►│                │                │
  │                  │                 │                 │ Verify sig     │                │
  │                  │                 │                 │ Check status   │                │
  │                  │                 │                 │ (idempotent)   │                │
  │                  │                 │                 │                │                │
  │                  │                 │                 │ Start workflow │                │
  │                  │                 │                 │───────────────►│                │
  │                  │                 │                 │                │                │
  │                  │                 │    200 OK       │                │                │
  │                  │                 │◄────────────────│                │                │
  │                  │                 │                 │                │                │
  │                  │                 │                 │ checkTreasury  │                │
  │                  │                 │                 │                │ query balance  │
  │                  │                 │                 │                │───────────────►│
  │                  │                 │                 │                │                │
  │                  │                 │                 │  ensureSol     │                │
  │                  │                 │                 │                │ check + send   │
  │                  │                 │                 │                │───────────────►│
  │                  │                 │                 │                │                │
  │                  │                 │                 │  createATA     │                │
  │                  │                 │                 │                │ check + create │
  │                  │                 │                 │                │───────────────►│
  │                  │                 │                 │                │                │
  │                  │                 │                 │  transferUSDC  │                │
  │                  │                 │                 │                │ SPL Transfer   │
  │                  │                 │                 │                │───────────────►│
  │                  │                 │                 │                │   finalized    │
  │                  │                 │                 │                │◄───────────────│
  │                  │                 │                 │                │                │
  │                  │                 │                 │ updateBalance  │                │
  │                  │                 │                 │ (idempotent)   │                │
  │                  │                 │                 │  DB: +25 USDC  │                │
  │                  │                 │                 │                │                │
  │                  │                 │                 │ status: FUNDED │                │
  │                  │                 │                 │                │                │
  │                  │                 │                 │                │                │
  │ GET /funding-orders/{id}           │                 │                │                │
  │─────────────────►│                 │                 │                │                │
  │  {status: FUNDED}│                 │                 │                │                │
  │◄─────────────────│                 │                 │                │                │
```

### 9.2 Failure Path: Stripe Payment Failed

```
Sender              API              Stripe           Webhook
  │ POST /fund       │                 │                 │
  │─────────────────►│                 │                 │
  │                  │ create PI       │                 │
  │                  │────────────────►│                 │
  │                  │◄────────────────│                 │
  │  201 {CONFIRMED} │                 │                 │
  │◄─────────────────│                 │                 │
  │                  │                 │  pi.failed      │
  │                  │                 │────────────────►│
  │                  │                 │                 │ Transition →   │
  │                  │                 │                 │ FAILED         │
  │                  │                 │    200 OK       │ (no workflow)  │
  │                  │                 │◄────────────────│                │
```

### 9.3 Refund Path (Stripe-only with on-chain balance gate)

```
Sender              API              Solana           Stripe
  │ POST /refund     │                 │                 │
  │─────────────────►│                 │                 │
  │                  │ Load FundingOrder                 │
  │                  │ must be FUNDED  │                 │
  │                  │ (else SP-0023)  │                 │
  │                  │                 │                 │
  │                  │ Check sender ATA│                 │
  │                  │ balance >= amt  │                 │
  │                  │────────────────►│                 │
  │                  │◄───────────────│                 │
  │                  │ (else SP-0025)  │                 │
  │                  │                 │                 │
  │                  │ status: FUNDED  │                 │
  │                  │ → REFUND_INITIATED                │
  │                  │                 │                 │
  │                  │ Stripe refund(pi_...)             │
  │                  │──────────────────────────────────►│
  │                  │                 │    success      │
  │                  │◄──────────────────────────────────│
  │                  │                 │                 │
  │                  │ In ONE tx:      │                 │
  │                  │  wallet.balance -= amt            │
  │                  │  status: REFUNDED                 │
  │  {status:        │                 │                 │
  │   REFUND_INITIATED}                │                 │
  │◄─────────────────│                 │                 │
```

**On Stripe failure:** `status: REFUND_INITIATED → REFUND_FAILED`. Wallet balance not decremented. Manual resolution.

**Why no on-chain USDC return:** Sender's ATA is owned by the sender's MPC wallet, not the treasury. Only the MPC key can authorize a debit. Adding MPC-signed return would require wiring `MpcWalletClient` into the refund path plus a full signing ceremony — out of scope for the hackathon. On devnet the USDC is a test token with no monetary value, so it remaining in the sender ATA is acceptable. The on-chain balance check prevents refunding USDC that's already been spent on a remittance (which would be a real platform loss). See appendix #20.

## 10. Error Handling

| Error Code | HTTP | Exception | When Thrown |
|---|---|---|---|
| SP-0020 | 404 | FundingOrderNotFoundException | `GET /funding-orders/{id}` or `POST /refund` — order not found |
| SP-0021 | 502 | FundingFailedException | Stripe PaymentIntent creation failed in `InitiateFundingHandler` |
| SP-0022 | 409 | FundingAlreadyInProgressException | `POST /fund` called for a wallet that already has a `PAYMENT_CONFIRMED` (in-flight) order. Thrown by the handler's pre-check OR mapped from `DataIntegrityViolationException` on the partial unique index race. |
| SP-0023 | 409 | RefundNotAllowedException | `POST /refund` on a FundingOrder not in FUNDED status |
| SP-0024 | 502 | RefundFailedException | Stripe refund API or on-chain USDC return failed |
| SP-0025 | 400 | InsufficientBalanceForRefundException | Refund requested but sender's on-chain ATA or DB balance is less than refund amount (USDC already spent on remittances) |

**GlobalExceptionHandler additions:**
- `FundingOrderNotFoundException` → 404
- `FundingFailedException` → 502
- `FundingAlreadyInProgressException` → 409
- `RefundNotAllowedException` → 409
- `RefundFailedException` → 502
- `InsufficientBalanceForRefundException` → 400
- `DataIntegrityViolationException` (scoped to funding_orders wallet_id unique violation) → translate to 409 SP-0022

## 11. Testing Strategy

### Unit Tests (new)
- `InitiateFundingHandlerTest` — creates FundingOrder, calls PaymentGateway, handles Stripe failure
- `CompleteFundingHandlerTest` — transitions PAYMENT_CONFIRMED, starts workflow, handles idempotent calls
- `FailFundingHandlerTest` — transitions PAYMENT_CONFIRMED → FAILED
- `GetFundingOrderHandlerTest` — returns order, handles not found
- `RefundFundingHandlerTest` — validates balance, transitions FUNDED → REFUND_INITIATED, handles insufficient balance
- `StripePaymentAdapterTest` — mocks StripeClient, verifies PaymentIntent params, tests test-mode config
- `WalletFundingActivitiesImplTest` — mocks TreasuryService, verifies on-chain calls, tests idempotency of balance update
- `StripeWebhookControllerTest` — signature validation, event parsing, succeeded/failed/unknown events, handler invocation

### Modified Tests
- `WalletControllerTest` — remove fund endpoint tests (moved to FundingController)
- `WalletApiIntegrationTest` — update fund tests to use new FundingController response format
- `FundWalletHandlerTest` — delete (handler is replaced by InitiateFundingHandler)
- `TreasuryServiceAdapterTest` — rewrite for real SPL/SOL transfer logic

### Integration Tests (new)
- `WalletFundingWorkflowIntegrationTest` — Temporal TestWorkflowEnvironment, mocked activities
- `FundingOrderRepositoryIntegrationTest` — TestContainers PostgreSQL

### E2E Test (Devnet)
- Full flow: fund API → Stripe test PaymentIntent → webhook → Temporal → on-chain USDC transfer → balance update → query status

## 12. Files to Create/Modify

### New Files (23)
| Layer | File |
|---|---|
| Domain | `domain/funding/model/FundingOrder.java` |
| Domain | `domain/funding/model/FundingStatus.java` |
| Domain | `domain/funding/model/PaymentRequest.java` |
| Domain | `domain/funding/model/PaymentResult.java` |
| Domain | `domain/funding/handler/InitiateFundingHandler.java` |
| Domain | `domain/funding/handler/CompleteFundingHandler.java` |
| Domain | `domain/funding/handler/FailFundingHandler.java` |
| Domain | `domain/funding/handler/GetFundingOrderHandler.java` |
| Domain | `domain/funding/handler/RefundFundingHandler.java` |
| Domain | `domain/funding/port/PaymentGateway.java` |
| Domain | `domain/funding/port/FundingOrderRepository.java` |
| Domain | `domain/funding/port/FundingWorkflowStarter.java` |
| Domain | `domain/funding/exception/FundingOrderNotFoundException.java` |
| Domain | `domain/funding/exception/FundingFailedException.java` |
| Domain | `domain/funding/exception/FundingAlreadyInProgressException.java` |
| Domain | `domain/funding/exception/RefundNotAllowedException.java` |
| Domain | `domain/funding/exception/InsufficientBalanceForRefundException.java` |
| Domain | `domain/funding/exception/RefundFailedException.java` |
| Infra | `infrastructure/stripe/StripePaymentAdapter.java` |
| Infra | `infrastructure/stripe/StripeProperties.java` |
| Infra | `infrastructure/stripe/StripeConfig.java` |
| Infra | `infrastructure/temporal/WalletFundingWorkflow.java` |
| Infra | `infrastructure/temporal/WalletFundingWorkflowImpl.java` |
| Infra | `infrastructure/temporal/WalletFundingActivities.java` |
| Infra | `infrastructure/temporal/WalletFundingActivitiesImpl.java` |
| Infra | `infrastructure/temporal/TemporalFundingWorkflowStarter.java` |
| Infra | `infrastructure/db/funding/FundingOrderEntity.java` |
| Infra | `infrastructure/db/funding/FundingOrderEntityMapper.java` |
| Infra | `infrastructure/db/funding/FundingOrderJpaRepository.java` |
| Infra | `infrastructure/db/funding/FundingOrderRepositoryAdapter.java` |
| App | `application/controller/webhook/StripeWebhookController.java` |
| App | `application/controller/funding/FundingController.java` |
| App | `application/controller/funding/mapper/FundingApiMapper.java` |
| App | `application/dto/FundingOrderResponse.java` |
| DB | `db/migration/V5__create_funding_orders.sql` |

### Modified Files (9)
| File | Change |
|---|---|
| `application/controller/wallet/WalletController.java` | Remove `fundWallet` method + `FundWalletHandler` import — funding moved to `FundingController` |
| `domain/wallet/handler/FundWalletHandler.java` | Delete — replaced by `InitiateFundingHandler` in funding subdomain |
| `application/dto/FundWalletRequest.java` | Add `@Min(1)`, `@Max(10000)`, `@Digits(integer=5, fraction=6)` |
| `application/config/GlobalExceptionHandler.java` | Add handlers for 5 new funding exceptions |
| `infrastructure/solana/TreasuryServiceAdapter.java` | Replace stub with real SPL + SOL transfers. Depends on `SolanaProperties` (for USDC mint address), `Connection` (for RPC). Uses `SplTransferInstruction(from, to, owner, mint, amount, decimals=6)` for USDC and `TransferInstruction(from, to, lamports)` for SOL. Treasury keypair loaded from `stablepay.treasury.private-key`. |
| `domain/wallet/port/TreasuryService.java` | Rename `transferFromTreasury` → `transferUsdc`. Add `transferSol`, `getSolBalance`, `getUsdcBalance`, `createAtaIfNeeded`. (The sole existing caller `FundWalletHandler` is being deleted; no backwards-compat alias needed.) |
| `infrastructure/temporal/TaskQueue.java` | Add `WALLET_FUNDING` task queue |
| `infrastructure/temporal/TemporalConfig.java` | Refactor to register BOTH workers (remittance + funding) before calling `workerFactory.start()` once. Current code calls `start()` in the remittance worker bean — must be moved to a separate `@Bean` that depends on both workers. |
| `build.gradle.kts` | Add `com.stripe:stripe-java:28.2.0` |
| `application.yml` | Add `stablepay.stripe.*` and `stablepay.treasury.*` properties |

### Modified Test Files (4)
| File | Change |
|---|---|
| `WalletControllerTest.java` | Remove fund endpoint test methods |
| `WalletApiIntegrationTest.java` | Update fund tests for new response format |
| `FundWalletHandlerTest.java` | Delete (replaced by InitiateFundingHandlerTest) |
| `TreasuryServiceAdapterTest.java` | Rewrite for real SPL/SOL transfer logic |

### Postman Collection
| File | Change |
|---|---|
| `docs/StablePay.postman_collection.json` | Update fund request to expect `FundingOrderResponse`, add GET funding order, add refund |

## 13. Implementation Order

| # | Task | Dependencies | Est. Size |
|---|---|---|---|
| 1 | Domain models: FundingOrder, FundingStatus, PaymentRequest, PaymentResult | None | S |
| 2 | Domain ports: PaymentGateway, FundingOrderRepository, FundingWorkflowStarter | Task 1 | S |
| 3 | Domain exceptions (5 exception classes) | None | S |
| 4 | DB migration V5 + FundingOrderEntity + JpaRepo + Mapper + Adapter | Task 1, 2 | M |
| 5 | StripeProperties + StripeConfig + StripePaymentAdapter | Task 2 | M |
| 6 | InitiateFundingHandler + GetFundingOrderHandler + FundingController + FundingOrderResponse | Task 2, 3, 4, 5 | M |
| 7 | StripeWebhookController + CompleteFundingHandler + FailFundingHandler | Task 4, 6 | M |
| 8 | TreasuryServiceAdapter: real SPL + SOL transfers | None (can parallel) | L |
| 9 | WalletFundingWorkflow + Activities (6 activities, idempotent balance update) | Task 7, 8 | L |
| 10 | RefundFundingHandler + refund endpoint (with balance validation) | Task 5, 6, 8 | M |
| 11 | Update existing tests + new tests (unit + integration) | Task 1-10 | L |
| 12 | GlobalExceptionHandler + FundWalletRequest validation + Postman collection | Task 6, 10 | S |
| 13 | Config: .env.example, docker-compose, Makefile stripe-listen | Task 5, 7 | S |
| 14 | E2E devnet test | Task 1-13 | M |

## 14. Out of Scope

- Stripe Customer objects (no persistent customer-to-Stripe mapping)
- Saved payment methods / recurring payments
- KYC/AML verification
- Reconciliation jobs (production concern)
- Kafka event publishing (no Kafka in StablePay)
- Partial refunds (full refund only)
- Multiple currency support (USD only)
- Rate limiting on fund endpoint
- Storing `stripe_client_secret` in DB (returned once on POST, then discarded)

## Appendix: Edge Cases Addressed in Rev 2

| # | Issue | Resolution |
|---|---|---|
| 1 | State machine gap: PENDING never transitions to PAYMENT_CONFIRMED | Removed PENDING state. InitiateFundingHandler saves as PAYMENT_CONFIRMED directly after Stripe succeeds. |
| 2 | Refund when sender already spent USDC | Added balance validation: check on-chain ATA + DB balance before refund. SP-0025 error. |
| 3 | payment_intent.payment_failed not handled | Added FailFundingHandler + webhook routing for failed events. |
| 4 | Existing tests break when fund moves to FundingController | Listed WalletControllerTest, WalletApiIntegrationTest, FundWalletHandlerTest as modified/deleted. |
| 5 | Unknown webhook events not handled | Spec: log at DEBUG, return 200. Never surface errors to Stripe. |
| 6 | No way to poll funding status | Added GET /api/funding-orders/{fundingId} + GetFundingOrderHandler. |
| 7 | Treasury depletion not checked in workflow | Added Activity 1: checkTreasuryBalance before any transfers. |
| 8 | SP-0022 never defined when thrown | Clarified: thrown when duplicate fund request for same wallet with existing active order. |
| 9 | REFUND_INITIATED stuck if refund fails | Added REFUND_FAILED status and transition. |
| 10 | Migration missing default for status column | Added DEFAULT 'PAYMENT_CONFIRMED'. |
| 11 | stripe_client_secret stored in DB | Removed from migration. Returned only on POST, never persisted. |
| 12 | Amount validation missing | Added @Min(1), @Max(10000), @Digits(integer=5, fraction=6) to FundWalletRequest. |
| 13 | updateWalletBalance not idempotent on retry | Added idempotency guard: check FundingOrder status before incrementing balance. |
| 14 | WorkerFactory.start() called twice for two task queues | Refactor TemporalConfig: register both workers before single start() call. |
| 15 | Remittance refund doesn't release wallet DB balance (pre-existing bug) | Noted as known issue. Refund balance check uses on-chain ATA balance as primary source of truth, DB balance as secondary. On-chain balance is accurate even if DB is stale. |
| 16 | FundingOrderRepository port methods not specified | Added explicit method signatures: findByFundingId, findByStripePaymentIntentId, findByWalletIdAndStatusIn. |
| 17 | TreasuryServiceAdapter needs SolanaProperties for mint + decimals | Documented dependency: SplTransferInstruction needs (from, to, owner, mint, amount, decimals=6). |
| 18 | Webhook lookup race against save→Stripe ordering | **Revised (rev 5):** Webhook looks up FundingOrder by `metadata.funding_id` (set on PaymentIntent create, echoed in event), NOT by PaymentIntent ID. PI ID is only persisted after Stripe returns, so a webhook arriving in the save→Stripe window would miss on a PI-ID lookup. `funding_id` is the durable key. `findByStripePaymentIntentId` remains on the port for duplicate-PI detection and refund handler lookup. |
| 19 | Race: webhook arrives before InitiateFundingHandler saves FundingOrder | Save FundingOrder to DB FIRST (status PAYMENT_CONFIRMED), then call Stripe API, then update with PI ID. Record exists when webhook arrives. |
| 20 | Refund ordering: USDC return vs Stripe refund | **Revised (rev 5):** On-chain USDC return removed entirely. Sender's ATA is owned by the sender's MPC wallet; treasury cannot sign a debit. Refund is Stripe-only, gated by an on-chain balance check (SP-0025) that blocks refunds once USDC has been spent on a remittance. USDC remains in the sender ATA after refund — acceptable on devnet (test tokens). Production would require MPC-signed return. State machine: FUNDED → REFUND_INITIATED → (Stripe ok → REFUNDED; Stripe fail → REFUND_FAILED). |
| 21 | USD-to-USDC conversion not specified | 1 USD = 1 USDC (hackathon). Stripe amount = amountUsdc × 100 cents. Conversion in StripePaymentAdapter. |
| 22 | FundWalletHandler.java not in delete list | Added to modified files: delete FundWalletHandler (replaced by InitiateFundingHandler). |
| 23 | Webhook controller needs raw body for signature verification | Documented: must use HttpServletRequest.getInputStream() or @RequestBody String, not typed DTO. Jackson would break HMAC. |
| 24 | stripe_payment_intent_id missing UNIQUE constraint | Added UNIQUE to migration column definition. |

## Appendix: Edge Cases Addressed in Rev 5

| # | Issue | Resolution |
|---|---|---|
| 25 | Activity-5 status-based idempotency guard was broken | Merged `updateWalletBalance` + `updateFundingOrderStatus` into a single transactional activity `finalizeFunding`. Status flip and balance increment commit atomically, so any retry after a successful commit finds status=FUNDED and short-circuits. |
| 26 | Amount validation (6 decimal places) would crash Stripe conversion | Tightened `FundWalletRequest.amount` to `@Digits(integer=5, fraction=2)`. Stripe requires whole cents; `amount.movePointRight(2).longValueExact()` would throw `ArithmeticException` on sub-cent values. |
| 27 | SP-0022 exception name misleading | Renamed `FundingAlreadyCompletedException` → `FundingAlreadyInProgressException`. Thrown when a `PAYMENT_CONFIRMED` (in-flight) order exists — not a completed one. |
| 28 | CompleteFundingHandler idempotency missed refund terminal states | Added REFUND_INITIATED, REFUNDED, REFUND_FAILED to the "ignore silently" list. A stale webhook replay after a refund must not re-start the funding workflow. |
| 29 | Concurrent fund requests can race past the duplicate-order check | Added partial unique index `UNIQUE(wallet_id) WHERE status = 'PAYMENT_CONFIRMED'`. Handler catches `DataIntegrityViolationException` and maps to SP-0022. |
| 30 | Webhook endpoint security config not specified | `/webhooks/stripe` must be permitted without auth and excluded from CSRF. Signature verification via `Webhook.constructEvent(payload, sig, secret, 300)` is the authentication mechanism. |
| 31 | `funding_orders.created_at`/`updated_at` typed as `TIMESTAMP` would lose timezone info | Switched to `TIMESTAMPTZ` in V5 migration to match V1–V4 (`wallets`, `remittances`, `claim_tokens`) and round-trip cleanly with Java `Instant`. Same 8-byte storage; normalizes to UTC on write. |
