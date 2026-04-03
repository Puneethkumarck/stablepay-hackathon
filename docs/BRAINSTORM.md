# StablePay — Hackathon Brainstorm

## 1. Colosseum Frontier Hackathon Intelligence

### What Judges Reward (from past winners)

- Working demo on devnet with real flows, not mockups
- Wallet abstraction for non-crypto users
- Clear vertical/corridor narrative
- Viable business model from day one
- Technical differentiation beyond commodity infra

### Market Data

- $18.6B in stablecoin remittances in Southeast Asia (H1 2025)
- Crypto card volume: ~$100M (early 2023) to ~$1.5B (late 2025) at 106% CAGR
- Visa USDC settlement: $3.5B annualized in Q4 2025
- Solana fees: <$0.01, finality: 400ms
- GENIUS Act (July 2025) provides regulatory clarity for stablecoin payments

### Key Insight

> "Users in these markets don't think of it as 'crypto' — they think of it as 'my savings that keep value.'"

### Underexplored Opportunities (per Colosseum data)

1. Geography-specific apps targeting corridors with daily stablecoin use
2. Family finance apps with stablecoin allowances and parental controls
3. Native iOS apps (React dominates at 41%, native Swift is rare = differentiator)
4. Local merchant loyalty via Token Extensions
5. Skill games with micropayment entries

### Well-Covered Territory (avoid or differentiate)

- Generic stablecoin wallets with debit cards
- Basic P2P payments (Cron, Beamlink already exist)
- DeFi dashboards

---

## 2. Competitive Landscape

### CargoBill (1st Place Breakout, Stablecoins Track, C3 Accelerator)

**What they solved:** Cross-border payments for freight forwarders and logistics carriers.

**Architecture:**
- Multisig business wallets (Squads Protocol on Solana)
- Role-based permission and approval engine
- Non-custodial model (avoids money transmitter licensing)
- On/off-ramp integration (Circle / local partners)
- USDC on Solana for instant settlement
- Flat fee pricing regardless of amount
- Cashback rewards turning cost center into revenue

**Why they won:**
- Narrow vertical (logistics only)
- Real pain point (carriers lose money to SWIFT fees + delayed settlement)
- Business-grade multisig (not just a single wallet)
- Clear revenue model from day one

**What they didn't solve:**
- No ERP integration (corporate buyer side untouched)
- No consumer layer (pure B2B)
- No tx-recovery / guaranteed delivery
- Single vertical only

### Other Relevant Projects

| Project | What They Do | Gap vs StablePay |
|---|---|---|
| **Cron** | Venmo-style P2P on Solana (native iOS, Swift) | No cross-border, no FX, no off-ramp |
| **Beamlink** | Send tokens via shareable links (iMessage/WhatsApp) | No tx-recovery, no MPC security |
| **Bucx** | Mobile USDC wallet with virtual debit cards | No corridor-specific UX, no guaranteed delivery |
| **Credible Finance** | USD-INR corridor, 2% better FX than Wise (C4 accelerator) | Only one corridor, no wallet abstraction |
| **Decal** | Point-of-sale + token-based loyalty | Merchant-focused, not remittance |
| **Bannga** | Spending → USDC cashback rewards | Not a payment/remittance tool |

---

## 3. Our Competitive Advantages

| Existing Asset | Hackathon Advantage |
|---|---|
| **MPC Wallet** | Non-custodial wallet abstraction — no seed phrases, threshold signing |
| **tx-recovery** | Guaranteed delivery with state machine + auto-retry + Temporal workflows |
| **stablebridge-indexer** | Real-time payment tracking and confirmation |
| **stablebridge-platform** | End-to-end Solana orchestration |

**Unique moat:** No other hackathon project combines production-grade reliability (tx-recovery) + non-custodial security (MPC) in a consumer UX.

---

## 4. Use Case Options

### Option A: Cross-Border Remittance (Primary Recommendation)

