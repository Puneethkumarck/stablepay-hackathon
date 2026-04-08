# StablePay — End-to-End Flow

Cross-border USD→INR remittance on Solana. This document describes the complete flow from sender onboarding to recipient delivery, including failure recovery.

## High-Level Architecture

```
┌──────────────┐     ┌──────────────────────────────────────────────────────┐     ┌──────────────┐
│  Mobile App  │     │                  BACKEND (Spring Boot)               │     │  Claim Page  │
│  (Sender)    │────▶│                                                      │◀────│  (Recipient) │
│  React Native│     │  ┌────────────┐  ┌──────────┐  ┌─────────────────┐  │     │  Next.js     │
└──────────────┘     │  │ Controllers│─▶│ Handlers │─▶│ Outbound Ports  │  │     └──────────────┘
                     │  └────────────┘  └──────────┘  └────────┬────────┘  │
                     └─────────────────────────────────────────┼───────────┘
                                                               │
                     ┌─────────────────────────────────────────┼───────────┐
                     │              INFRASTRUCTURE              │           │
                     │  ┌──────────┐ ┌────────┐ ┌──────┐ ┌────▼─────────┐ │
                     │  │ Temporal  │ │ MPC    │ │Twilio│ │  Solana      │ │
                     │  │ Workflow  │ │Sidecar │ │ SMS  │ │  Devnet      │ │
                     │  │ Engine   │ │ (Go)   │ │      │ │  (Escrow PDA)│ │
                     │  └──────────┘ └────────┘ └──────┘ └──────────────┘ │
                     │  ┌──────────┐ ┌────────┐ ┌──────────────────────┐  │
                     │  │PostgreSQL│ │ Redis  │ │ ExchangeRate API     │  │
                     │  └──────────┘ └────────┘ └──────────────────────┘  │
                     └─────────────────────────────────────────────────────┘
```

**Dependency rule:** Controller → Handler → Outbound Port → Adapter. Never skip the domain layer.

**Components:**

| Component | Tech | Role |
|---|---|---|
| Mobile App | React Native + Expo SDK 52 | Sender-facing app (onboarding, send, track) |
| Backend API | Spring Boot 4.0.3, Java 25 | Hexagonal backend (MVC + JPA) |
| Temporal | Temporal Server | Durable workflow orchestration for remittance lifecycle |
| MPC Sidecar | Go + forked mpcium | Ed25519 threshold signing (2-of-2) |
| Solana | Anchor program on devnet | USDC escrow PDA (deposit/claim/refund/cancel) |
| Twilio | SMS API | Claim link delivery to recipient |
| Claim Page | Next.js + shadcn/ui | Recipient web page (no app needed) |
| PostgreSQL | 16 | Wallets, remittances, claim tokens |
| Redis | Cache | FX rate caching (60s TTL) |

---

## Phase 1: Sender Onboarding & Wallet Creation

Sender signs up with email or phone. No seed phrase, no browser extension. A Solana wallet is created silently via MPC distributed key generation.

```
┌────────┐          ┌──────────┐          ┌───────────┐          ┌──────────┐
│ Sender │          │  Backend │          │ MPC       │          │ Postgres │
│ Mobile │          │  API     │          │ Sidecar   │          │          │
└───┬────┘          └────┬─────┘          └─────┬─────┘          └────┬─────┘
    │  POST /wallets     │                      │                     │
    │ (email/phone)      │                      │                     │
    │───────────────────▶│                      │                     │
    │                    │  gRPC: DKG keygen     │                     │
    │                    │  (2-of-2 Ed25519)     │                     │
    │                    │─────────────────────▶│                     │
    │                    │                      │                     │
    │                    │  Ed25519 public key   │                     │
    │                    │◀─────────────────────│                     │
    │                    │                      │                     │
    │                    │  Derive Solana address (base58)             │
    │                    │────────────────────────────────────────────▶│
    │                    │                      │    Save wallet       │
    │  Wallet created    │                      │                     │
    │  (no seed phrase!) │                      │                     │
    │◀───────────────────│                      │                     │
```

**What happens:**

