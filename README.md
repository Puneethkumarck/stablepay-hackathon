# StablePay

[![CI](https://github.com/Puneethkumarck/stablepay-hackathon/actions/workflows/ci.yml/badge.svg)](https://github.com/Puneethkumarck/stablepay-hackathon/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://jdk.java.net/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Stripe](https://img.shields.io/badge/Stripe-On--Ramp-635BFF?logo=stripe&logoColor=white)](https://stripe.com/)
[![Solana](https://img.shields.io/badge/Solana-devnet-9945FF?logo=solana&logoColor=white)](https://solana.com/)
[![Anchor](https://img.shields.io/badge/Anchor-0.32.1-blue)](https://www.anchor-lang.com/)
[![Go](https://img.shields.io/badge/Go-1.26-00ADD8?logo=go&logoColor=white)](https://go.dev/)

> **Instant cross-border remittances on Solana. No seed phrases. No app for recipients. Guaranteed delivery.**

StablePay is a consumer-facing remittance application for the **USD → INR** corridor, built on USDC/Solana. It combines MPC wallet abstraction, a custom Anchor escrow program, and Temporal durable workflows to deliver a seamless sender-to-recipient experience — the recipient claims funds via an SMS link, no crypto knowledge required.

> Built for the [Colosseum Frontier Hackathon](https://www.colosseum.org/) (April 6 – May 11, 2026)

---

## The Problem We're Solving

```
Traditional Cross-Border Payment (USD → INR)

  US Sender           Correspondent       Correspondent       Indian
   Bank          →      Bank #1      →      Bank #2      →   Recipient
                                                               Bank
  ─────────────────────────────────────────────────────────────────────
  Day 0                Day 1-2             Day 2-3            Day 3-5

  Cost: 3-5% in fees + hidden FX spreads
  Time: 1-3 business days
```

```
StablePay (USD → INR)

  Sender App      StablePay API      Solana Escrow       Recipient
  (Wallet)   →   (Temporal WF)  →   (USDC PDA)     →   (SMS Claim)
  ─────────────────────────────────────────────────────────────────────
  0 sec            ~30 sec           ~1 min              Claim anytime

  Cost: < $0.01 on-chain fees
  Time: Sub-minute settlement, 48h claim window
```

---

## Architecture Overview

```mermaid
graph TB
    subgraph "Application Layer"
        A[REST API — Spring MVC]
        A1["/api/wallets"]
        A2["/api/remittances"]
        A3["/api/fx"]
        A4["/api/claims"]
        A5["/api/funding-orders"]
        A6["/webhooks/stripe"]
    end

    subgraph "Domain Layer — Zero Framework Dependencies"
        B[Handlers — Use Cases]
        C[Models — Immutable Records]
        D[Ports — Interfaces]
        E[Exceptions — SP-XXXX Codes]
    end

    subgraph "Infrastructure Adapters"
        F[JPA + PostgreSQL + Flyway]
        G[MPC gRPC Client]
        H[Solana RPC — sol4k]
        I[ExchangeRate-API + Redis Cache]
        J[Temporal Workflows]
        K[Twilio SMS]
        L[Razorpay UPI Disbursement]
        S[Stripe Payments]
    end

    subgraph "External Systems"
        M[MPC Sidecar x2 — Go + tss-lib]
        N[Solana Devnet — Anchor Escrow]
        O[open.er-api.com]
        P[Twilio API]
        Q[Razorpay API]
        R[Stripe API]
    end

    A --> B
    B --> D
    D --> F
    D --> G
    D --> H
    D --> I
    D --> J
    D --> K
    D --> L
    D --> S
    G --> M
    H --> N
    I --> O
    K --> P
    L --> Q
    S --> R
```

**Dependency rule:** `domain` → nothing. `application` → `domain`. `infrastructure` → `domain`. Never the reverse.

---

## The Payment Lifecycle: Step by Step

```mermaid
sequenceDiagram
    participant Sender as Sender (Mobile/API)
    participant API as StablePay API
    participant Stripe as Stripe
    participant MPC as MPC Sidecar x2
    participant DB as PostgreSQL
    participant Temporal as Temporal Workflow
    participant Solana as Solana Devnet
    participant SMS as Twilio SMS
    participant Recipient as Recipient (Web)
    participant Razorpay as Razorpay UPI

    Note over Sender,API: Use Case 1 — Create MPC Wallet
    Sender->>API: POST /api/wallets {userId}
    API->>MPC: gRPC GenerateKey (DKG ceremony)
    MPC-->>API: solanaAddress + publicKey + keyShareData
    API->>DB: INSERT wallet
    API-->>Sender: 201 {solanaAddress, balance: 0}

    Note over Sender,Stripe: Use Case 2 — Fund Wallet (Stripe On-Ramp)
    Sender->>API: POST /api/wallets/{id}/fund {amount}
    API->>Stripe: Create PaymentIntent
    Stripe-->>API: clientSecret + paymentIntentId
    API->>DB: INSERT funding_order (PAYMENT_CONFIRMED)
    API-->>Sender: 201 {fundingId, clientSecret}
    Stripe->>API: Webhook: payment_intent.succeeded
    API->>Temporal: Start WalletFundingWorkflow
    Temporal->>Solana: Transfer SOL (rent) + create ATA + transfer USDC
    Temporal->>DB: UPDATE funding_order → FUNDED

    Note over Sender,API: Use Case 3 — Get FX Rate
    Sender->>API: GET /api/fx/USD-INR
    API->>API: Check Redis cache
    API-->>Sender: {rate: 84.50, source, expiresAt}

    Note over Sender,Razorpay: Use Case 4 — Send Remittance
    Sender->>API: POST /api/remittances {senderId, phone, amount}
    API->>DB: Reserve sender balance
    API->>API: Lock FX rate, calculate INR
    API->>DB: INSERT remittance (INITIATED)
    API->>DB: INSERT claim_token (48h expiry)
    API->>Temporal: Start RemittanceLifecycleWorkflow
    API-->>Sender: 201 {remittanceId, status: INITIATED}

    Note over Temporal,Solana: Workflow Phase 1 — Escrow
    Temporal->>MPC: Sign escrow deposit transaction
    MPC-->>Temporal: Ed25519 signature
    Temporal->>Solana: Submit deposit (USDC → PDA vault)
    loop Poll getSignatureStatuses (3s interval, max 40 attempts)
        Temporal->>Solana: Check confirmation status
        Solana-->>Temporal: PROCESSED / CONFIRMED / FINALIZED
    end
    Temporal->>DB: UPDATE status → ESCROWED

    Note over Temporal,SMS: Workflow Phase 2 — Notify
    Temporal->>SMS: Send claim link via SMS
    SMS-->>Recipient: "Claim your funds: https://..."

    Note over Temporal,Recipient: Workflow Phase 3 — Wait
    Temporal->>Temporal: Await claim signal (48h timeout)

    Note over Recipient,Razorpay: Use Case 5 — Claim Funds
    Recipient->>API: GET /api/claims/{token}
    API-->>Recipient: {amountUsdc, amountInr, fxRate}
    Recipient->>API: POST /api/claims/{token} {upiId}
    API->>DB: UPDATE claim_token (claimed=true, upiId)
    API->>Temporal: Signal claimSubmitted(upiId)

    Note over Temporal,Razorpay: Workflow Phase 4 — Deliver
    Temporal->>Solana: Release escrow to recipient
    loop Poll getSignatureStatuses (3s interval, max 40 attempts)
        Temporal->>Solana: Check confirmation status
    end
    Temporal->>DB: UPDATE status → CLAIMED
    Temporal->>Razorpay: Disburse INR to UPI
    Temporal->>DB: UPDATE status → DELIVERED
```

---

## Use Case 1: Create MPC Wallet

> **No seed phrases.** A 2-of-2 threshold key generation ceremony produces an Ed25519 Solana wallet. The full private key never exists in memory.

```mermaid
sequenceDiagram
    participant Client
    participant Controller as WalletController
    participant Handler as CreateWalletHandler
    participant Repo as WalletRepository
    participant MPC as MpcWalletGrpcClient
    participant Sidecar0 as MPC Sidecar 0
    participant Sidecar1 as MPC Sidecar 1

    Client->>Controller: POST /api/wallets {userId: "user-123"}

    Controller->>Handler: handle("user-123")
    Handler->>Repo: findByUserId("user-123")
    Repo-->>Handler: Optional.empty()

    Handler->>MPC: generateKey()
    MPC->>Sidecar0: gRPC GenerateKey (ceremonyId, threshold=1, parties=2)
    Sidecar0->>Sidecar1: P2P DKG round messages (port 7000↔7001)
    Sidecar1->>Sidecar0: P2P DKG round messages
    Note over Sidecar0,Sidecar1: Ed25519 DKG ceremony completes
    Sidecar0-->>MPC: {solanaAddress, publicKey, keyShareData}
    MPC-->>Handler: GeneratedKey

    Handler->>Handler: Build Wallet(userId, solanaAddress, balance=0)
    Handler->>Repo: save(wallet)
    Repo-->>Handler: Wallet with generated id

    Handler-->>Controller: Wallet
    Controller-->>Client: 201 Created
```

```
POST /api/wallets
Content-Type: application/json

{ "userId": "user-123" }
```

```json
{
  "id": 1,
  "userId": "user-123",
  "solanaAddress": "7xK...abc",
  "availableBalance": 0.000000,
  "totalBalance": 0.000000,
  "createdAt": "2026-04-13T10:00:00Z",
  "updatedAt": "2026-04-13T10:00:00Z"
}
```

### What Happens Inside the MPC Sidecars

```
┌─────────────────────────────────────────────────────────────────┐
│                   MPC Key Generation (DKG)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Backend sends gRPC GenerateKey to Sidecar 0 (port 50051)    │
│                                                                  │
│  2. Sidecar 0 registers ceremony in P2P CeremonyRegistry        │
│     └─ Creates buffered channel for round messages               │
│                                                                  │
│  3. Both sidecars run tss-lib Ed25519 DKG protocol               │
│     ├─ Round 1: Commitment exchange (P2P port 7000↔7001)         │
│     ├─ Round 2: Share distribution                               │
│     └─ Round 3: Key derivation                                   │
│                                                                  │
│  4. Result: Both parties hold a key share                        │
│     ├─ Neither party has the full private key                    │
│     ├─ Public key (Ed25519) derived cooperatively                │
│     └─ Solana address = Base58(publicKey)                        │
│                                                                  │
│  5. Sidecar 0 returns to backend:                                │
│     ├─ solanaAddress: Base58-encoded Solana address              │
│     ├─ publicKey: Raw Ed25519 public key bytes                   │
│     └─ keyShareData: Serialized key share (stored in DB)         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Error Paths

| Condition | Error Code | HTTP |
|---|---|---|
| User already has a wallet | SP-0008 | 409 Conflict |
| MPC ceremony fails | SP-0010 | 500 |
| gRPC timeout (>30s) | SP-0010 | 500 |

---

## Use Case 2: Fund Wallet (Stripe On-Ramp)

> **Real Stripe integration.** The sender pays via Stripe (card), which triggers a webhook. A Temporal workflow then transfers SOL (for rent/fees), creates an Associated Token Account, and transfers USDC from the treasury to the sender's MPC wallet on-chain.

```mermaid
sequenceDiagram
    participant Client
    participant Controller as FundingController
    participant Handler as InitiateFundingHandler
    participant Writer as FundingOrderWriter
    participant Stripe as StripePaymentAdapter
    participant DB as PostgreSQL
    participant Webhook as StripeWebhookController
    participant Complete as CompleteFundingHandler
    participant Temporal as WalletFundingWorkflow
    participant Treasury as TreasuryServiceAdapter
    participant Solana as Solana Devnet

    Client->>Controller: POST /api/wallets/1/fund {amount: 1.00}
    Controller->>Writer: persist funding order
    Writer->>DB: INSERT funding_order (PAYMENT_CONFIRMED)
    Controller->>Handler: initiate funding
    Handler->>Stripe: Create PaymentIntent ($1.00)
    Stripe-->>Handler: paymentIntentId + clientSecret
    Handler-->>Client: 201 {fundingId, clientSecret}

    Note over Stripe,Webhook: Stripe fires webhook
    Stripe->>Webhook: POST /webhooks/stripe (payment_intent.succeeded)
    Webhook->>Complete: handle(fundingId)
    Complete->>Temporal: Start WalletFundingWorkflow

    Note over Temporal,Solana: Temporal orchestrates on-chain funding
    Temporal->>Treasury: checkTreasuryBalance(1.00 USDC)
    Temporal->>Treasury: ensureSolBalance(senderAddress)
    Treasury->>Solana: Transfer SOL for rent + fees
    Temporal->>Treasury: createAtaIfNeeded(senderAddress)
    Treasury->>Solana: Create Associated Token Account
    Temporal->>Treasury: transferUsdc(senderAddress, 1.00)
    Treasury->>Solana: SPL token transfer (treasury → sender ATA)
    Temporal->>DB: UPDATE funding_order → FUNDED
```

```
POST /api/wallets/1/fund
Content-Type: application/json

{ "amount": 1.00 }
```

```json
{
  "fundingId": "b2c8a29a-fcca-487f-979b-bf355c82faeb",
  "stripePaymentIntentId": "pi_3TO0YZ3nnME1dfOB0dz1SICr",
  "stripeClientSecret": "pi_...secret_...",
  "walletId": 1,
  "amountUsdc": 1.00,
  "status": "PAYMENT_CONFIRMED"
}
```

### Funding Order Status Machine

```
PAYMENT_CONFIRMED → FUNDED     (webhook + Temporal workflow succeeds)
PAYMENT_CONFIRMED → FAILED     (webhook: payment_intent.payment_failed)
FUNDED → REFUND_INITIATED      (manual refund)
REFUND_INITIATED → REFUNDED    (refund completed)
```

### Error Paths

| Condition | Error Code | HTTP |
|---|---|---|
| Wallet not found | SP-0006 | 404 |
| Treasury balance insufficient | SP-0007 | 503 |
| Funding already in progress for wallet | SP-0022 | 409 |
| Stripe PaymentIntent creation failed | SP-0021 | 502 |

---

## Use Case 3: Get FX Rate

> **Real-time rates with fallback.** FX rates come from ExchangeRate-API with Redis caching (5-minute TTL). If the API is unreachable, a hardcoded fallback rate of 84.50 is used.

```mermaid
sequenceDiagram
    participant Client
    participant Controller as FxRateController
    participant Handler as GetFxRateQueryHandler
    participant Adapter as ExchangeRateApiAdapter
    participant Redis as Redis Cache
    participant API as open.er-api.com

    Client->>Controller: GET /api/fx/USD-INR
    Controller->>Handler: handle("USD", "INR")
    Handler->>Adapter: getRate("USD", "INR")

    alt Cache Hit
        Adapter->>Redis: GET fxRate::USD
        Redis-->>Adapter: Cached FxQuote
    else Cache Miss
        Adapter->>API: GET /v6/latest/USD
        API-->>Adapter: {rates: {INR: 84.50, ...}}
        Adapter->>Redis: SET fxRate::USD (5m TTL)
    else API Failure
        Note over Adapter: Fallback: rate=84.50, source="fallback"
    end

    Adapter-->>Handler: FxQuote{rate, source, timestamp, expiresAt}
    Handler-->>Controller: FxQuote
    Controller-->>Client: 200 OK
```

```
GET /api/fx/USD-INR
```

```json
{
  "rate": 84.500000,
  "source": "open.er-api.com",
  "timestamp": "2026-04-13T10:00:00Z",
  "expiresAt": "2026-04-13T10:01:00Z"
}
```

### Error Paths

| Condition | Error Code | HTTP |
|---|---|---|
| Unsupported corridor (e.g., EUR-INR) | SP-0009 | 400 |

---

## Use Case 4: Send Remittance

> **The core flow.** Reserves the sender's balance, locks the FX rate, generates a claim token, and starts a Temporal durable workflow that orchestrates the entire escrow-to-delivery lifecycle.

```mermaid
sequenceDiagram
    participant Client
    participant Handler as CreateRemittanceHandler
    participant WalletRepo as WalletRepository
    participant FxProvider as ExchangeRateApiAdapter
    participant RemitRepo as RemittanceRepository
    participant ClaimRepo as ClaimTokenRepository
    participant Temporal as TemporalWorkflowStarter
    participant DB as PostgreSQL

    Client->>Handler: handle("user-123", "+919876543210", 100.00)

    Note over Handler,DB: Step 1 — Reserve Balance
    Handler->>WalletRepo: findByUserId("user-123")
    WalletRepo-->>Handler: Wallet{available: 100.00}
    Handler->>Handler: wallet.reserveBalance(100.00)
    Note over Handler: available: 100→0, total: 100 (unchanged)
    Handler->>WalletRepo: save(reserved wallet)

    Note over Handler,FxProvider: Step 2 — Lock FX Rate
    Handler->>FxProvider: getRate("USD", "INR")
    FxProvider-->>Handler: FxQuote{rate: 84.50}
    Handler->>Handler: INR = 100.00 × 84.50 = ₹8,450.00

    Note over Handler,DB: Step 3 — Create Remittance
    Handler->>Handler: remittanceId = UUID.randomUUID()
    Handler->>RemitRepo: save(Remittance{status: INITIATED})
    RemitRepo->>DB: INSERT INTO remittances

    Note over Handler,DB: Step 4 — Generate Claim Token
    Handler->>Handler: token = UUID.randomUUID()
    Handler->>ClaimRepo: save(ClaimToken{expires: now+48h})
    ClaimRepo->>DB: INSERT INTO claim_tokens
    Handler->>RemitRepo: save(remittance + claimTokenId)

    Note over Handler,Temporal: Step 5 — Start Workflow
    Handler->>Temporal: startWorkflow(remittanceId, senderAddr, phone, amount, token)
    Temporal-->>Handler: Workflow started (async)

    Handler-->>Client: 201 Created
```

```
POST /api/remittances
Content-Type: application/json

{
  "senderId": "user-123",
  "recipientPhone": "+919876543210",
  "amountUsdc": 100.00
}
```

```json
{
  "remittanceId": "550e8400-e29b-41d4-a716-446655440000",
  "senderId": "user-123",
  "recipientPhone": "+919876543210",
  "amountUsdc": 100.000000,
  "amountInr": 8450.00,
  "fxRate": 84.500000,
  "status": "INITIATED",
  "claimTokenId": "a1b2c3d4-token-uuid",
  "smsNotificationFailed": false
}
```

### What Happens After the API Returns

The Temporal workflow takes over asynchronously. The sender gets an immediate response, and the workflow progresses through the escrow lifecycle in the background.

### Error Paths

| Condition | Error Code | HTTP |
|---|---|---|
| Sender wallet not found | SP-0006 | 404 |
| Insufficient balance | SP-0002 | 400 |
| Unsupported corridor | SP-0009 | 400 |

---

## Temporal Workflow: The Remittance Lifecycle

> **Guaranteed delivery.** If the process crashes at any point, Temporal resumes exactly where it left off. Every remittance reaches a terminal state — delivered, refunded, or failed.

```mermaid
stateDiagram-v2
    [*] --> INITIATED: POST /api/remittances
    INITIATED --> ESCROWED: depositEscrow activity succeeds

    ESCROWED --> CLAIMED: Recipient claims within 48h
    ESCROWED --> REFUNDED: 48h timeout — no claim

    CLAIMED --> DELIVERED: INR disbursement succeeds
    CLAIMED --> DISBURSEMENT_FAILED: INR disbursement fails

    INITIATED --> FAILED: Deposit escrow fails

    note right of ESCROWED
        USDC locked in Solana PDA
        SMS sent to recipient
        Workflow awaits claim signal
    end note

    note right of DELIVERED
        Escrow released on-chain
        INR sent to UPI
        Terminal state
    end note

    note right of REFUNDED
        USDC returned to sender
        Escrow closed on-chain
        Terminal state
    end note

    note right of DISBURSEMENT_FAILED
        Escrow released (irreversible)
        INR payout failed
        Requires manual resolution
    end note
```

### Workflow Activities — In Execution Order

```
┌─────────────────────────────────────────────────────────────────┐
│              RemittanceLifecycleWorkflow.execute()                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Phase 1: ESCROW DEPOSIT                                         │
│  ├─ Activity: depositEscrow (60s timeout, 3 retries, 2s backoff)│
│  │   ├─ Fetch wallet's keyShareData from DB                     │
│  │   ├─ Build Solana escrow deposit instruction                  │
│  │   ├─ MPC-sign transaction (gRPC → sidecar)                   │
│  │   └─ Submit to Solana devnet → returns signature              │
│  ├─ Poll: awaitTransactionConfirmation(signature)                │
│  │   ├─ Calls getSignatureStatuses RPC every 3 seconds           │
│  │   ├─ Max 40 attempts (~120s total polling window)             │
│  │   ├─ Accepts CONFIRMED or FINALIZED as success                │
│  │   └─ Throws SP-0012 on timeout, SP-0031 on on-chain failure  │
│  └─ Status Update: INITIATED → ESCROWED                         │
│                                                                  │
│  Phase 2: SMS NOTIFICATION                                       │
│  ├─ Activity: sendClaimSms (30s timeout, 3 retries, 5s backoff) │
│  │   ├─ Build claim URL: {claimBaseUrl}/{claimToken}             │
│  │   └─ Send via Twilio (or log in dev mode)                     │
│  └─ On failure: set smsNotificationFailed=true, continue         │
│                                                                  │
│  Phase 3: AWAIT CLAIM                                            │
│  ├─ Workflow.await(48 hours, () -> claimReceived)                │
│  │                                                               │
│  │   ┌─ PATH A: Claim signal received ──────────────────────┐   │
│  │   │  Activity: releaseEscrow (60s, 3 retries, 2s backoff)│   │
│  │   │  Poll: awaitTransactionConfirmation (3s × 40 max)     │   │
│  │   │  Status Update: ESCROWED → CLAIMED                    │   │
│  │   │  Activity: disburseInr (45s, NO retry) via Razorpay   │   │
│  │   │  ├─ Success: Status → DELIVERED                       │   │
│  │   │  └─ Failure: Status → DISBURSEMENT_FAILED             │   │
│  │   └──────────────────────────────────────────────────────┘   │
│  │                                                               │
│  │   ┌─ PATH B: 48h timeout — no claim ────────────────────┐   │
│  │   │  Activity: refundEscrow (60s, 3 retries, 2s backoff) │   │
│  │   │  Poll: awaitTransactionConfirmation (3s × 40 max)     │   │
│  │   │  Status Update: ESCROWED → REFUNDED                   │   │
│  │   └──────────────────────────────────────────────────────┘   │
│  └                                                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Re-Sign on Retry

Solana blockhashes expire in ~60 seconds. If a deposit or release fails and retries, the workflow requests a **fresh MPC signature** with a new blockhash — it never replays a stale transaction.

### Disbursement Does Not Retry

Once escrow is released on-chain, the USDC is gone. If the INR disbursement fails after release, retrying could cause duplicate payouts. The workflow marks the status as `DISBURSEMENT_FAILED` for manual resolution.

---

## Use Case 5: Claim Funds (Recipient)

> **No app required.** The recipient opens an SMS link, sees how much they'll receive in INR, enters their UPI ID, and submits. The Temporal workflow wakes up and completes the delivery.

### Step 1: View Claim Details

```mermaid
sequenceDiagram
    participant Recipient
    participant API as ClaimController
    participant Handler as GetClaimQueryHandler
    participant ClaimRepo as ClaimTokenRepository
    participant RemitRepo as RemittanceRepository

    Recipient->>API: GET /api/claims/{token}
    API->>Handler: handle(token)
    Handler->>ClaimRepo: findByToken(token)
    ClaimRepo-->>Handler: ClaimToken{remittanceId, expiresAt}
    Handler->>RemitRepo: findByRemittanceId(remittanceId)
    RemitRepo-->>Handler: Remittance{amountUsdc: 100, amountInr: 8450, fxRate: 84.50}
    Handler-->>API: ClaimDetails
    API-->>Recipient: 200 OK
```

```
GET /api/claims/a1b2c3d4-token-uuid
```

### Step 2: Submit Claim with UPI ID

```mermaid
sequenceDiagram
    participant Recipient
    participant API as ClaimController
    participant Handler as SubmitClaimHandler
    participant ClaimRepo as ClaimTokenRepository
    participant RemitRepo as RemittanceRepository
    participant Signaler as TemporalClaimSignaler
    participant Workflow as RemittanceLifecycleWorkflow

    Recipient->>API: POST /api/claims/{token} {upiId: "raj@upi"}
    API->>Handler: handle(token, "raj@upi")

    Note over Handler: Validation Chain
    Handler->>ClaimRepo: findByToken(token)
    Handler->>Handler: ✓ Token exists (else SP-0011)
    Handler->>Handler: ✓ Not already claimed (else SP-0012)
    Handler->>Handler: ✓ Not expired (else SP-0013)
    Handler->>RemitRepo: findByRemittanceId(...)
    Handler->>Handler: ✓ Remittance exists (else SP-0010)
    Handler->>Handler: ✓ Status == ESCROWED (else SP-0014)

    Handler->>ClaimRepo: save(claimed=true, upiId="raj@upi")
    Handler->>Signaler: signalClaim(remittanceId, token, "raj@upi")

    Signaler->>Signaler: Resolve claim authority Solana address
    Signaler->>Workflow: claimSubmitted(ClaimSignal)
    Note over Workflow: claimReceived=true → unblocks await

    Handler-->>API: ClaimDetails
    API-->>Recipient: 200 OK
```

```
POST /api/claims/a1b2c3d4-token-uuid
Content-Type: application/json

{ "upiId": "raj@upi" }
```

### After the Signal: Workflow Completes Delivery

```
┌──────────────────────────────────────────────────────────────┐
│         What Happens After claimSubmitted Signal               │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  1. Workflow.await() unblocks (claimReceived = true)           │
│                                                                │
│  2. releaseEscrow activity                                     │
│     ├─ Build Solana claim instruction                          │
│     ├─ Transfer USDC from escrow PDA → destination address     │
│     ├─ Close vault token account (reclaim rent)                │
│     └─ Escrow status on-chain: Active → Claimed                │
│                                                                │
│  3. Status update: ESCROWED → CLAIMED                          │
│                                                                │
│  4. disburseInr activity                                       │
│     ├─ Call Razorpay Payout API                                │
│     ├─ Transfer INR to recipient's UPI ID                      │
│     └─ INR credited to recipient's bank via UPI                │
│                                                                │
│  5. Status update: CLAIMED → DELIVERED ✓                       │
│                                                                │
└──────────────────────────────────────────────────────────────┘
```

### Claim Validation Rules

| # | Check | Fails With | HTTP |
|---|---|---|---|
| 1 | Token exists in database | SP-0011 | 404 |
| 2 | Token not already claimed | SP-0012 | 409 |
| 3 | Token not expired (48h window) | SP-0013 | 410 |
| 4 | Remittance exists | SP-0010 | 404 |
| 5 | Remittance status is ESCROWED | SP-0014 | 409 |

---

## On-Chain Escrow Program

**Program ID:** `7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij`

Custom Anchor program managing USDC escrow on Solana devnet.

```mermaid
stateDiagram-v2
    [*] --> Active: deposit(amount, deadline)
    Active --> Claimed: claim() — by claim authority
    Active --> Refunded: refund() — after deadline
    Active --> Cancelled: cancel() — by sender

    note right of Active
        USDC locked in PDA vault
        seeds: ["escrow", remittance_id]
    end note
```

### Instructions

| Instruction | Caller | What It Does |
|---|---|---|
| `deposit(amount, deadline)` | Sender (MPC-signed) | Create escrow PDA, transfer USDC to vault, set 48h deadline |
| `claim()` | Backend (claim authority) | Transfer vault USDC to recipient, close accounts |
| `refund()` | Anyone (after deadline) | Return vault USDC to sender, close accounts |
| `cancel()` | Sender only | Return USDC before claim, close accounts |

### Escrow Account

```rust
pub struct Escrow {
    pub sender: Pubkey,           // Sender wallet (MPC-derived)
    pub claim_authority: Pubkey,  // Backend authority for claim
    pub mint: Pubkey,             // USDC mint address
    pub amount: u64,              // Locked amount (6 decimals)
    pub deadline: i64,            // Unix timestamp for refund eligibility
    pub status: EscrowStatus,     // Active | Claimed | Refunded | Cancelled
    pub bump: u8,                 // Canonical PDA bump
    pub remittance_id: Pubkey,    // Links on-chain to off-chain
}
```

### PDA Derivation

| Account | Seeds |
|---|---|
| Escrow | `["escrow", remittance_id]` |
| Vault | `["vault", escrow_pubkey]` |

---

## 🏦 Solana Accounting Model: Where Every Dollar Goes

> A visual walkthrough of every wallet, PDA, and token account involved in a $25 remittance — from program deployment to the recipient receiving INR in their bank.

### 🗝️ The Players

On Solana, there are different kinds of accounts. Here's what each one is, with a real-world analogy:

| | Solana Concept | Real-World Analogy | What It Does |
|---|---|---|---|
| 🔑 | **Wallet** (System Account) | Your **physical wallet** with cash | Holds SOL for tx fees. Has a private key you sign with. Cannot hold tokens directly — needs a Token Account for that. |
| 🪙 | **Token Account** (ATA) | A **bank account** for one specific currency | Holds USDC (or any SPL token) on behalf of a wallet. One per currency — you have a separate USDC account, BONK account, etc. Address auto-derived from your wallet + token type. |
| 📦 | **PDA** (Program Derived Address) | A **locked safety deposit box** at the bank | No one has the key — only the program's rules can open it. "Release funds when the authority says OK." Used for escrow vaults and trustless custody. |
| 🏭 | **Mint** | The **US Treasury** / central bank that prints money | Defines a token type (like USDC). Has a "mint authority" — the only entity that can create new tokens. Circle is the mint authority for real USDC. |
| ⚙️ | **Program** | The **rulebook** at an escrow company | Code deployed once to Solana, reused forever by all transactions. "Hold the buyer's money. Release when conditions met. Refund if timeout." Our escrow program handles every remittance with the same rules. |

```
Think of the whole system like this:

  👤 You (wallet)  ──►  🏦 Your bank account (ATA)  ──►  📦 Escrow company (PDA)
      │                         │                              │
      has SOL                   has USDC                       has locked USDC
      (cash for fees)           (your balance)                 (held until rules say release)
      │                         │                              │
      signs with                managed by                     controlled by
      private key               Token Program                  Escrow Program
```

Now the actual wallets involved in StablePay:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        STABLEPAY WALLETS                                 │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  🏛️ DEPLOYER WALLET (one-time setup)                                    │
│  ├─ Address: 58gFSCTW...                                                 │
│  ├─ Role: Deploys the escrow program to Solana (one-time operation)      │
│  ├─ Becomes the "upgrade authority" — can update the program later       │
│  ├─ Pays ~2 SOL for program storage rent                                 │
│  ├─ In production: would be a multisig for security                      │
│  └─ ⚠️ NOT involved in any remittance transaction after deployment       │
│                                                                          │
│  👤 SENDER MPC WALLET (one per user)                                     │
│  ├─ Address: DQoGcVse...  (Ed25519 — no one holds the full private key) │
│  ├─ Created via MPC DKG — key split across 2 sidecars                    │
│  ├─ SOL balance: pays for deposit tx fees + account rent                 │
│  └─ Token Account (ATA): GuDuFKeX... — holds the user's USDC            │
│                                                                          │
│  🔐 CLAIM AUTHORITY (one per StablePay deployment)                       │
│  ├─ Address: 3LZh792t...  (backend-controlled keypair)                   │
│  ├─ The ONLY key that can release USDC from escrow (via claim)           │
│  ├─ Stored as CLAIM_AUTHORITY_PRIVATE_KEY in .env                        │
│  ├─ Token Account (ATA): 2KKehH5e... — receives USDC when claims happen │
│  ├─ Pays tx fees for claim/refund operations                             │
│  └─ In production: would be a multisig or HSM-backed key                 │
│                                                                          │
│  📦 ESCROW PDA + 🏦 VAULT PDA (one pair per remittance)                  │
│  ├─ Escrow: stores metadata (sender, amount, deadline, status)           │
│  ├─ Vault: SPL Token Account that holds the locked USDC                  │
│  ├─ Both created on deposit, both closed on claim/refund                 │
│  ├─ Rent SOL always returned to sender when accounts close               │
│  └─ No private key exists — only the escrow program can move funds       │
│                                                                          │
│  🏭 USDC MINT                                                            │
│  ├─ Devnet: Circle's USDC (4zMMC9sr...) or test mint (CAUBK3cr...)      │
│  ├─ Mint authority: whoever created the mint (Circle, or us for tests)   │
│  └─ We use a test mint for E2E testing because we can't mint real USDC   │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 📖 The Story of a $25 Remittance

#### Act 0: 🚀 Program Deployment (one-time — NOT per transaction)

> The escrow program is deployed **once** to Solana. It's like publishing a smart contract. Every remittance reuses the same program code — only the data accounts (escrow + vault) are created per transaction.

```
  🏛️ Deployer Wallet                        Solana Network
  58gFSCTW...
  ┌─────────────┐     anchor deploy         ┌─────────────────────────┐
  │ SOL: 5.0    │──────────────────────────►│ ⚙️ Program Account       │
  │             │     ~2 SOL for rent       │ 7C2zsbhg...             │
  │ Upgrade     │                           │ Code: deposit, claim,   │
  │ Authority 🔑│                           │       refund, cancel    │
  └─────────────┘                           │ Size: ~285 KB           │
                                            │ Owner: BPF Loader       │
       After deployment:                    └─────────────────────────┘
       Deployer SOL: 5.0 → 3.0
       Program lives on-chain permanently    ✅ Deployed once, used forever
       Deployer is NOT involved in any
       remittance transaction after this
```

**In production:**
- Deploy once to mainnet (costs ~2 SOL ≈ $300 at current prices)
- Transfer upgrade authority to a multisig
- Program is immutable after authority is revoked (optional)

#### Act 1: 🔑 Wallet Creation (MPC DKG)

> A sender signs up and gets a Solana wallet. But unlike MetaMask, **no one ever sees a seed phrase**. The private key is split across two MPC sidecars using a Distributed Key Generation ceremony.

```
  Backend                MPC Sidecar 0          MPC Sidecar 1
  ┌──────────┐          ┌──────────┐          ┌──────────┐
  │ POST     │─ gRPC ──►│ Party 0  │◄════════►│ Party 1  │
  │ /api/    │          │ partyId=0│  P2P DKG │ partyId=1│
  │ wallets  │          │          │  rounds   │          │
  └──────────┘          └────┬─────┘  7000↔7001└────┬─────┘
                             │                      │
                             ▼                      ▼
                        Key Share 0            Key Share 1
                        (primary)              (peer)
                             │                      │
                             └──────┬───────────────┘
                                    ▼
                            ┌──────────────┐
                            │  PostgreSQL   │
                            │  wallets table│
                            │              │
                            │ key_share_data│ ◄── Party 0's share
                            │ peer_key_     │
                            │ share_data    │ ◄── Party 1's share
                            │ solana_address│ ◄── DQoGcVse...
                            └──────────────┘

  🔒 The full Ed25519 private key NEVER exists anywhere
  🔒 Each sidecar only sees its own share during the ceremony
  🔒 Key shares persist in DB — survive app restarts
  🔒 Both sidecars needed to sign (2-of-2 threshold)
```

**What's created on Solana:** Nothing! A Solana "wallet" is just a keypair. The address `DQoGcVse...` exists mathematically but has no on-chain account yet. It becomes a real account when someone sends SOL to it.

#### Act 2: 💰 Funding the Wallet

> Before the sender can make a remittance, their MPC wallet needs SOL (for transaction fees) and USDC (the stablecoin to send). The sender pays via Stripe (card payment), which triggers a Temporal workflow that transfers SOL and USDC from a pre-funded treasury on Solana devnet.

```
  🏛️ Deployer / Treasury               👤 Sender MPC Wallet
  58gFSCTW...                           DQoGcVse...

  Step 1: Send SOL for tx fees
  ┌─────────────┐                       ┌─────────────┐
  │ SOL: 5.0    │───── 1 SOL ─────────►│ SOL: 1.0    │
  └─────────────┘                       └─────────────┘
  (This creates the sender's system account on-chain!)


  Step 2: Create sender's USDC token account (ATA)
  ┌────────────────────────────────────────────────────────────────┐
  │  spl-token create-account USDC_MINT --owner DQoGcVse...       │
  │                                                                │
  │  Derives ATA address: GuDuFKeX... = PDA([DQoGcVse, TOKEN, MINT])│
  │  Creates a new SPL Token Account on-chain                      │
  │  Owner: DQoGcVse... (the MPC wallet)                           │
  │  Mint: USDC                                                    │
  │  Balance: 0                                                    │
  └────────────────────────────────────────────────────────────────┘


  Step 3: Mint/transfer USDC to sender
  🏭 USDC Mint                          Sender ATA (GuDuFKeX...)
  ┌─────────────┐                       ┌─────────────┐
  │ Mint Auth:  │──── 100 USDC ────────►│ USDC: 100   │
  │ (deployer   │   (mint or transfer)  │ Owner: DQo..│
  │  for test)  │                       │ Mint: USDC  │
  └─────────────┘                       └─────────────┘

  Mint authority can create tokens out of thin air (test only!)
  In production: USDC comes from Circle via Stripe on-ramp
```

**After funding:**

| Account | SOL | USDC | Notes |
|---------|-----|------|-------|
| 👤 Sender wallet `DQoGcVse...` | 1.0 | — | System account (holds SOL) |
| 🪙 Sender ATA `GuDuFKeX...` | — | 100 | Token account (holds USDC) |
| 🏛️ Deployer `58gFSCTW...` | 3.0 | — | Paid for program + funding |

#### Act 3: 📤 Escrow Deposit ($25 USDC)

> The sender sends $25 to India. The Temporal workflow kicks off and deposits USDC into an on-chain escrow.

```
  🔐 MPC Signing Ceremony
  ┌─────────────────────────────────────────────────┐
  │  Backend builds deposit instruction              │
  │  ├─ 9 accounts: sender, escrow, vault, ATA...   │
  │  ├─ Data: amount=25,000,000 + deadline           │
  │                                                   │
  │  Sidecar 0 + Sidecar 1 co-sign (2-of-2)         │
  │  ├─ Each uses their key share from DB             │
  │  ├─ P2P signing rounds over port 7000↔7001       │
  │  └─ Result: 64-byte Ed25519 signature             │
  │                                                   │
  │  Backend builds raw tx: sig_count(1) + sig + msg  │
  └─────────────────────────────────────────────────┘
```

```
  BEFORE DEPOSIT                          AFTER DEPOSIT
  ══════════════                          ═════════════

  Sender ATA (GuDuFKeX...)                Sender ATA (GuDuFKeX...)
  ┌─────────────┐                         ┌─────────────┐
  │ USDC: 100   │                         │ USDC: 75    │  -$25 ✅
  └─────────────┘                         └─────────────┘

  Escrow PDA                              Escrow PDA (NEW!)
  ┌─────────────┐                         ┌─────────────┐
  │ (does not   │                         │ sender: DQo.│
  │  exist)     │                         │ amount: 25M │
  └─────────────┘                         │ deadline:48h│
                                          │ status: ✅   │
                                          │ Active      │
                                          └─────────────┘

  Vault PDA                               Vault PDA (NEW!)
  ┌─────────────┐                         ┌─────────────┐
  │ (does not   │                         │ USDC: 25    │  Locked! 🔒
  │  exist)     │                         │ auth: escrow│
  └─────────────┘                         └─────────────┘

  Sender SOL                              Sender SOL
  ┌─────────────┐                         ┌─────────────┐
  │ SOL: 1.000  │                         │ SOL: 0.992  │  -0.008 (rent+fee)
  └─────────────┘                         └─────────────┘
```

**On-chain transaction:** [Finalized ✅](https://explorer.solana.com/?cluster=devnet)
- 🔒 25 USDC locked in vault PDA (only the escrow program can move it)
- 📋 Escrow PDA stores: sender, claim_authority, mint, amount, deadline
- 💸 Sender paid ~0.008 SOL for account rent + tx fee

#### Act 4: 📱 SMS Notification

```
  Temporal Workflow                     Recipient's Phone
  ┌──────────────┐                     ┌──────────────────┐
  │ sendClaimSms │────── SMS ─────────►│ 📱 "You have a   │
  │ activity     │   (via Twilio)      │ StablePay         │
  └──────────────┘                     │ remittance!       │
                                       │ Claim: https://..."│
                                       └──────────────────┘
  
  Workflow now waits ⏳ (up to 48 hours for claim signal)
```

#### Act 5: ✋ Recipient Claims

> The recipient opens the SMS link, sees ₹2,336 (at 93.44 rate), enters UPI ID, and submits.

```
  POST /api/claims/{token}  { "upiId": "raj@upi" }
  
  ┌───────────────────────────────────────────────────────┐
  │  SubmitClaimHandler                                    │
  │  ├─ ✅ Token exists                                    │
  │  ├─ ✅ Not already claimed                             │
  │  ├─ ✅ Not expired (within 48h)                        │
  │  ├─ ✅ Remittance status == ESCROWED                   │
  │  └─ Signal Temporal workflow: claimSubmitted!           │
  └───────────────────────────────────────────────────────┘

  TemporalRemittanceClaimSignaler:
  ├─ Derives claim authority ATA: PublicKey.findProgramDerivedAddress(
  │      claimAuthority, usdcMint) → 2KKehH5e...
  └─ Sends ClaimSignal to workflow (wakes it up!)
```

#### Act 6: 💸 Escrow Release (Claim Transaction)

> The Temporal workflow wakes up and submits the claim transaction on-chain.

```
  🔐 Claim Authority signs (NOT MPC — this is the backend's own keypair)

  Claim Transaction (single instruction, 6 accounts):
  ┌──────────────────────────────────────────────────────────────┐
  │  Account 0: 🔐 Claim Authority (signer)     3LZh792t...    │
  │  Account 1: 📦 Escrow PDA (mut, close)      4f5fxvV4...    │
  │  Account 2: 🏦 Vault PDA (mut)              FaRgcuRb...    │
  │  Account 3: 💰 Recipient Token ATA (mut)    2KKehH5e...    │
  │  Account 4: 👤 Sender wallet (mut)          DQoGcVse...    │
  │  Account 5: ⚙️  Token Program               TokenkegQ...   │
  └──────────────────────────────────────────────────────────────┘
```

```
  BEFORE CLAIM                            AFTER CLAIM
  ════════════                            ═══════════

  Vault PDA (FaRgcuRb...)                 Vault PDA
  ┌─────────────┐                         ┌─────────────┐
  │ USDC: 25    │────── 25 USDC ─────────►│ CLOSED ❌    │  Rent → sender
  └─────────────┘                         └─────────────┘
                          │
                          ▼
  Claim Auth ATA (2KKehH5e...)            Claim Auth ATA (2KKehH5e...)
  ┌─────────────┐                         ┌─────────────┐
  │ USDC: 0     │                         │ USDC: 25    │  +$25 ✅
  └─────────────┘                         └─────────────┘

  Escrow PDA (4f5fxvV4...)                Escrow PDA
  ┌─────────────┐                         ┌─────────────┐
  │ status:     │                         │ CLOSED ❌    │  Rent → sender
  │ Active      │                         └─────────────┘
  └─────────────┘

  Sender SOL (DQoGcVse...)                Sender SOL (DQoGcVse...)
  ┌─────────────┐                         ┌─────────────┐
  │ SOL: 0.992  │                         │ SOL: 0.996  │  +0.004 (rent back!)
  └─────────────┘                         └─────────────┘

  Claim Auth SOL (3LZh792t...)            Claim Auth SOL (3LZh792t...)
  ┌─────────────┐                         ┌─────────────┐
  │ SOL: 5.000  │                         │ SOL: 4.999  │  -0.001 (tx fee)
  └─────────────┘                         └─────────────┘
```

**What happens on-chain (2 CPI calls inside the escrow program):**
1. 🔄 **Transfer**: Vault PDA → Claim Authority ATA (25 USDC)
2. 🗑️ **Close vault**: Account deleted, rent SOL → sender
3. 🗑️ **Close escrow**: Account deleted, rent SOL → sender

**On-chain transaction:** [Finalized ✅](https://explorer.solana.com/?cluster=devnet)

#### Act 7: 🏦 INR Disbursement

```
  Temporal Workflow                     Razorpay API
  ┌──────────────┐                     ┌──────────────────┐
  │ disburseInr  │────── API call ────►│ Create Payout    │
  │ activity     │                     │ Send ₹2,336 to   │
  └──────────────┘                     │ raj@upi via UPI  │
                                       └──────────────────┘
  Status: CLAIMED → DELIVERED ✅
```

### 📊 Final Ledger

```
  ┌──────────────────────────────────────────────────────────────┐
  │                   FINAL STATE ($25 remittance)                │
  ├──────────────────────────────────────────────────────────────┤
  │                                                                │
  │  👤 Sender MPC Wallet (DQoGcVse...)                            │
  │  ├─ SOL:  0.996  (started 1.000, paid rent, got rent back)    │
  │  ├─ USDC: 75     (started 100, sent 25)                       │
  │  └─ DB balance: 0 (reserved on send)                           │
  │                                                                │
  │  🔐 Claim Authority (3LZh792t...)                              │
  │  ├─ SOL:  4.999  (paid claim tx fee)                           │
  │  └─ USDC: 25     (received from escrow vault)                  │
  │                                                                │
  │  📦 Escrow PDA: CLOSED ❌ (rent returned to sender)             │
  │  🏦 Vault PDA:  CLOSED ❌ (rent returned to sender)             │
  │                                                                │
  │  📱 Recipient:                                                  │
  │  └─ ₹2,336 received in bank via UPI                            │
  │                                                                │
  │  💰 Net cost to sender: $25.00 USDC + ~$0.001 SOL              │
  │  💰 Net cost to platform: ~$0.001 SOL (claim tx fee)            │
  │                                                                │
  └──────────────────────────────────────────────────────────────┘
```

### 🔄 Alternative Path: Refund (No Claim Within 48h)

```
  BEFORE REFUND                           AFTER REFUND
  ═════════════                           ════════════

  Vault PDA                               Vault PDA
  ┌─────────────┐                         ┌─────────────┐
  │ USDC: 25    │────── 25 USDC ─────────►│ CLOSED ❌    │
  └─────────────┘          │              └─────────────┘
                           ▼
  Sender ATA (GuDuFKeX...) ◄──────────    Sender ATA (GuDuFKeX...)
  ┌─────────────┐                         ┌─────────────┐
  │ USDC: 75    │                         │ USDC: 100   │  Full refund! ✅
  └─────────────┘                         └─────────────┘

  Escrow PDA: CLOSED ❌ (rent → sender)
  Vault PDA:  CLOSED ❌ (rent → sender)
  Status: ESCROWED → REFUNDED
```

### 🔑 Key Insight: Why PDAs?

```
  Regular wallet:  "I have the private key, I control the funds"
  PDA (escrow):    "The PROGRAM controls the funds — rules are code"

  ┌──────────────────────────────────────────────────────────┐
  │  Only the escrow program can sign for the escrow PDA     │
  │  ├─ deposit: anyone (if they're the sender)              │
  │  ├─ claim:   ONLY claim authority can trigger             │
  │  ├─ refund:  anyone, but ONLY after deadline passes       │
  │  └─ cancel:  ONLY the original sender                     │
  │                                                            │
  │  Seeds are deterministic — anyone can derive the PDA      │
  │  address, but NOBODY can sign for it except the program   │
  └──────────────────────────────────────────────────────────┘
```

---

## Complete Error Code Reference

| Code | HTTP | Exception | Description |
|---|---|---|---|
| SP-0002 | 400 | InsufficientBalanceException | Wallet balance too low for remittance |
| SP-0003 | 400 | MethodArgumentNotValidException | Request validation failure |
| SP-0006 | 404 | WalletNotFoundException | Wallet not found by ID or userId |
| SP-0007 | 503 | TreasuryDepletedException | Treasury has insufficient funds |
| SP-0008 | 409 | WalletAlreadyExistsException | Wallet already exists for userId |
| SP-0009 | 400 | UnsupportedCorridorException | Currency pair not supported |
| SP-0010 | 404/500 | RemittanceNotFoundException / MpcKeyGenerationException | Remittance not found / MPC ceremony failed |
| SP-0011 | 404 | ClaimTokenNotFoundException | Claim token not found |
| SP-0012 | 409/500 | ClaimAlreadyClaimedException / SolanaTransactionException | Claim already submitted / TX confirmation timeout |
| SP-0013 | 410 | ClaimTokenExpiredException | Claim token past 48h expiry |
| SP-0014 | 409 | InvalidRemittanceStateException | Invalid state for operation |
| SP-0016 | 409 | InvalidRemittanceStateException | Invalid status transition |
| SP-0017 | 500 | SmsDeliveryException | SMS delivery failed |
| SP-0018 | 502 | DisbursementException | INR disbursement failed |
| SP-0020 | 404 | FundingOrderNotFoundException | Funding order not found |
| SP-0021 | 502 | FundingFailedException | Stripe payment / funding failed |
| SP-0022 | 409 | FundingAlreadyInProgressException | Funding already in progress for wallet |
| SP-0026 | 400 | InvalidWebhookSignatureException | Stripe webhook signature invalid |
| SP-0031 | 500 | SolanaTransactionException | Transaction failed on-chain |

---

## Quick Start

### Prerequisites

- Java 25 (`sdk install java 25-tem`)
- Docker + Docker Compose
- Go 1.26 (for MPC sidecar)
- Solana CLI 2.2.7 + Anchor CLI 0.32.1 (for on-chain program)
- Node.js 22+ (for Anchor tests)

### Full Stack (Docker Compose)

```bash
make up
```

```
============================================
  StablePay Dev Stack
============================================
  Backend API:    http://localhost:8080
  Swagger UI:     http://localhost:8080/swagger-ui.html
  Health:         http://localhost:8080/actuator/health
  Temporal UI:    http://localhost:8088
  PostgreSQL:     localhost:5432
  Redis:          localhost:6379
  MPC Sidecar 0:  localhost:50051 (gRPC)
  MPC Sidecar 1:  localhost:50052 (gRPC)
============================================
```

### Infrastructure Only (Local Backend Dev)

```bash
make infra
cd backend && ./gradlew bootRun
```

### Individual Components

```bash
# Backend — compile + format + all tests
cd backend && ./gradlew build

# Anchor program
anchor build && anchor test

# MPC sidecar
cd mpc-sidecar && go build ./... && go test ./... -v -count=1 -timeout 120s
```

### Makefile Targets

| Target | Description |
|---|---|
| `make up` | Build backend + start full Docker Compose stack (7 services) |
| `make down` | Stop all services |
| `make infra` | Start infrastructure only (for local backend dev) |
| `make logs` | Follow Docker Compose logs |
| `make clean` | Stop all services and remove volumes |

### Try the API

A Postman collection is available at [`docs/StablePay.postman_collection.json`](docs/StablePay.postman_collection.json).

Interactive Swagger UI: http://localhost:8080/swagger-ui.html

---

## Testing

```bash
# Backend: all tests + formatting
cd backend && ./gradlew build

# Unit tests only
cd backend && ./gradlew test

# Integration tests with TestContainers
cd backend && ./gradlew integrationTest

# Anchor program tests (799 lines, TypeScript on localnet)
anchor test

# MPC sidecar tests
cd mpc-sidecar && go test ./... -v -count=1 -timeout 120s
```

### CI Pipeline

GitHub Actions runs **7 jobs** on every push to `main` and every PR:

```mermaid
graph TD
    A[Spotless Check] --> B[Unit Tests]
    A --> C[Integration Tests]
    B --> D[Build JAR]
    C --> D
    E[MPC Sidecar Tests]
    F[Anchor Build] --> G[Anchor Tests]
```

---

## Tech Stack

| Component | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.0.5, Spring MVC, JPA |
| Database | PostgreSQL 18, Flyway 12.3 |
| Cache | Redis 8 |
| Workflows | Temporal 1.29.5 (SDK 1.34.0) |
| On-chain | Rust, Anchor 0.32.1, Solana 2.2.7 (devnet) |
| MPC | Go 1.26, bnb-chain/tss-lib (fystack fork) |
| Solana SDK | sol4k 0.7.0 |
| On-ramp | Stripe (card payments + webhooks) |
| Off-ramp | Razorpay UPI Payouts |
| SMS | Twilio 11.3.6 |
| Mapping | MapStruct 1.6.3 |
| Resilience | Resilience4j 2.3.0 |
| API Docs | springdoc-openapi 3.0.2 |
| Build | Gradle 9.4.1 (Kotlin DSL), Jib |
| Testing | JUnit 5, BDDMockito, AssertJ, ArchUnit 1.4.1, TestContainers 1.21.4 |
| CI | GitHub Actions (7 jobs) |

---

## Project Structure

```
stablepay-hackathon/
├── backend/                          # Spring Boot API
│   └── src/
│       ├── main/java/com/stablepay/
│       │   ├── application/          # Controllers, DTOs, config
│       │   ├── domain/               # Models, handlers, ports
│       │   │   ├── wallet/           #   MPC wallet management
│       │   │   ├── remittance/       #   Core remittance flow
│       │   │   ├── funding/          #   Stripe funding orders
│       │   │   ├── claim/            #   SMS claim tokens
│       │   │   ├── fx/               #   FX rate quotes
│       │   │   └── common/           #   Shared ports (SMS, disbursement)
│       │   └── infrastructure/       # Adapters
│       │       ├── db/               #   JPA + Flyway (7 migrations)
│       │       ├── temporal/         #   Workflows + activities
│       │       ├── mpc/              #   gRPC client to sidecars
│       │       ├── solana/           #   RPC + escrow instruction builder + tx confirmation
│       │       ├── stripe/           #   Stripe payments + webhook verification
│       │       ├── razorpay/         #   Razorpay UPI disbursement
│       │       ├── fx/               #   ExchangeRate-API + Redis cache
│       │       └── sms/              #   Twilio + logging fallback
│       ├── test/                     # 61 unit test files
│       └── integration-test/         # 18 integration test files
├── programs/stablepay-escrow/        # Anchor program (Rust)
│   └── src/
│       ├── lib.rs                    # 4 instructions
│       ├── instructions/             # deposit, claim, refund, cancel
│       ├── state/                    # Escrow account + EscrowStatus enum
│       ├── errors.rs                 # 10 custom error codes
│       └── constants.rs              # PDA seeds
├── mpc-sidecar/                      # MPC threshold signing (Go)
│   ├── cmd/sidecar/                  # Entry point
│   ├── internal/
│   │   ├── tss/                      # DKG + Ed25519 signing
│   │   ├── p2p/                      # Ceremony registry + TCP coordination
│   │   ├── server/                   # gRPC (GenerateKey, Sign, HealthCheck)
│   │   └── config/                   # Environment-based config
│   └── proto/                        # Protobuf definitions (sidecar + p2p)
├── tests/                            # Anchor E2E tests (TypeScript, 799 lines)
├── docs/                             # Architecture, standards, ADRs
├── docker-compose.yml                # 7 services
├── Makefile                          # Build + orchestration
└── Anchor.toml
```

---

## Contributing

All work goes through feature branches and pull requests. Never commit directly to `main`.

```bash
git checkout -b feature/STA-42-add-claim-page
cd backend && ./gradlew build
git push -u origin feature/STA-42-add-claim-page
gh pr create --title "STA-42: Add claim page"
```

Branch naming: `feature/STA-{N}-description` · Commit messages: `feat(STA-{N}): description`
