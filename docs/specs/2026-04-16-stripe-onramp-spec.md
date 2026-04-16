---
title: Stripe On-Ramp Integration for Wallet Funding
status: approved
created: 2026-04-16
issue: STA-72
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
- `POST /api/wallets/{id}/fund {"amount": 25.00}` creates a Stripe PaymentIntent and returns a `FundingOrderResponse` with status `PENDING`
- In test mode (configurable), PaymentIntent auto-confirms with test payment method
- Stripe webhook `payment_intent.succeeded` starts `WalletFundingWorkflow`
- Workflow transfers USDC from treasury ATA to sender ATA on Solana devnet
- Wallet `available_balance` and `total_balance` updated in DB
- FundingOrder transitions: PENDING → PAYMENT_CONFIRMED → FUNDED
- Subsequent `GET /api/wallets/{id}` shows updated balance

### US-2: Refund funding order
**As a** sender,
**I want to** refund a funding order,
**So that** my USDC is returned to the treasury and my fiat is refunded via Stripe.

**Acceptance criteria:**
- `POST /api/funding-orders/{fundingId}/refund` initiates a refund
- USDC transferred from sender ATA back to treasury ATA on-chain
- Stripe refund API called for the original PaymentIntent
- FundingOrder transitions: FUNDED → REFUND_INITIATED → REFUNDED
- Wallet `available_balance` and `total_balance` decremented

### US-3: Webhook idempotency
**As the** system,
**I want** duplicate webhooks to be safely ignored,
**So that** a retry from Stripe doesn't double-fund a wallet.

**Acceptance criteria:**
- If FundingOrder is already `FUNDED`, webhook returns 200 without re-processing
- FundingOrder status acts as the idempotency guard

### US-4: SOL and ATA provisioning
**As the** system,
**I want** the funding workflow to ensure the sender has SOL and a USDC ATA,
**So that** the sender can transact immediately after funding without manual setup.

**Acceptance criteria:**
- If sender SOL balance < 0.005, transfer 0.01 SOL from treasury
- If sender USDC ATA does not exist, create it (treasury pays rent)
- Both checks happen before the USDC transfer

## 3. Architecture

### 3.1 Domain Model

```
domain/funding/
├── model/
│   ├── FundingOrder.java          # Immutable record with @Builder
│   └── FundingStatus.java         # Enum: PENDING, PAYMENT_CONFIRMED, FUNDED, FAILED,
│                                  #        REFUND_INITIATED, REFUNDED
├── handler/
│   ├── InitiateFundingHandler.java    # Creates FundingOrder + Stripe PaymentIntent
│   ├── CompleteFundingHandler.java    # Called by webhook → starts Temporal workflow
│   └── RefundFundingHandler.java      # Refund USDC + Stripe refund
├── port/
│   ├── PaymentGateway.java            # Abstracts Stripe (initiatePayment, refund)
│   └── FundingOrderRepository.java    # CRUD for FundingOrder
└── exception/
    ├── FundingOrderNotFoundException.java
    └── FundingFailedException.java
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
├── WalletFundingWorkflowImpl.java     # Orchestrates 5 activities
├── WalletFundingActivities.java       # Activity interface
└── WalletFundingActivitiesImpl.java   # Activity implementations

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
���── FundingController.java             # POST /api/wallets/{id}/fund (modified)
│                                      # POST /api/funding-orders/{fundingId}/refund
└── mapper/
    └── FundingApiMapper.java          # MapStruct: domain → response DTO

application/dto/
├── FundingOrderResponse.java          # fundingId, walletId, amountUsdc, status,
│                                      # stripePaymentIntentId, stripeClientSecret
└── FundWalletRequest.java             # amount (existing, reused)
```

### 3.4 State Machine

```
PENDING ──────────────────► PAYMENT_CONFIRMED ──────► FUNDED
   │                              │                      │
   └──► FAILED                    └──► FAILED            ���──► REFUND_INITIATED ──► REFUNDED
```