1. Sender taps "Sign up" with email or phone number.
2. Backend calls MPC sidecar via gRPC to run distributed key generation (DKG) — two sidecar instances each generate a key share using the EdDSA module from forked mpcium.
3. Neither party holds the full private key (threshold: 2-of-2).
4. The combined Ed25519 public key is derived into a Solana base58 address.
5. Wallet record saved to PostgreSQL. Sender sees "Wallet created" with zero crypto exposure.

---

## Phase 2: Fund Wallet (Demo Mode)

For the hackathon demo, the sender funds their wallet from a pre-funded devnet treasury. In production, this would be Stripe ACH on-ramp.

```
┌────────┐          ┌──────────┐          ┌───────────┐
│ Sender │          │  Backend │          │  Solana   │
│ Mobile │          │  API     │          │  Devnet   │
└───┬────┘          └────┬─────┘          └─────┬─────┘
    │ POST /wallets/     │                      │
    │   {id}/fund        │                      │
    │───────────────────▶│                      │
    │                    │  Transfer USDC from   │
    │                    │  treasury → sender    │
    │                    │─────────────────────▶│
    │                    │                      │
    │                    │  Tx confirmed         │
    │                    │◀─────────────────────│
    │                    │                      │
    │                    │  Update balance in DB │
    │  Balance: $100 USDC│                      │
    │◀───────────────────│                      │
```

**What happens:**

1. Sender taps "Fund Wallet" in the app.
2. Backend transfers devnet USDC from a pre-funded treasury wallet (10,000 USDC) to the sender's on-chain wallet.
3. On-chain transfer confirmed, balance updated in PostgreSQL.
4. Sender sees their USDC balance in the app.

---

## Phase 3: Send Remittance (Core Flow)

The sender enters an amount, recipient phone number, and confirms the FX rate. This triggers a Temporal workflow that handles MPC signing, on-chain escrow, and SMS delivery.

```
┌────────┐     ┌──────────┐     ┌──────────┐     ┌─────────┐     ┌────────┐     ┌────────┐
│ Sender │     │ Backend  │     │ Temporal │     │  MPC    │     │ Solana │     │ Twilio │
│ Mobile │     │ API      │     │ Workflow │     │Sidecar  │     │ Devnet │     │  SMS   │
└───┬────┘     └────┬─────┘     └────┬─────┘     └────┬────┘     └───┬────┘     └───┬────┘
    │               │                │                 │              │              │
    │ GET /fx/rate  │                │                 │              │              │
    │ ?from=USD     │                │                 │              │              │
    │ &to=INR       │                │                 │              │              │
    │──────────────▶│                │                 │              │              │
    │               │ Fetch rate (ExchangeRate API + Redis cache)     │              │
    │ Rate: 84.50   │                │                 │              │              │
    │◀──────────────│                │                 │              │              │
    │               │                │                 │              │              │
    │ POST          │                │                 │              │              │
    │ /remittances  │                │                 │              │              │
    │ {amt: $100,   │                │                 │              │              │
    │  phone: +91.. │                │                 │              │              │
    │  fxRate: 84.5}│                │                 │              │              │
    │──────────────▶│                │                 │              │              │
    │               │                │                 │              │              │
    │               │ ┌──────────────────────────┐     │              │              │
    │               │ │ In one DB transaction:   │     │              │              │
    │               │ │ 1. Lock FX rate          │     │              │              │
    │               │ │ 2. Reserve balance       │     │              │              │
    │               │ │    (SELECT FOR UPDATE)   │     │              │              │
    │               │ │ 3. Create remittance     │     │              │              │
    │               │ │    (status: INITIATED)   │     │              │              │
    │               │ │ 4. Generate claim token  │     │              │              │
    │               │ └──────────────────────────┘     │              │              │
    │               │                │                 │              │              │
    │               │ Start workflow │                 │              │              │
    │               │───────────────▶│                 │              │              │
    │ 202 Accepted  │                │                 │              │              │
    │◀──────────────│                │                 │              │              │
    │               │                │                 │              │              │
    │               │                │ ── SIGNING ──   │              │              │
    │               │                │ Build escrow    │              │              │
    │               │                │ deposit tx      │              │              │
    │               │                │ (fresh blockhash)              │              │
    │               │                │────────────────▶│              │              │
    │               │                │                 │ Ed25519 sign │              │
    │               │                │ Signed tx bytes │              │              │
    │               │                │◀────────────────│              │              │
    │               │                │                 │              │              │
    │               │                │ ── SUBMITTING ──│              │              │
    │               │                │ Submit signed tx│              │              │
    │               │                │────────────────────────────────▶              │
    │               │                │                 │              │              │
    │               │                │                 │  Tx confirmed│              │
    │               │                │◀───────────────────────────────│              │
    │               │                │                 │              │              │
    │               │                │ Status → ESCROWED              │              │
    │               │                │                 │              │              │
    │               │                │ Send claim SMS  │              │              │
    │               │                │─────────────────────────────────────────────▶│
    │               │                │                 │              │              │
    │               │                │                 │              │  SMS to +91..│
    │               │                │                 │              │  "You have   │
    │               │                │                 │              │   ₹8,450!    │
    │               │                │ SMS delivered   │              │   Claim: URL"│
    │               │                │◀─────────────────────────────────────────────│
    │               │                │                 │              │              │
    │ GET /remittances/{id}          │                 │              │              │
    │──────────────▶│                │                 │              │              │
    │ Status:       │                │                 │              │              │
    │ ESCROWED      │                │                 │              │              │
    │◀──────────────│                │                 │              │              │
```

