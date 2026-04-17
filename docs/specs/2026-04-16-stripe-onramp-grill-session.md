---
title: Grill Session — Stripe On-Ramp Integration
date: 2026-04-16
participants: Puneethkumar CK, Claude
outcome: 16 decisions resolved → spec approved
---

# Grill Session: Stripe On-Ramp Integration

16 design decisions resolved through structured interrogation before writing the spec.

---

## Q1: What exactly are we building?

**Context:** The reference `stablecoin-payments/fiat-on-ramp` is a separate microservice with its own domain (CollectionOrder, Refund, Reconciliation), Kafka events, and a full state machine. StablePay is a monolith with a hackathon deadline (May 11).

**Options:**
- A: Full fiat-on-ramp microservice (like stablebridge)
- B: Minimal Stripe integration inside the StablePay monolith

**Decision: B — Minimal inside the monolith.** No separate microservice, no Kafka, no reconciliation jobs. Just Stripe PaymentIntent + webhook + on-chain USDC transfer.

---

## Q2: Stripe payment method — Checkout Session or PaymentIntent?

**Options:**

| | Checkout Session | PaymentIntent |
|---|---|---|
| UI | Stripe-hosted redirect | Your own card form |
| Effort | Low (backend only) | High (backend + mobile + web) |
| Time | ~1 day | ~3-4 days |

**Decision: PaymentIntent.** User wanted Stripe integration consistent with the stablebridge reference project. However, after seeing the effort comparison...

---

## Q3: Actually, keep as-is or integrate Stripe?

User considered keeping the treasury stub. After discussion, decided to continue the grill to understand the full scope before committing.

**Decision: Continue — understand the work, then decide.**

---

## Q4 (resumed): What's the funding flow?

**Context:** User clarified the intent: "Stripe for fiat on sender side → convert to USDC → transfer."

**Decision: Stripe collects fiat USD → backend converts to USDC from pre-funded treasury → SPL transfer to sender's ATA.**

---

## Q5: Checkout Session vs PaymentIntent (revisited with full context)?

User asked for the difference explained simply:
- **Checkout Session:** Stripe builds the payment UI. You redirect. Zero frontend work.
- **PaymentIntent:** You build the payment UI. Card form in your app. Full control.

**Decision: Considered too much frontend work for hackathon. But then...**

---

## Q6: Keep as-is or proceed?

After seeing both options require frontend work for the card form, user initially said "keep as is." Then asked to continue the grill to understand the full Checkout Session approach.

**Decision: Continue grilling — Stripe Checkout Session path.**

---

## Q7: How does the stablebridge reference project do it?

**Finding:** The stablebridge `fiat-on-ramp` uses **raw HTTP calls** (not Stripe SDK) to create PaymentIntents. No card UI — it's API-to-API. The `CollectionController` accepts bank account details in the request, creates a PaymentIntent, and the webhook confirms it.

**Key insight:** No card form anywhere. The payment is initiated server-side with `payment_method_types: ["us_bank_account"]`.

---

## Q8: SDK or raw HTTP for Stripe?

**Options:**

| | Raw HTTP (like stablebridge) | Stripe Java SDK |
|---|---|---|
| Dependency | None | `com.stripe:stripe-java:28.x` |
| Webhook verify | Manual HMAC | `client.constructEvent()` |
| Type safety | Manual JSON parsing | Typed objects |

**Decision: Stripe Java SDK.** Less code, type-safe, recognizable to judges.

---

## Q9: What triggers USDC transfer after Stripe succeeds?

**Options:**
- A: Webhook handler calls TreasuryServiceAdapter directly
- B: Webhook starts a Temporal workflow

**Decision: B — Temporal workflow.** Automatic retries, handles long-running Solana transfers, crash-resilient.

---

## Q10: New Temporal workflow or extend existing?

**Options:**
- A: New `WalletFundingWorkflow`
- B: Add activity to existing `RemittanceLifecycleWorkflow`

**Decision: A — New workflow.** Funding and remittance are independent lifecycles. Own task queue, own retry config, failure isolation.

---

## Q11: Treasury wallet — separate keypair or reuse claim authority?

**Options:**

| | Separate treasury keypair | Reuse claim authority |
|---|---|---|
| Security | Compromise one ≠ compromise other | One key = everything |
| Accounting | Clear separation of roles | Muddied |
| Production | Different access policies | Not acceptable |

**Decision: Separate treasury keypair.** New `TREASURY_PRIVATE_KEY` env var. Clear accounting: treasury funds users, claim authority releases escrow.

---

## Q12: FundingOrder — new DB table or just update wallet?

**Options:**
- A: New `funding_orders` table with state machine
- B: Just increment wallet balance directly

