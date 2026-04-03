# Competitive Analysis — StablePay

> Source: Colosseum Copilot API (builder corpus of 6,500+ hackathon projects)
> Date: 2026-04-03
> Cluster: "Stablecoin Payment Rails and Infrastructure" (202 projects, crowdedness score: 202)

## Executive Summary

StablePay's triple differentiator — **guaranteed delivery (Temporal state machine) + MPC wallet abstraction (Ed25519 threshold signing) + frictionless recipient claiming (web link, no wallet)** — is unmatched in the Colosseum corpus. The closest competitors either lack on-chain escrow logic (Rampa, Vikki), submitted no working code (Rampa, FIATORY), or target B2B instead of consumers (Credible Finance). Our full-stack architecture with a custom Anchor program is the deepest technical implementation in this competitive set.

## Direct Competitors

### 1. Credible Finance (Strongest Competitor)

| Field | Detail |
|---|---|
| **Hackathon** | Cypherpunk (Sept 2025) |
| **Prize** | 2nd Place Stablecoins ($20K) |
| **Accelerator** | C4 |
| **Team** | 2 |
| **Corridor** | USD → INR |
| **Target** | B2B (banks, fintechs, businesses) |
| **One-liner** | "Stablecoin-powered USD-INR remittance rail for businesses offering guaranteed FX rates 2% better than competitors" |

**Architecture (inferred — GitHub repo is private):**
- API-first approach (solution tag: "api-first cross-border payments")
- Liquidity aggregation for FX pricing
- Oracle integration for rate feeds
- Dune Analytics for monitoring
- No visible on-chain program, wallet abstraction, or tx-recovery mechanism

**Strengths:**
- Clear, measurable value prop ("2% better than Wise") — judges love this
- B2B focus simplifies user acquisition narrative
- Won prize + accelerator — validates the USD→INR corridor
- Small team (2) executed well enough to win

**Weaknesses vs StablePay:**
- B2B only — no consumer UX, no wallet abstraction
- No MPC wallet — recipients/senders need existing crypto infrastructure
- No guaranteed delivery — no state machine, no auto-retry
- No on-chain escrow — likely raw transfers without atomic guarantees
- Private repo — cannot verify technical depth

**StablePay's edge:** Consumer UX + wallet abstraction + guaranteed delivery + on-chain escrow. Different target market (consumer vs B2B) means we're not directly competing for the same judges' attention.

---

### 2. Rampa — Send Money, Build Wealth

| Field | Detail |
|---|---|
| **Hackathon** | Cypherpunk (Sept 2025) |
| **Prize** | None |
| **Accelerator** | None |
| **Team** | 5 |
| **Corridor** | EUR → LATAM |
| **Target** | Consumer (migrant workers, LATAM families) |
| **One-liner** | "On-chain remittance platform for Europe to LATAM using Solana stablecoins with integrated wealth-building tools" |

**Architecture:**
- **Backend:** NestJS, PostgreSQL, Docker
- **Mobile:** Kotlin Android + React Native
- **Web:** Next.js landing page
- **Wallet:** Para SDK (third-party MPC, non-custodial) — chosen over Web3Auth for React Native compatibility
- **On-chain:** Pure USDC transfers on Solana — **no custom program, no escrow**
- **Off-ramp:** Virtual cards + local cash-out mentioned but not implemented
- **Reliability:** No retry/recovery mechanism visible
- **Code maturity:** README + demo video only — **no source code in the submission repo**

**Strengths:**
- MPC wallet (Para SDK) — same UX goal as ours
- Wealth-building angle (tokenized portfolio) — novel narrative
- Virtual debit cards — practical for recipients
- Active (4 updates posted)

**Weaknesses vs StablePay:**
- **No working code submitted** despite team of 5 — execution gap
- No on-chain escrow (raw transfers only)
- No guaranteed delivery or tx-recovery
- Recipient needs the app or virtual card — not frictionless
- Used third-party MPC (Para SDK) vs our own infrastructure
- Didn't win any prize