| From | Trigger | To |
|---|---|---|
| PENDING | Stripe PaymentIntent created successfully | PAYMENT_CONFIRMED |
| PENDING | Stripe PaymentIntent creation failed | FAILED |
| PAYMENT_CONFIRMED | Webhook `payment_intent.succeeded` + USDC transferred | FUNDED |
| PAYMENT_CONFIRMED | Webhook `payment_intent.payment_failed` or USDC transfer failed | FAILED |
| FUNDED | Refund requested | REFUND_INITIATED |
| REFUND_INITIATED | USDC returned + Stripe refund completed | REFUNDED |

## 4. API Contracts

### 4.1 Fund Wallet (Modified)

```
POST /api/wallets/{id}/fund
Content-Type: application/json

{"amount": 25.00}
```

**Response 201:**
```json
{
    "fundingId": "a1b2c3d4-uuid",
    "walletId": 4,
    "amountUsdc": 25.00,
    "status": "PENDING",
    "stripePaymentIntentId": "pi_3Mn...",
    "stripeClientSecret": "pi_3Mn..._secret_...",
    "createdAt": "2026-04-16T15:00:00Z"
}
```

### 4.2 Refund Funding Order

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

### 4.3 Stripe Webhook

```
POST /webhooks/stripe
Stripe-Signature: t=...,v1=...
Content-Type: application/json

{raw Stripe event body}
```

**Response:** 200 (always, to prevent Stripe retries on processing errors)

## 5. Temporal Workflows

### 5.1 WalletFundingWorkflow

**Task queue:** `stablepay-wallet-funding`

```
WalletFundingWorkflow.execute(WalletFundingRequest)
    │
    ├─ Activity 1: ensureSolBalance          (30s timeout, 3 retries)
    │   ├─ Query sender SOL balance via RPC
    │   └─ If < 0.005 SOL → transfer 0.01 SOL from treasury
    │
    ├─ Activity 2: createAtaIfNeeded         (30s timeout, 3 retries)
    │   ├─ Check if sender USDC ATA exists via getAccountInfo
    │   └─ If not → CreateAssociatedTokenAccountInstruction (treasury pays)
    │
    ├─ Activity 3: transferUsdc              (60s timeout, 3 retries)
    │   ├─ Build SPL Transfer instruction
    │   ├─ Sign with treasury keypair
    │   └─ Submit to Solana RPC
    │
    ├�� Activity 4: updateWalletBalance       (10s timeout, 3 retries)
    │   └─ DB: available_balance += amount, total_balance += amount
    │
    └─ Activity 5: updateFundingOrderStatus  (10s timeout, 3 retries)
        └─ PAYMENT_CONFIRMED → FUNDED
```

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
    stripe_payment_intent_id   VARCHAR(255),
    stripe_client_secret       VARCHAR(255),
    status                     VARCHAR(50) NOT NULL,
    created_at                 TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMP
);

