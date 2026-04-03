---
date: 2026-04-03
topic: stablepay-cross-border-usd-inr
---

# StablePay — Cross-Border USD→INR Remittance on Solana

## Problem Frame

Indian diaspora in the US (18M people) send $125B annually to India. Current options are slow and expensive: banks charge 3-5% with 1-3 day settlement; Wise charges 1-2% with hours-long delays. Stablecoin rails can settle in seconds for <$0.01 in fees, but existing crypto remittance tools require recipients to understand wallets, seed phrases, and token mechanics.

**No hackathon project combines** guaranteed delivery (tx-recovery) + wallet abstraction + frictionless recipient claiming in a consumer-grade UX. Credible Finance (Colosseum C4 accelerator) targets USD→INR but lacks wallet abstraction and guaranteed delivery.

**The builder has production-tested components** across five existing projects — on-ramp (Stripe), off-ramp (Circle), tx-recovery (Temporal state machines), wallet infrastructure, and blockchain indexing — all in Java/Spring Boot 4.0.3. This is an assembly + UX challenge, not a greenfield build.

**"Guaranteed delivery" defined:** Every remittance reaches a terminal state — either CLAIMED + DELIVERED (recipient got INR) or REFUNDED (sender got USDC back). No remittance is silently lost or stuck indefinitely.