**One-liner:** Send money home in seconds, not days — USDC remittances on Solana with MPC wallet abstraction and guaranteed delivery.

**Target corridor candidates:**
- USD → INR (18M Indian diaspora in US, $125B annual remittances to India)
- USD → PHP (4M Filipino diaspora in US, $38B annual remittances to Philippines)
- USD → MXN (37M Mexican diaspora in US, $63B annual remittances to Mexico)

**Key features:**
1. Send by phone/email — recipient doesn't need a wallet or app
2. MPC-secured wallet — created on signup, no seed phrase
3. Guaranteed delivery — state machine: INITIATED → SIGNED → SUBMITTED → CONFIRMED → DELIVERED
4. Real-time tracking with push notifications
5. Corridor-aware FX with rate lock at send time
6. Escrow-based claiming via link (SMS/WhatsApp)

### Option B: AI Agent Cross-Border Payroll

**One-liner:** AI agent auto-routes global contractor payments through cheapest corridor.

- Combines x402 pattern + tx-recovery + MPC wallet
- Targets "Brex for AI Agents" gap
- Less consumer-friendly for demo

### Option C: Family Finance Cross-Border

**One-liner:** Parent in US sets stablecoin allowance for family abroad with spending controls.

- Parental controls, spending categories, auto-disbursement
- Explicitly called out as "unexploited" by Colosseum
- Narrower market but strong narrative

---

## 5. Proposed Architecture (Option A)

```
Mobile App (React Native / Expo)
├── Send Flow (amount, corridor, recipient phone/email)
├── Payment Tracking (real-time status)
└── Contacts / Recipients

API Gateway / Backend
├── MPC Wallet Service (key mgmt, threshold signing)
├── TX Recovery Engine (state machine, auto-retry, Temporal)
├── Payment Orchestrator (corridor routing, FX quotes, compliance)
└── Notification Service (push, SMS, WhatsApp)

Solana
├── Escrow Program (Anchor — lock USDC, release on claim)
├── USDC (SPL Token)
└── Token-2022 (transfer hooks for compliance)

Off-Ramp Layer
├── Circle API (USDC redemption, sandbox for hackathon)
├── Stripe On-Ramp (fiat → USDC)
└── Local partners (future — post-hackathon)
```

### Tech Stack

| Layer | Technology | Source |
|---|---|---|
| Mobile | React Native (Expo) | New |
| Backend API | Spring Boot or Node.js | tx-recovery patterns |
| Wallet | MPC (2-of-3 threshold) | MPC wallet project |
| Tx Reliability | State machine + retry | tx-recovery project |
| On-chain | Anchor (Solana program) | New + stablebridge-platform |
| Indexer | Payment confirmation | stablebridge-indexer |
| Off-ramp | Circle API sandbox | New integration |

---

## 6. Hackathon Timeline (April 6 – May 11)

| Week | Focus |
|---|---|
| Week 1 (Apr 6-12) | Solana escrow program (Anchor), MPC wallet integration, project setup |
| Week 2 (Apr 13-19) | Backend: payment orchestrator, state machine, corridor routing |
| Week 3 (Apr 20-26) | Mobile app: send flow, tracking, recipient claiming |
| Week 4 (Apr 27-May 3) | Off-ramp integration (Circle sandbox), end-to-end testing |
| Week 5 (May 4-11) | Polish demo, record video, write pitch, submit |

---

## 7. Open Questions

- [ ] Which corridor to target for MVP? (USD→INR has largest TAM)
- [ ] React Native vs native Swift? (RN faster to demo, Swift is differentiator)
- [ ] Spring Boot backend (leverage existing code) vs Node.js (faster for hackathon)?
- [ ] What sponsor credits will be available? (drops April 6)
- [ ] Solo or find co-founders via Colosseum's matching?
- [ ] Should we also target the Eternal Challenge as a fallback?