CREATE INDEX idx_funding_orders_wallet_id ON funding_orders(wallet_id);
CREATE INDEX idx_funding_orders_stripe_pi ON funding_orders(stripe_payment_intent_id);
CREATE INDEX idx_funding_orders_status ON funding_orders(status);
```

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

## 9. Sequence Diagram

```
Sender              API              Stripe           Webhook          Temporal          Solana
  │                  │                 │                 │                │                │
  │ POST /fund       │                 │                 │                │                │
  │ {amount: 25}     │                 │                 │                │                │
  │─────────────────►��                 │                 │                │                │
  │                  │ PaymentIntent    │                 │                │                │
  │                  │ create(2500,usd) │                 │                │                │
  │                  │────────────────►│                 │                │                │
  │                  │   pi_... + secret│                 │                │                │
  │                  │◄────────────────│                 │                │                │
  │                  │                 │                 │                │                │
  │                  │ Save FundingOrder│                 │                │                │
  │                  │ (PENDING)        │                 │                │                │
  │  201 {fundingId, │                 │                 │                │                │
  │   status:PENDING}│                 │                 │                │                │
  │◄─────────────────│                 │                 │                │                │
  │                  │                 │                 │                │                │
  │                  │                 │  pi.succeeded    │                │                │
  │                  │                 │────────────────►│                │                │
  │                  │                 │                 │ Verify sig     │                │
  │                  │                 │                 │ Parse event    │                │
  │                  │                 │                 │ Update:CONFIRMED                │
  │                  │                 │                 │                │                │
  │                  │                 │                 │ Start workflow │                │
  │                  │                 │                 │───────────────►│                │
  │                  │                 │                 │                │                │
  │                  │                 │    200 OK       │                │                │
  │                  │                 │◄────────────────│                │                │
  │                  │                 │                 │                │                │
  │                  │                 │                 │  ensureSol     │                │
  │                  │                 │                 │                │ check balance  │
  │                  │                 │                 │                │───────────────►│
  │                  │                 │                 │                │  transfer SOL  │
  │                  │                 │                 │                │─────────���─────►│
  │                  │                 │                 │                │                │
  │                  │                 │                 │  createATA     │                │
  │                  │                 ��                 │                │ getAccountInfo │
  │                  │                 │                 ���                │─��─────────────►│
  │                  │                 │                 │                │ createATA tx   │
  │                  │                 │                 │                │───────────────►│
  │                  │                 │                 │                │                │
  │                  │                 │                 │  transferUSDC  │                ��
  │                  │                 │                 │                │ SPL Transfer   │
  │                  │                 │                 │                │───────────────►│
  │                  │                 │                 │                │   finalized    │
  │                  │                 │                 │                │◄───────────────│
  │                  │                 │                 │                │                │
  │                  │                 │                 │  updateBalance │                │
  │                  │                 │                 │  DB: +25 USDC  │                │
  │                  │                 │                 │                │                │
  │                  │                 │                 │  status: FUNDED│                │
  │                  │                 │                 │                │                │