**What happens:**

1. **FX quote** — Sender sees live USD→INR rate (fetched from ExchangeRate-API, cached in Redis for 60s, with hardcoded 84.50 fallback).
2. **Create remittance** — Sender confirms. In a single DB transaction:
   - FX rate locked for this remittance.
   - Wallet balance reserved via pessimistic lock (`SELECT ... FOR UPDATE`, 4s timeout — see ADR-021).
   - Remittance record created with status `INITIATED`.
   - Cryptographically random claim token (UUID) generated.
3. **Temporal workflow starts** — `RemittanceLifecycleWorkflow` launched. API returns `202 Accepted` immediately.
4. **SIGNING** (internal sub-state) — Workflow builds an Anchor escrow deposit instruction, fetches a fresh Solana blockhash, and sends unsigned tx bytes to MPC sidecar for Ed25519 threshold signing.
5. **SUBMITTING** (internal sub-state) — Signed transaction submitted to Solana devnet. On confirmation, USDC is locked in an escrow PDA (seeds: `[b"escrow", remittance_id]`).
6. **ESCROWED** — Remittance status updated. Workflow sends SMS via Twilio with a claim link containing the bearer token.
7. **Sender polls** — Mobile app polls `GET /remittances/{id}` every 3 seconds to show status updates.

---

## Phase 4: Recipient Claims

The recipient receives an SMS with a claim link. They open it in a browser, see the amount in INR, enter their UPI ID, and claim the funds. No app, no wallet, no crypto knowledge required.