## User Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        SENDER (US)                              │
│                                                                 │
│  1. Sign up (email/phone) ──→ Solana wallet created silently    │
│  2. Load USD ──→ Stripe ACH collects fiat ──→ backend transfers │
│     devnet USDC from treasury to sender's wallet                │
│  3. Enter amount + recipient phone ──→ FX rate locked at        │
│     app-side confirmation (before INITIATED state)              │
│  4. Confirm send ──→ USDC locked in Anchor escrow PDA           │
│  5. Track status in-app (INITIATED → ESCROWED → CLAIMED         │
│     → DELIVERED)                                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                         SMS link
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     RECIPIENT (India)                            │
│                                                                 │
│  6. Receives SMS with claim link ──→ opens web page             │
│  7. Enters UPI ID or bank account details                       │
│  8. Claims ──→ backend (claim authority) releases escrow        │
│     ──→ simulated INR disbursement for hackathon demo           │
│  9. Sees INR confirmation on claim page                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                    Throughout the flow:
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              REMITTANCE LIFECYCLE (Temporal)                     │
│                                                                 │
│  • State machine tracks every transition                        │
│  • Auto-retry on Solana tx failure (with backoff)               │
│  • Escalation path: retry → re-sign → manual review             │
│  • Sender sees status updates in-app at every state change      │
└─────────────────────────────────────────────────────────────────┘
```

## Requirements

### P0 — Must-Have for Demo (maps to SC1–SC5)

**Sender Experience**

- R1. Sender signs up with email or phone — no seed phrase, no browser extension. Solana wallet created silently on signup via MPC wallet (Ed25519 threshold signing using forked mpcium Ed25519 code integrated into the existing Java + Go sidecar architecture).
- R2. Sender loads USD via Stripe ACH on-ramp (reuse StripePspAdapter pattern). For devnet: backend transfers USDC from a pre-funded treasury wallet to sender's wallet. Stripe collects fiat; it does not mint USDC.
- R3. Sender enters amount in USD, recipient's phone number, and optional message. App displays live FX rate (USD→INR) and locks it when sender taps "confirm" in the app (before INITIATED state) for a configurable window.
- R4. Sender confirms and USDC is locked in a Solana escrow PDA via custom Anchor program. Transaction is signed by the MPC wallet (Ed25519 threshold signing via forked mpcium, no single point of failure).

**Recipient Experience**

- R6. Recipient receives SMS (via Twilio) with a claim link. No app download required.
- R7. Recipient opens a web claim page showing: sender name, amount in INR, and expiry countdown.
- R8. Recipient enters UPI ID or bank account details and confirms claim.
- R9. On claim: backend (as claim authority on the escrow PDA) releases USDC. For hackathon demo, INR disbursement is simulated with a mock service that shows the real architecture (USDC → Circle redemption → partner API → INR to UPI/bank).

**Guaranteed Delivery**

- R11. Every remittance is tracked by a Temporal workflow state machine with states: INITIATED → ESCROWED → CLAIMED → DELIVERED. (Intermediate signing/submission states are internal to the workflow, not user-facing.)
- R15. Custom Anchor escrow program on Solana that locks USDC in a PDA keyed by remittance ID.
- R16. Escrow supports: deposit (sender's wallet), claim (backend as claim authority — recipient has no wallet), refund (on expiry, back to sender), and cancel (sender before claim). Claim instruction accepts backend signature + recipient authentication proof, not a recipient wallet signature.
- R17. All transactions use USDC (SPL Token) on Solana devnet for hackathon demo.

**Mobile App (Sender)**

- R21. React Native + Expo app with core screens: onboarding, home/balance, send flow, transaction history, transaction detail/tracking.

**Web Claim Page (Recipient)**

- R23. Lightweight responsive web page (can use Next.js from stablebridge-web as base) for the recipient claim flow.
- R24. Claim page shows sender info, amount in INR, expiry timer, and a form for UPI ID or bank details.

### P1 — Strengthens Demo

**Sender Experience**

- R5. Sender sees real-time status updates in-app (WebSocket or polling) through the state machine lifecycle.

**Recipient Experience**

- R10. Recipient receives confirmation on the web page and optional SMS after claim completes.

**Guaranteed Delivery**

- R12. On Solana transaction failure: automatic retry with exponential backoff (up to configurable max retries). On repeated failure: escalate to re-sign → manual review.
- R13. If recipient does not claim within the expiry window, escrow automatically refunds USDC to sender's wallet.
- R14. Sender is notified in-app at every state transition.

**On-Ramp / Off-Ramp**

- R18. USD on-ramp via Stripe sandbox (reuse StripePspAdapter pattern from stablecoin-payments S3).
- R19. USDC off-ramp architecture via Circle API (reuse Circle adapter pattern from stablecoin-payments S5). For hackathon: Circle redeems USDC to USD; INR conversion is simulated.

### P2 — Cut If Behind Schedule

- R20. Real INR disbursement via local partner API (TransFi, Mudrex, or similar) for UPI/bank payout. Mock is the primary hackathon path; real integration is stretch goal.
- R22. Mobile deep-linking for transaction sharing and recipient notifications.

## Success Criteria

- SC1. **Working devnet demo** of full end-to-end flow: USD in → USDC on Solana → escrow → claim → INR out (simulated disbursement with real on-chain escrow).
- SC2. **Sub-minute settlement** from send confirmation to escrow lock on Solana.
- SC3. **Guaranteed delivery demo**: show a transaction failing, auto-retrying, and eventually succeeding — proving the tx-recovery differentiator live.
- SC4. **No seed phrases, no extensions**: judge can create a wallet and send money without touching any crypto primitives.
- SC5. **Recipient claims via link**: open a URL, enter UPI, see INR confirmation — no app, no wallet, no crypto knowledge.

## Scope Boundaries

- **One corridor only**: USD→INR. No other currency pairs.
- **One stablecoin**: USDC on Solana only.
- **Devnet only**: no mainnet deployment for hackathon.
- **No KYC/AML**: Stripe and Circle sandbox environments skip identity verification. Compliance marked as post-hackathon.
- **No multi-currency wallet**: sender wallet holds USDC only.
- **No recurring payments or scheduled transfers**: single one-time sends only.
- **No merchant payments**: consumer-to-consumer remittance only.
- **No Kafka / indexer for hackathon**: Temporal workflows drive state transitions directly via Solana RPC polling. Kafka + indexer are production architecture, not demo requirements.
- **No push notifications**: in-app status updates only. Push is post-hackathon polish.
- **INR off-ramp is simulated**: mock disbursement service with real architecture. Real partner API is P2 stretch goal.

## Key Decisions

- **Java + Spring Boot 4.0.3 backend**: Reuse patterns from all five existing projects. Same stack (Temporal, PostgreSQL, Redis, Resilience4j) across tx-recovery, wallet, and stablecoin-payments. All projects confirmed on Spring Boot 4.0.3.
- **React Native + Expo mobile**: Fastest path to working demo.
- **Custom Anchor escrow**: Demonstrates Solana programming depth. Full control over claim/refund/cancel logic. Backend acts as claim authority (recipient has no wallet).
- **Web claim page for recipients**: "Recipient doesn't need an app" is the UX differentiator. Lower barrier than WhatsApp bot.
- **Simulated INR off-ramp as primary path**: Real architecture (Circle + partner API) shown in code, but hackathon demo uses mock disbursement. Judges understand sandbox constraints.
- **No Kafka for hackathon**: Temporal orchestrates directly. Eliminates infrastructure complexity for demo.
- **SMS only (Twilio)**: Drop WhatsApp delivery. Single channel, 20-minute integration.
- **In-app status, no push notifications**: WebSocket/polling for real-time updates. Push adds infra cost for no demo value.
- **Fork mpcium Ed25519 into existing sidecar**: Fork fystack/mpcium's EdDSA keygen + signing code and integrate into the existing Java + Go sidecar architecture from stablebridge-mpc-wallet. Same tss-lib foundation (bnb-chain fork). Build thin Solana integration: base58 address derivation + Solana tx byte signing. Keeps MPC as a real differentiator, not just a roadmap item.
- **Triple differentiator positioning vs Credible Finance**: Guaranteed delivery (tx-recovery) + wallet abstraction (real MPC, not custodial) + frictionless claiming (web link). The combination is the moat.

## Dependencies / Assumptions

- Stripe sandbox supports ACH on-ramp (verified in stablecoin-payments S3).
- Circle sandbox supports USDC redemption (verified in stablecoin-payments S5).
- Solana devnet USDC faucet or pre-funded treasury wallet available for testing.
- Team of 2-3 (builder + 1-2 teammates via Colosseum matching).
- All existing projects confirmed on Spring Boot 4.0.3 + Java 25 — direct code reuse viable.
- Twilio SMS API for claim link delivery.

## Outstanding Questions

### Resolve Before Planning

None — all blocking questions resolved.

### Deferred to Planning

- [Affects R3][Needs research] FX rate source for USD→INR: CoinGecko API, ExchangeRate API, or partner API rate? Need to determine best source for hackathon reliability.
- [Affects R15, R16][Technical] Anchor escrow program design: PDA derivation scheme, claim authority verification, expiry mechanism (clock sysvar vs. slot-based).
- [Affects R11][Technical] How much of the tx-recovery Temporal workflow can be directly reused vs. adapted for Solana-specific remittance lifecycle?
- [Affects R21][Technical] React Native Solana integration: evaluate @solana/web3.js compatibility with Expo and the chosen wallet abstraction service SDK.

## Existing Assets to Reuse

| Asset | Source Project | What to Reuse |
|---|---|---|
| Stripe on-ramp adapter | stablecoin-payments (S3) | StripePspAdapter, CollectionOrder state machine, outbox pattern |
| Circle off-ramp adapter | stablecoin-payments (S5) | Circle adapter, PayoutOrder state machine, payout orchestration |
| MPC wallet (Ed25519) | stablebridge-mpc-wallet + fystack/mpcium fork | Java + Go sidecar architecture (existing) + mpcium EdDSA keygen/signing (forked). Build Solana address derivation + tx signing layer. |
| TX recovery engine | stablebridge-tx-recovery | Temporal workflow patterns, state machine, auto-retry, escalation logic, Solana RPC client |
| Blockchain indexer | stablebridge-indexer | Solana RPC client code, transfer event parsing (production use, not needed for hackathon demo) |
| Frontend template | stablebridge-web | Next.js base for claim page, shadcn/ui components, TanStack Query patterns |

## Next Steps

→ All blocking questions resolved. `/ce:plan` for structured implementation planning.
