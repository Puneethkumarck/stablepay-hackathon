# StablePay — Hackathon Roadmap

## Timeline: April 6 – May 11, 2026 (5 weeks)

## Dependency Graph

```
Week 1 (April 6-12) — FOUNDATION (all parallel)
═══════════════════════════════════════════════

  Epic #1                Epic #2                Epic #3
  Anchor Escrow          MPC Ed25519 Spike      Backend Scaffolding
  ┌─────────┐            ┌─────────┐            ┌─────────┐
  │ #9  Setup│            │#16 Setup│            │#23 Setup│
  │ #10 State│            │#17 DKG  │◄─ SPIKE   │#24 Model│
  │ #11 Dep. │            │#18 Sign │   Apr 8   │#25 JPA  │
  │ #12 Claim│            │#19 Addr │            │#26 Dock.│
  │ #13 Refnd│            │#20 P2P  │            │#27 Arch │
  │ #14 Cancl│            │#21 gRPC │            └────┬────┘
  │ #15 Deply│            │#22 Dock.│                 │
  └────┬─────┘            └────┬────┘                 │
       │                       │                      │
       │    MILESTONE: Escrow on devnet,              │
       │    MPC signs Ed25519, backend compiles       │
       │                       │                      │
═══════╪═══════════════════════╪══════════════════════╪═══
       │                       │                      │
Week 2-3 (April 13-26) — CORE BACKEND                │
═══════════════════════════════════════════════════════╪═══
       │                       │                      │
       ▼                       ▼                      ▼
  ┌────────────────────────────────────────────────────┐
  │              Epic #4: Temporal Workflow             │
  │  #28 Config  │  #29 MPC Client  │  #30 Solana Svc │
  │  #31 Activities  │  #32 Workflow Implementation    │
  └──────────────────────┬─────────────────────────────┘
                         │
                         ▼
  ┌────────────────────────────────────────────────────┐
  │              Epic #5: REST API                     │
  │  #37 Stubs (EARLY!)  ──────────────────┐           │
  │  #33 Wallet  │  #34 Remittance  │  #35 │Claims    │
  │  #36 FX Rate                           │           │
  └──────────────────────┬─────────────────┼───────────┘
                         │                 │
       MILESTONE: Full lifecycle via curl  │
                         │                 │
═════════════════════════╪═════════════════╪═══════════
                         │                 │
Week 3-4 (April 20 - May 3) — FRONTENDS   │ (parallel)
═════════════════════════╪═════════════════╪═══════════
                         │                 │
                         ▼                 ▼
  ┌──────────────────────┐  ┌──────────────────────────┐
  │  Epic #6: Mobile App │  │  Epic #7: Claim Page     │
  │  #38 Expo Setup      │  │  #42 Next.js Setup       │
  │  #39 Onboarding/Home │  │  #43 Claim Page          │
  │  #40 Send Flow       │  │  #44 Claim Form          │
  │  #41 History/Detail  │  │  #45 Twilio SMS          │
  └──────────┬───────────┘  └──────────┬───────────────┘
             │                         │
    MILESTONE: Send from mobile,       │
    claim from web page                │
             │                         │
═════════════╪═════════════════════════╪═══════════════
             │                         │
Week 4-5 (April 27 - May 11) — INTEGRATION + DEMO
═══════════════════════════════════════════════════════
             │                         │
             ▼                         ▼
  ┌────────────────────────────────────────────────────┐
  │              Epic #8: E2E + Demo                   │
  │  #46 Devnet Setup Script                           │
  │  #47 Demo 1: Happy Path (SC1, SC4, SC5)            │
  │  #48 Demo 2: Guaranteed Delivery (SC3)             │
  │  #49 Demo 3: Sub-minute Settlement (SC2)           │
  │  #50 Docker Compose + Demo Script                  │
  │  #51 Video Recording + Submission                  │
  └────────────────────────────────────────────────────┘

  MILESTONE: All 5 success criteria demonstrable.
  SUBMISSION: May 11, 2026
```

## Team Assignment

| Person | Week 1 | Week 2-3 | Week 3-4 | Week 4-5 |
|--------|--------|----------|----------|----------|
| **Builder (you)** | #3 Backend scaffolding | #4, #5 Temporal + API | Support | #8 Integration |
| **Teammate A (Solana)** | #1 Anchor escrow | Anchor testing + devnet | #7 Claim page | Demo polish |
| **Teammate B (Frontend)** | #2 MPC sidecar | MPC Java integration | #6 Mobile app | Demo video |

## Critical Path

```
#23 → #24 → #25 → #28 → #31 → #32 → #34 → #37(stubs) → #39 → #40 → #47
 │                                                          │
 └─ Backend setup                                           └─ Frontend
```

The critical path runs through: backend scaffolding → domain models → persistence → Temporal config → activities → workflow → API → mobile app → E2E demo.

**Key de-risking:**
- #37 (API stubs) created early in Week 2 to unblock frontend (#38-#44)
- #17 (EdDSA keygen spike) validated by April 8 — pivot to custodial if needed
- #15 (Anchor deploy to devnet) completes Week 1 — available for backend integration

## Milestones

| Milestone | Due | Key Deliverable |
|-----------|-----|-----------------|
| Phase 1: Foundation | April 12 | Escrow on devnet, MPC signs Ed25519, backend compiles |
| Phase 2: Core Backend | April 26 | Full remittance lifecycle via curl |
| Phase 3: Frontends | May 3 | Send from mobile, claim from web |
| Phase 4: Integration + Demo | May 11 | All 5 SCs demonstrable, video submitted |

## Success Criteria

| SC | Description | Demo Scenario |
|----|-------------|---------------|
| SC1 | Working devnet E2E flow | #47 |
| SC2 | Sub-minute settlement | #49 |
| SC3 | Guaranteed delivery (failure + retry) | #48 |
| SC4 | No seed phrases | #47 |
| SC5 | Recipient claims via link | #47 |