```
┌───────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌─────────┐     ┌────────┐
│ Recipient │     │  Claim   │     │ Backend  │     │ Temporal │     │  MPC    │     │ Solana │
│  (India)  │     │  Page    │     │ API      │     │ Workflow │     │Sidecar  │     │ Devnet │
└─────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬────┘     └───┬────┘
      │                 │                │                │                 │              │
      │ Click SMS link  │                │                │                 │              │
      │ stablepay.com/  │                │                │                 │              │
      │ claim/{token}   │                │                │                 │              │
      │────────────────▶│                │                │                 │              │
      │                 │                │                │                 │              │
      │                 │ GET /claims/   │                │                 │              │
      │                 │   {claimId}    │                │                 │              │
      │                 │───────────────▶│                │                 │              │
      │                 │                │                │                 │              │
      │                 │ {sender: "Raj",│                │                 │              │
      │                 │  amount: ₹8450,│                │                 │              │
      │                 │  expiry: 46h}  │                │                 │              │
      │                 │◀───────────────│                │                 │              │
      │                 │                │                │                 │              │
      │ Shows:          │                │                │                 │              │
      │ "Raj sent you   │                │                │                 │              │
      │  ₹8,450"        │                │                │                 │              │
      │ [Enter UPI ID]  │                │                │                 │              │
      │◀────────────────│                │                │                 │              │
      │                 │                │                │                 │              │
      │ Enter UPI:      │                │                │                 │              │
      │ name@upi        │                │                │                 │              │
      │────────────────▶│                │                │                 │              │
      │                 │ POST /claims/  │                │                 │              │
      │                 │   {claimId}    │                │                 │              │
      │                 │ {upiId: ".."}  │                │                 │              │
      │                 │───────────────▶│                │                 │              │
      │                 │                │                │                 │              │
      │                 │                │ Signal:        │                 │              │
      │                 │                │ claim_submitted│                 │              │
      │                 │                │───────────────▶│                 │              │
      │                 │                │                │                 │              │
      │                 │                │                │ ── CLAIMING ──  │              │
      │                 │                │                │ Build release tx│              │
      │                 │                │                │ (claim authority│              │
      │                 │                │                │  signs release) │              │
      │                 │                │                │────────────────▶│              │
      │                 │                │                │                 │ Release USDC │
      │                 │                │                │ Signed tx       │ from escrow  │
      │                 │                │                │◀────────────────│              │
      │                 │                │                │                 │              │
      │                 │                │                │ Submit release  │              │
      │                 │                │                │──────────────────────────────▶│
      │                 │                │                │                 │              │
      │                 │                │                │                 │ Tx confirmed │
      │                 │                │                │◀─────────────────────────────│
      │                 │                │                │                 │              │
      │                 │                │                │ Status → CLAIMED│              │
      │                 │                │                │                 │              │
      │                 │                │                │ ── DELIVERING ──│              │
      │                 │                │                │ Simulate INR    │              │
      │                 │                │                │ disbursement    │              │
      │                 │                │                │ (mock: USDC →   │              │
      │                 │                │                │  Circle → INR → │              │
      │                 │                │                │  UPI payout)    │              │
      │                 │                │                │                 │              │
      │                 │                │                │ Status →        │              │
      │                 │                │                │ DELIVERED       │              │
      │                 │                │                │                 │              │
      │                 │ 200 OK         │                │                 │              │
      │                 │ "₹8,450 sent   │                │                 │              │
      │                 │  to name@upi"  │                │                 │              │
      │                 │◀───────────────│                │                 │              │
      │                 │                │                │                 │              │
      │  Confirmation   │                │                │                 │              │
      │◀────────────────│                │                │                 │              │
```

**What happens:**

1. **Recipient clicks SMS link** — Opens the Next.js claim page in their browser. No app download, no wallet setup.
2. **Claim page loads** — Fetches remittance details via `GET /claims/{claimId}`. Shows: sender name, amount in INR, expiry countdown.
3. **Recipient enters UPI ID** — Basic UPI format validation (non-empty, contains `@`). This serves as a weak second factor.
4. **Claim submitted** — `POST /claims/{claimId}` sends a `claim_submitted` signal to the Temporal workflow.
5. **CLAIMING** — Workflow builds an escrow release instruction. The backend's claim authority keypair signs the release. USDC is released from the escrow PDA on-chain. The Anchor program closes the escrow account (prevents double-claim).
6. **DELIVERING** — Simulated INR disbursement. In production: USDC → Circle redemption → partner API → INR to UPI/bank. For hackathon: mock service with real architecture.
7. **DELIVERED** — Terminal state. Recipient sees confirmation on the claim page. Sender sees "Delivered" in the mobile app.

---

## Phase 5: Failure Recovery (Guaranteed Delivery)

Every remittance reaches a terminal state — either `DELIVERED` (recipient got INR) or `REFUNDED` (sender got USDC back). No remittance is silently lost or stuck.

### State Machine

```
                  ┌──────────────────────────────────────────────┐
                  │         TEMPORAL WORKFLOW STATE MACHINE       │
                  │                                              │
                  │   INITIATED ──▶ ESCROWED ──▶ CLAIMED ──▶ DELIVERED
                  │       │             │                        │
                  │       │             │ (48h timeout)          │
                  │       ▼             ▼                        │
                  │   (retry with   REFUNDED                     │
                  │    re-sign)     (auto-refund                 │
                  │       │          to sender)                  │
                  │       ▼                                      │
                  │   FAILED                                     │
                  │   (manual review                             │
                  │    after max retries)                        │
                  └──────────────────────────────────────────────┘
```

**User-facing states:** `INITIATED` → `ESCROWED` → `CLAIMED` → `DELIVERED`