**StablePay's edge:** Working code > README. Custom Anchor escrow > raw transfers. Own MPC infra > third-party SDK. Claim-via-link > app required. Temporal guaranteed delivery is unique.

---

### 3. FIATORY

| Field | Detail |
|---|---|
| **Hackathon** | Cypherpunk (Sept 2025) |
| **Prize** | None |
| **Accelerator** | None |
| **Team** | 1 |
| **Corridor** | Generic |
| **Target** | Migrant workers, unbanked |
| **One-liner** | "Decentralized remittance platform enabling low-cost cross-border payments using stablecoins" |

**Architecture:**
- GitHub repo is private — no code analyzable
- Description is one line: "Decentralized remittance platform"
- Uses escrow primitive (per Colosseum tags) but no details available
- Solo builder

**Strengths:**
- Mentions escrow pattern

**Weaknesses vs StablePay:**
- Minimal description, private repo — likely incomplete
- Solo builder with no prize — suggests low execution quality
- Generic corridor (no specific market focus)
- No wallet abstraction, no tx-recovery

**StablePay's edge:** Everything. This is a low-quality submission.

---

### 4. Vikki Cross-Border Remit

| Field | Detail |
|---|---|
| **Hackathon** | Cypherpunk (Sept 2025) |
| **Prize** | None |
| **Accelerator** | None |
| **Team** | 1 |
| **Corridor** | USD → VND (Vietnam) |
| **Target** | Retail users, SMEs, expatriates |
| **One-liner** | "Blockchain-based cross-border remittance for retail users and SMEs with low fees and fast settlement" |

**Architecture:**
- **Backend:** NestJS + TypeScript, Prisma/SQLite, Redis + BullMQ job queues
- **On-chain:** Reads USDC transfers via @solana/web3.js — **no custom program**
- **Wallet:** External wallet sends USDC to custodial bank wallet address
- **Off-ramp:** Partner API with webhook confirmation, FX quotes table (USD/VND)
- **Reliability:** BullMQ queues with retry logic across 5 queues (fx, transfer, offramp, notify, kyc)
- **Recipient:** Receives fiat in bank account — no crypto knowledge needed
- **Code maturity:** Backend prototype only — no frontend, no on-chain program

**Strengths:**
- BullMQ retry queues show awareness of reliability concerns
- Off-ramp partner API integration (beyond mock)
- Recipient gets fiat — practical

**Weaknesses vs StablePay:**
- BullMQ is stateless between restarts — Temporal is durable
- No on-chain escrow — raw transfer monitoring only
- Custodial wallet — sender needs external wallet
- Backend only — no mobile app, no claim page
- Solo builder, no prize

**StablePay's edge:** Temporal > BullMQ for reliability. Custom Anchor escrow > read-only monitoring. MPC wallet > external wallet requirement. Full-stack > backend-only.

---

## Architecture Comparison Matrix

| Dimension | Credible Finance | Rampa | Vikki | **StablePay** |
|---|---|---|---|---|
| **On-chain program** | Unknown (private) | None (raw transfers) | None (read-only) | **Custom Anchor escrow** |
| **Wallet abstraction** | None visible | Para SDK (third-party MPC) | External wallet | **Own MPC Ed25519 threshold** |
| **Transaction reliability** | None visible | None visible | BullMQ retry queues | **Temporal durable workflows** |
| **State machine** | None visible | None | None | **INITIATED→ESCROWED→CLAIMED→DELIVERED** |
| **Auto-retry on failure** | None | None | BullMQ retries | **Re-sign + re-submit with fresh blockhash** |
| **Backend framework** | Unknown | NestJS | NestJS + BullMQ | **Spring Boot 4.0.3 + hexagonal** |
| **Recipient experience** | Needs crypto infra | Needs app + virtual card | Bank deposit (no app) | **Claim via web link (no wallet)** |
| **On-ramp** | Unknown | Not implemented | Not visible | **Stripe sandbox + treasury** |
| **Off-ramp** | Unknown | Virtual cards (mentioned) | Partner API + webhooks | **Circle + simulated INR partner** |
| **Mobile client** | None | React Native + Kotlin | None | **React Native + Expo** |
| **Web client** | Unknown | Next.js landing | None | **Next.js claim page** |
| **Code submitted** | Private | README only | Backend only | **Full-stack + on-chain** |
| **Target market** | B2B | Consumer | Retail + SME | **Consumer** |
| **Corridor** | USD→INR | EUR→LATAM | USD→VND | **USD→INR** |
| **Team size** | 2 | 5 | 1 | **2-3** |
| **Prize** | 2nd Stablecoins ($20K) | None | None | — |
| **Accelerator** | C4 | None | None | — |