**Decision: A — New FundingOrder.** Provides idempotency (no double-funding on webhook retry), audit trail, and refund tracking. Lightweight state machine: PENDING → PAYMENT_CONFIRMED → FUNDED → REFUND_INITIATED → REFUNDED.

---

## Q13: Replace existing fund endpoint or add new one?

**Options:**
- A: Replace `POST /api/wallets/{id}/fund` — returns FundingOrder instead of Wallet
- B: New endpoint, keep existing as demo shortcut

**Decision: A — Replace existing.** Response changes to `FundingOrderResponse` with Stripe PaymentIntent details.

---

## Q14: How to make the demo seamless in Stripe test mode?

**Approach:** In test mode, PaymentIntent is created with `confirm: true` and test payment method `pm_card_visa`. Succeeds instantly, webhook fires within seconds. No card UI needed.

**Decision: Configure via properties** (`stripe.test-mode`, `stripe.auto-confirm`, `stripe.test-payment-method`). Not hardcoded — configurable for production vs demo.

---

## Q15: How does Stripe reach local Docker stack for webhooks?

**Options:**
- A: Stripe CLI (`stripe listen --forward-to localhost:8080/webhooks/stripe`)
- B: ngrok tunnel
- C: Poll instead of webhook

**Decision: A — Stripe CLI.** Official tool, stable webhook secret, real-time event logs. Added as Makefile target.

---

## Q16: Who pays for sender's first SOL transaction fee?

**Options:**
- A: Fund SOL in the WalletFundingWorkflow (automatic)
- B: Keep SOL funding separate/manual

**Decision: A — Workflow handles SOL.** Activity `ensureSolBalance` checks sender's SOL balance, tops up from treasury if < 0.005 SOL. Also creates ATA if needed. Demo is fully self-contained — one API call funds everything.

---

## Q17: Where does funding live in hexagonal architecture?

**Options:**
- A: New `domain/funding/` subdomain
- B: Inside existing `domain/wallet/`

**Decision: A — New subdomain.** FundingOrder has its own lifecycle, own port (`PaymentGateway`), and is a separate bounded context from wallet CRUD. Matches stablebridge pattern.

---

## Q18: PaymentGateway port — minimal or include refunds?

**Options:**
- A: Minimal — just `initiatePayment`
- B: Include `refund` method

**Decision: B — Include refunds.** Full flow: FUNDED → REFUND_INITIATED → REFUNDED. Explicit refund endpoint `POST /api/funding-orders/{fundingId}/refund`.

---

## Q19: When does refund happen?

**Options:**
- A: Automatic on remittance refund (cross-domain coupling)
- B: Explicit refund endpoint (sender requests it)

**Decision: B — Explicit.** Sender calls refund when they want fiat back. No coupling between funding and remittance lifecycles.

---

## Q20: Final scope confirmation

All 16 decisions confirmed. Scope summary:

- **26 new files, 7 modified**
- **New domain:** `funding/` with FundingOrder, 3 handlers, 2 ports
- **Stripe:** Java SDK, PaymentIntent, webhook with signature verification
- **Temporal:** `WalletFundingWorkflow` with 5 activities
- **Treasury:** Real SPL + SOL transfers replacing the stub
- **State machine:** PENDING → PAYMENT_CONFIRMED → FUNDED (+ FAILED, REFUND path)
- **Config:** All Stripe params via properties, test mode auto-confirm
- **Refund:** Explicit endpoint, USDC return + Stripe refund

**Outcome:** Spec written at `docs/specs/2026-04-16-stripe-onramp-spec.md`

---

## Decision Summary Table

| # | Question | Decision | Rationale |
|---|---|---|---|
| 1 | Microservice or monolith? | Monolith | Hackathon deadline |
| 2 | Checkout or PaymentIntent? | PaymentIntent | API-to-API like stablebridge |
| 3 | SDK or raw HTTP? | Stripe Java SDK | Type-safe, less code |
| 4 | Webhook trigger | Temporal workflow | Retries, crash-resilient |
| 5 | New or existing workflow? | New WalletFundingWorkflow | Separate lifecycle |
| 6 | Treasury keypair | Separate from claim authority | Security, clear accounting |
| 7 | FundingOrder table? | Yes — with state machine | Idempotency, audit, refund |
| 8 | New or existing endpoint? | Replace existing | Cleaner API |
| 9 | Test mode approach | Auto-confirm via properties | Seamless demo |
| 10 | Webhook delivery | Stripe CLI | Official, reliable |
| 11 | SOL funding | Automatic in workflow | Self-contained demo |
| 12 | Domain placement | New funding subdomain | Bounded context |
| 13 | PaymentGateway scope | Include refunds | Full lifecycle |
| 14 | Refund trigger | Explicit endpoint | No cross-domain coupling |