**Internal sub-states** (within Temporal, not exposed to user): `SIGNING`, `SUBMITTING`, `CLAIMING`, `DELIVERING`

### Retry Strategy (Re-Sign on Every Retry)

Solana blockhashes expire in ~60 seconds. Stale signed transactions cannot be resubmitted. Each retry gets a fresh blockhash and a new MPC signature.

```
  ┌───────────┐     ┌──────────────────────────────────┐
  │ Attempt 1 │────▶│ Fail → get fresh blockhash       │
  └───────────┘     │      → re-sign via MPC (~1-3s)   │
                    │      → retry (exponential backoff)│
                    └──────────────┬─────────────────────┘
                                   │
  ┌───────────┐     ┌──────────────▼─────────────────────┐
  │ Attempt 2 │────▶│ Fail → get fresh blockhash         │
  └───────────┘     │      → re-sign via MPC              │
                    │      → retry                        │
                    └──────────────┬───────────────────────┘
                                   │
  ┌───────────┐     ┌──────────────▼──────────────────┐
  │ Attempt N │────▶│ Max retries hit → escalate      │
  └───────────┘     │ to manual review                │
                    └─────────────────────────────────┘
```

### Expiry & Refund

- **48-hour expiry window** — accommodates US→India timezone difference (up to 13.5h offset).
- If recipient does not claim within 48 hours, the Temporal workflow automatically refunds USDC to the sender's wallet.
- **Claim-after-timeout race:** Workflow uses a mutex flag. If a claim signal arrives during refund, claim wins only if the refund transaction hasn't been submitted yet.

### SMS Failure Handling

- SMS delivery is a retriable Temporal activity. If SMS fails after escrow lock, the workflow retries.
- On persistent failure: remittance stays `ESCROWED`, API response includes `smsNotificationFailed: true`, sender sees "Recipient not reached" with a "Resend" option.

---

## Complete Lifecycle Summary

```
SENDER (US)                          SYSTEM                        RECIPIENT (India)
─────────────                        ──────                        ─────────────────
1. Sign up (email/phone)
   └─▶ MPC wallet created silently

2. Fund wallet ($100 USDC)
   └─▶ Treasury → sender wallet

3. Send $100 to +91xxxxxxxxxx
   └─▶ FX locked at 84.50
   └─▶ Balance reserved
   └─▶ Temporal workflow starts
        │
        ├─▶ MPC signs deposit tx
        ├─▶ USDC locked in escrow PDA
        ├─▶ Status: ESCROWED
        └─▶ SMS sent ──────────────────────────▶ 4. Receives SMS
                                                    "You have ₹8,450!"
                                                    [Claim link]

5. Tracks status in app                         6. Opens claim page
   (polling every 3s)                              Sees: "Raj sent ₹8,450"
                                                   Enters UPI: name@upi

                                                 7. Submits claim
        │◀─────────────────────────────────────────┘
        ├─▶ Claim signal to workflow
        ├─▶ MPC signs release tx
        ├─▶ Escrow released on-chain
        ├─▶ Status: CLAIMED
        ├─▶ Simulated INR → UPI
        └─▶ Status: DELIVERED

8. Sees "Delivered" in app              ◀─────▶ 9. Sees "₹8,450 sent
                                                    to name@upi"
```

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| Re-sign on every retry | Solana blockhashes expire in ~60s; re-submission is unreliable |
| Pessimistic locking for balance | Prevents double-spend on concurrent sends (ADR-021) |
| Bearer token claim links | Simple for hackathon; UPI entry as weak second factor |
| Escrow account closed on claim | On-chain double-claim prevention (idempotency) |
| SMS as retriable Temporal activity | Recipient must be notified; failure leaves remittance stuck |
| 48-hour expiry | US→India timezone gap; auto-refund guarantees sender isn't stuck |
| Simulated INR off-ramp | Real architecture (Circle → partner → UPI) with mock execution |

## Related Docs

- [Requirements](brainstorms/2026-04-03-stablepay-cross-border-requirements.md)
- [Implementation Plan](plans/2026-04-03-001-feat-cross-border-remittance-plan.md)
- [Architecture Decision Records](ADR.md)
- [Coding Standards](CODING_STANDARDS.md)