## Cluster Analysis

Our cluster "Stablecoin Payment Rails and Infrastructure" has **202 projects**. Top problem tags:

| Problem | Count | StablePay addresses? |
|---|---|---|
| High remittance fees | 25 | Yes — <$0.01 Solana fees |
| Slow cross-border payments | 16 | Yes — sub-minute settlement |
| High cross-border fees | 13 | Yes — stablecoin rails |
| Complex crypto onboarding | 12 | Yes — MPC wallet, no seed phrases |
| High transaction fees | 11 | Yes — Solana devnet |

Top tech in the cluster: Solana (200), React (65), Rust (39), TypeScript (30), Anchor (26). StablePay uses all of these.

## What Judges Rewarded (Lessons from Winners)

Based on Credible Finance (2nd place + C4 accelerator):

1. **Measurable value prop** — "2% better FX than Wise" is concrete and testable
2. **Specific corridor** — USD→INR, not "all corridors"
3. **Working demo** — enough to win prize despite private repo
4. **Clear business model** — B2B with API-first approach

Based on CargoBill (1st place Breakout, from our brainstorm research):

1. **Narrow vertical** — logistics freight only
2. **Real pain point** — carriers lose money to SWIFT fees
3. **Business-grade features** — multisig, role-based permissions
4. **Non-custodial** — avoids money transmitter licensing

## StablePay's Positioning

**Our pitch should lead with:**

1. **"Guaranteed delivery"** — the Temporal state machine with auto-retry is truly unique. No other project in the corpus has this. Demo it live (SC3: inject failure → watch recovery).

2. **"Recipient doesn't need anything"** — claim-via-link is our UX differentiator. Demo it: judge clicks a URL, enters UPI, sees confirmation.

3. **"Real MPC, not a wrapper"** — we built our own Ed25519 threshold signing, not a third-party SDK. Technical depth judges will appreciate.

4. **"$125B corridor with 18M diaspora"** — USD→INR market data is compelling, validated by Credible Finance's accelerator admission.

**Our pitch should NOT:**
- Compare directly to Credible Finance (they're B2B, we're consumer — different markets)
- Claim "lower fees" as the primary differentiator (everyone says this)
- Over-explain the architecture (show the demo, not the diagram)

## Risk: What Could Beat Us

| Threat | Likelihood | Mitigation |
|---|---|---|
| Another team builds similar consumer remittance with better UI | Medium | Our MPC + tx-recovery depth is hard to replicate in 5 weeks |
| Judges prefer B2B over consumer (market size argument) | Low | $125B corridor + 18M diaspora is massive consumer TAM |
| Demo failure during judging | Medium | Pre-record backup video, test 24h before |
| Credible Finance competes again in Frontier | Low | They're in C4 accelerator, likely focused on growth not hackathons |

## Sources

- Colosseum Copilot API: project search, cluster analysis, project details
- GitHub repos: Rampa (README), Vikki (NestJS backend), Credible/FIATORY (private)
- Colosseum Arena: project pages and team data