```

## 10. Error Handling

| Error Code | HTTP | Exception | Description |
|---|---|---|---|
| SP-0020 | 404 | FundingOrderNotFoundException | Funding order not found |
| SP-0021 | 502 | FundingFailedException | Stripe PaymentIntent creation failed |
| SP-0022 | 409 | FundingAlreadyCompletedException | Attempting to re-fund an already funded order |
| SP-0023 | 409 | RefundNotAllowedException | Refund on non-FUNDED order |
| SP-0024 | 502 | RefundFailedException | Stripe refund API or on-chain USDC return failed |

## 11. Testing Strategy

### Unit Tests
- `InitiateFundingHandlerTest` — creates FundingOrder, calls PaymentGateway, returns response
- `CompleteFundingHandlerTest` — transitions PAYMENT_CONFIRMED, starts Temporal workflow
- `RefundFundingHandlerTest` — transitions FUNDED → REFUND_INITIATED, calls PaymentGateway.refund
- `StripePaymentAdapterTest` — mocks StripeClient, verifies PaymentIntent params
- `WalletFundingActivitiesImplTest` — mocks TreasuryService, verifies on-chain calls
- `StripeWebhookControllerTest` — signature validation, event parsing, handler invocation

### Integration Tests
- `WalletFundingWorkflowIntegrationTest` — Temporal TestWorkflowEnvironment, mocked activities
- `FundingOrderRepositoryIntegrationTest` — TestContainers PostgreSQL

### E2E Test (Devnet)
- Full flow: fund API → Stripe test PaymentIntent → webhook → Temporal → on-chain USDC transfer → balance update

## 12. Files to Create/Modify

### New Files (19)
| Layer | File |
|---|---|
| Domain | `domain/funding/model/FundingOrder.java` |
| Domain | `domain/funding/model/FundingStatus.java` |
| Domain | `domain/funding/model/PaymentRequest.java` |
| Domain | `domain/funding/model/PaymentResult.java` |
| Domain | `domain/funding/handler/InitiateFundingHandler.java` |
| Domain | `domain/funding/handler/CompleteFundingHandler.java` |
| Domain | `domain/funding/handler/RefundFundingHandler.java` |
| Domain | `domain/funding/port/PaymentGateway.java` |
| Domain | `domain/funding/port/FundingOrderRepository.java` |
| Domain | `domain/funding/exception/FundingOrderNotFoundException.java` |
| Domain | `domain/funding/exception/FundingFailedException.java` |
| Infra | `infrastructure/stripe/StripePaymentAdapter.java` |
| Infra | `infrastructure/stripe/StripeProperties.java` |
| Infra | `infrastructure/stripe/StripeConfig.java` |
| Infra | `infrastructure/temporal/WalletFundingWorkflow.java` |
| Infra | `infrastructure/temporal/WalletFundingWorkflowImpl.java` |
| Infra | `infrastructure/temporal/WalletFundingActivities.java` |
| Infra | `infrastructure/temporal/WalletFundingActivitiesImpl.java` |
| Infra | `infrastructure/db/funding/FundingOrderEntity.java` |
| Infra | `infrastructure/db/funding/FundingOrderEntityMapper.java` |
| Infra | `infrastructure/db/funding/FundingOrderJpaRepository.java` |
| Infra | `infrastructure/db/funding/FundingOrderRepositoryAdapter.java` |
| App | `application/controller/webhook/StripeWebhookController.java` |
| App | `application/controller/funding/FundingController.java` |
| App | `application/controller/funding/mapper/FundingApiMapper.java` |
| App | `application/dto/FundingOrderResponse.java` |
| DB | `db/migration/V5__create_funding_orders.sql` |

### Modified Files (7)
| File | Change |
|---|---|
| `application/controller/wallet/WalletController.java` | Remove `fundWallet` — moved to `FundingController` |
| `infrastructure/solana/TreasuryServiceAdapter.java` | Replace stub with real SPL + SOL transfers |
| `domain/wallet/port/TreasuryService.java` | Add `transferSol`, `getSolBalance`, `getUsdcBalance`, `createAtaIfNeeded` |
| `infrastructure/temporal/TaskQueue.java` | Add `WALLET_FUNDING` task queue |
| `infrastructure/temporal/TemporalConfig.java` | Register new worker + workflow + activities |
| `build.gradle.kts` | Add `com.stripe:stripe-java:28.2.0` |
| `application.yml` | Add `stablepay.stripe.*` and `stablepay.treasury.*` properties |

## 13. Implementation Order

| # | Task | Dependencies | Est. Size |
|---|---|---|---|
| 1 | Domain models: FundingOrder, FundingStatus, PaymentRequest, PaymentResult | None | S |
| 2 | Domain ports: PaymentGateway, FundingOrderRepository | Task 1 | S |
| 3 | Domain exceptions: FundingOrderNotFoundException, FundingFailedException | None | S |
| 4 | DB migration V5 + FundingOrderEntity + JpaRepo + Mapper + Adapter | Task 1, 2 | M |
| 5 | StripeProperties + StripeConfig + StripePaymentAdapter | Task 2 | M |
| 6 | InitiateFundingHandler + FundingController + FundingOrderResponse | Task 2, 4, 5 | M |
| 7 | StripeWebhookController + CompleteFundingHandler | Task 4, 5, 6 | M |
| 8 | TreasuryServiceAdapter: real SPL + SOL transfers | None (can parallel) | L |
| 9 | WalletFundingWorkflow + Activities (5 activities) | Task 7, 8 | L |
| 10 | RefundFundingHandler + refund endpoint | Task 5, 6, 8 | M |
| 11 | Tests: unit + integration + E2E devnet | Task 1-10 | L |
| 12 | Config: .env.example, docker-compose, Makefile stripe-listen | Task 5, 7 | S |

## 14. Out of Scope

- Stripe Customer objects (no persistent customer-to-Stripe mapping)
- Saved payment methods / recurring payments
- KYC/AML verification
- Reconciliation jobs (production concern)
- Kafka event publishing (no Kafka in StablePay)
- Partial refunds (full refund only)
- Multiple currency support (USD only)
- Rate limiting on fund endpoint
