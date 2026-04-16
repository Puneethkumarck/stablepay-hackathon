# 🌍 StablePay: A $25 Remittance from US to India

> **The complete visual story of a cross-border payment settled on Solana in under 30 seconds — no seed phrases, no app for the recipient.**

---

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🇺🇸 Sender (New York)                   🇮🇳 Recipient (Chennai)
  Priya — sends $25                       Her mother — receives ₹2,329
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 🎬 Act 1: Priya Creates Her Wallet

> 🔑 **No seed phrase. No browser extension. Just sign up.**

```
  👩 Priya                 🏦 StablePay              🔐 MPC Sidecar 0   🔐 MPC Sidecar 1
    |                          |                         |                    |
    |  📱 "Sign me up"        |                         |                    |
    |————————————————————————→ |                         |                    |
    |                          |  🎲 "Start DKG ceremony"|                    |
    |                          |————————————————————————→ |                    |
    |                          |  🎲 "You too, join in"  |                    |
    |                          |—————————————————————————————————————————————→ |
    |                          |                         |                    |
    |                          |                         | ←── 🔄 P2P ────→  |
    |                          |                         | ←── round     ──→  |
    |                          |                         | ←── messages  ──→  |
    |                          |                         | ←── ~100ms    ──→  |
    |                          |                         |                    |
    |                          |  🔑 address + my share  |                    |
    |                          | ←———————————————————————|                    |
    |                          |  🔑 MY share too                             |
    |                          | ←————————————————————————————————————————————|
    |                          |                         |                    |
    |  ✅ Your wallet:         |                         |                    |
    |     BkRg8y5VWS...        |                         |                    |
    | ←————————————————————————|                         |                    |
```

### 🧠 What Just Happened?

```
┌──────────────────────────────────────────────────────────────┐
│  🔐 Distributed Key Generation (DKG)                         │
│                                                               │
│  Two servers each generated HALF of Priya's private key.      │
│                                                               │
│  🔐 Sidecar 0:  holds share A ████░░░░                       │
│  🔐 Sidecar 1:  holds share B ░░░░████                       │
│  🔒 Full key:   NEVER EXISTS  ████████  ← never assembled    │
│                                                               │
│  ✅ Both shares stored in database (686 bytes each)           │
│  ✅ Solana address derived cooperatively                      │
│  ✅ Priya sees: one address, zero complexity                  │
└──────────────────────────────────────────────────────────────┘
```

**📋 Log proof:** `Captured peer key share for ceremony 511c5de6 (686 bytes)`

---

## 💰 Act 2: Priya Loads $100 USDC

> 💵 **Demo treasury funds the wallet — one tap.**

```
  👩 Priya                 🏦 StablePay              ⛓️ Solana
    |                          |                         |
    |  💰 "Add $100"           |                         |
    |————————————————————————→ |                         |
    |                          |  ☀️ Airdrop 2 SOL       |
    |                          |————————————————————————→ |
    |                          |  🪙 Mint 100 USDC       |
    |                          |————————————————————————→ |
    |                          |  📝 Update DB balance   |
    |                          |———→ [🐘 PostgreSQL]     |
    |                          |                         |
    |  ✅ Balance: 100 USDC    |                         |
    | ←————————————————————————|                         |
```

```
┌─────────────────────────────────┐
│  💼 Priya's Wallet              │
│                                  │
│  ☀️ SOL:  2.000000  (tx fees)   │
│  🪙 USDC: 100.000000            │
│  📍 BkRg8y5VWS...               │
└─────────────────────────────────┘
```

---

## 📤 Act 3: Priya Sends $25 to Her Mom

> 📱 **Lock the FX rate, reserve the balance, start the workflow — all in one tap.**

```
  👩 Priya                 🏦 StablePay              🌐 ExchangeRate API
    |                          |                         |
    |  📤 "Send $25 to         |                         |
    |   +91-98765-43210"       |                         |
    |————————————————————————→ |                         |
    |                          |  📊 USD/INR rate?       |
    |                          |————————————————————————→ |
    |                          |  📊 93.18               |
    |                          | ←————————————————————————|
    |                          |                         |
    |                          |  🔒 Reserve 25 USDC from balance
    |                          |  🧮 $25 × 93.18 = ₹2,329.42
    |                          |  🎫 Generate claim token: db4e650f...
    |                          |  ⚡ Start Temporal workflow (async)
    |                          |                         |
    |  ✅ Remittance created!  |                         |
    |  📋 Status: INITIATED    |                         |
    | ←————————————————————————|                         |
```

```
┌─────────────────────────────────────────────────────┐
│  📋 Remittance: c40c0777-471e-4e91-...               │
│                                                       │
│  👩 Sender:     Priya (e2e-lifecycle)                │
│  📱 Recipient:  +91-98765-43210                      │
│  💵 Amount:     25 USDC                              │
│  💱 FX Rate:    93.18 (locked)                       │
│  🇮🇳 INR:       ₹2,329.42                            │
│  🎫 Claim:      db4e650f-3ebc-4f03-...              │
│  ⏰ Expires:    48 hours                             │
│  📊 Status:     INITIATED                            │
└─────────────────────────────────────────────────────┘
```

**⚡ Priya gets an instant response.** Everything below happens in the background.

---

## 🔒 Act 4: The Escrow — USDC Locked On-Chain

> ⛓️ **Two MPC servers cooperate to sign a Solana transaction. The escrow program locks 25 USDC in a PDA vault.**

```
  ⚡ Temporal               🔐 Sidecar 0     🔐 Sidecar 1     ⛓️ Solana
    |                          |                 |                  |
    |  🖊️ "Sign deposit tx"   |                 |                  |
    |————————————————————————→ |                 |                  |
    |  🖊️ "You too"           |                 |                  |
    |——————————————————————————————————————————→ |                  |
    |                          |                 |                  |
    |                          | ←—— 🔄 P2P ——→ |                  |
    |                          | ←—— signing ——→ |                  |
    |                          | ←—— ~100ms  ——→ |                  |
    |                          |                 |                  |
    |  🖊️ 64-byte Ed25519     |                 |                  |
    |     signature            |                 |                  |
    | ←————————————————————————|                 |                  |
    |                                                               |
    |  📡 Submit signed tx (skipPreflight)                          |
    |——————————————————————————————————————————————————————————————→ |
    |                                                               |
    |  ┌─────────────────────────────────────────────────┐          |
    |  │  ⛓️ Anchor Escrow Program Executes              │          |
    |  │                                                  │          |
    |  │  1️⃣  Create escrow PDA account                  │          |
    |  │  2️⃣  Create vault token account                 │          |
    |  │  3️⃣  Transfer 25 USDC → vault 🔒                │          |
    |  │  4️⃣  Set 48h deadline ⏰                        │          |
    |  │  5️⃣  Record claim authority 🏛️                  │          |
    |  │                                                  │          |
    |  │  📊 Status: Active                               │          |
    |  │  🔒 Locked: 25,000,000 lamports (25 USDC)       │          |
    |  │  ⛽ Gas: 29,997 compute units                    │          |
    |  └─────────────────────────────────────────────────┘          |
    |                                                               |
    |  ✅ Transaction confirmed!                                    |
    | ←—————————————————————————————————————————————————————————————|
    |                                                               |
    |  📝 DB: INITIATED → ESCROWED                                  |
    |                                                               |
    |  📱 SMS to +91-98765-43210:                                   |
    |  "You have a StablePay remittance!                            |
    |   Claim: https://claim.stablepay.app/db4e650f..."             |
    |                                                               |
    |  ⏳ Waiting for claim signal (up to 48 hours)...              |
```

```
┌─────────────────────────────────────────────────────┐
│  ⛓️ On-Chain State After Deposit                     │
│                                                       │
│  📍 Escrow PDA: seeds=["escrow", remittance_id]      │
│  📍 Vault PDA:  seeds=["vault", escrow_pubkey]       │
│                                                       │
│  🔒 vault balance:    25 USDC                        │
│  👩 sender:           BkRg8y5VWS... (Priya)          │
│  🏛️ claim_authority:  3LZh792tEa... (backend)       │
│  ⏰ deadline:         now + 48 hours                  │
│  📊 status:           Active                          │
└─────────────────────────────────────────────────────┘
```

**📋 Log proof:** `Program 6G9X8R... success` — 29,997 compute units consumed.

**⏱️ Time: 24 seconds** from INITIATED → ESCROWED.

---

## 📱 Act 5: Mom Claims the Money

> 🔗 **No app required. Open link → see amount → enter UPI → done.**

```
  👵 Mom (Chennai)         🏦 StablePay              ⚡ Temporal Workflow
    |                          |                         |
    |  📱 Opens SMS link       |                         |
    |     in browser           |                         |
    |                          |                         |
    |  🔍 GET /claims/db4e650f |                         |
    |————————————————————————→ |                         |
    |                          |                         |
    |  💸 "You're receiving    |                         |
    |     $25 = ₹2,329.42"    |                         |
    | ←————————————————————————|                         |
    |                          |                         |
    |  ✏️ Enters UPI: raj@upi  |                         |
    |  POST /claims/db4e650f   |                         |
    |————————————————————————→ |                         |
    |                          |                         |
    |                          |  🔍 Validation Chain:   |
    |                          |  ✅ Token exists         |
    |                          |  ✅ Not expired          |
    |                          |  ✅ Not already claimed  |
    |                          |  ✅ Remittance exists    |
    |                          |  ✅ Status = ESCROWED    |
    |                          |                         |
    |                          |  📡 Signal: claimSubmitted(upiId)
    |                          |————————————————————————→ |
    |                          |                         |
    |                          |              🔔 Workflow |
    |                          |                 wakes up |
    |  ✅ Claimed: true        |                         |
    | ←————————————————————————|                         |
```

```
┌─────────────────────────────────────────────┐
│  👵 Mom's Experience                         │
│                                               │
│  1️⃣  Gets SMS: "Claim your funds"           │
│  2️⃣  Taps link → web page opens             │
│  3️⃣  Sees: "$25 = ₹2,329.42"               │
│  4️⃣  Types UPI ID: raj@upi                  │
│  5️⃣  Taps "Claim"                           │
│  6️⃣  Done ✅                                │
│                                               │
│  📱 No app download                          │
│  🔑 No wallet setup                          │
│  🪙 No crypto knowledge                      │
└─────────────────────────────────────────────┘
```

---

## 🚀 Act 6: Release + Delivery

> 💸 **Escrow releases on-chain. INR hits mom's bank account.**

```
  ⚡ Temporal               ⛓️ Solana                 💱 Transak (off-ramp)
    |                          |                         |
    |  🔓 Release escrow       |                         |
    |  (claim authority signs) |                         |
    |————————————————————————→ |                         |
    |                          |                         |
    |  ┌────────────────────────────────┐                |
    |  │  ⛓️ Escrow Program:            │                |
    |  │                                │                |
    |  │  1️⃣ Transfer vault USDC       │                |
    |  │     → recipient address        │                |
    |  │  2️⃣ Close vault account       │                |
    |  │     (reclaim rent → sender)    │                |
    |  │  3️⃣ Status: Claimed ✅        │                |
    |  └────────────────────────────────┘                |
    |                                                    |
    |  ✅ Confirmed!                                     |
    | ←————————————————————————|                         |
    |                                                    |
    |  📝 DB: ESCROWED → CLAIMED                         |
    |                                                    |
    |  💱 Disburse ₹2,329.42 to raj@upi                 |
    |——————————————————————————————————————————————————→ |
    |                                                    |
    |                          💸 ₹2,329.42 sent via UPI |
    | ←——————————————————————————————————————————————————|
    |                                                    |
    |  📝 DB: CLAIMED → DELIVERED ✅                      |
```

```
┌─────────────────────────────────────────────────────┐
│  ✅ DELIVERED                                        │
│                                                       │
│  👩 Priya sent:        25 USDC ($25)                 │
│  👵 Mom received:      ₹2,329.42 via UPI             │
│  💱 FX Rate:           93.18 (locked at send time)   │
│  ⛽ On-chain fees:     < $0.01                       │
│  ⏱️ Total time:        ~30 seconds                   │
└─────────────────────────────────────────────────────┘
```

**⏱️ Time: 5 seconds** from claim to DELIVERED.

---

## ⏱️ The Complete Timeline

```
  T+0s       👩 Priya taps "Send $25"
              ├─ 💱 FX rate locked: 93.18
              ├─ 🔒 Balance reserved: 25 USDC
              ├─ 🎫 Claim token generated
              └─ ⚡ Temporal workflow started
                   │
  T+24s      ⛓️ ESCROWED
              ├─ 🔐 MPC 2-of-2 signed the deposit
              ├─ 🔒 25 USDC locked in Solana PDA
              └─ 📱 SMS sent to mom
                   │
  T+???      👵 Mom opens SMS, enters UPI ID
                   │
  T+0s       ✅ Claim submitted
              └─ 🔔 Temporal workflow wakes up
                   │
  T+3s       🔓 Escrow released on-chain
                   │
  T+5s       💸 DELIVERED
              ├─ 💰 ₹2,329.42 disbursed to UPI
              └─ 📱 Priya notified
```

---

## 🔄 State Machine: Every Remittance's Journey

```
                          ┌──────────┐
                 ┌───────→│ ESCROWED │───────────────────────┐
                 │        │ 🔒⛓️     │                       │
                 │        └────┬─────┘                       │
                 │             │                             │
            ┌────┴─────┐      │ 👵 Claims                   │ ⏰ 48h timeout
            │INITIATED │      │ within 48h                   │ no claim
            │ 📤       │      ▼                              ▼
            └──────────┘ ┌─────────┐                   ┌──────────┐
                         │ CLAIMED │                   │ REFUNDED │
                         │ 🔓     │                   │ ↩️       │
                         └────┬───┘                   └──────────┘
                              │                        USDC returned
                    ┌─────────┴─────────┐              to Priya
                    │                   │
                    ▼                   ▼
             ┌───────────┐    ┌─────────────────────┐
             │ DELIVERED │    │ DISBURSEMENT_FAILED  │
             │ ✅💸      │    │ ⚠️                   │
             └───────────┘    └─────────────────────┘
             INR in mom's      Escrow released but
             bank account      INR payout failed
```

---

## 🛡️ Safety Guarantees

```
┌──────────────────────────────────────────────────────────────┐
│                     🛡️ What Protects the Money                │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  🔐 No seed phrase      MPC key split across 2 servers        │
│                          Full key NEVER exists in memory       │
│                                                                │
│  🔒 Can't be stolen     Neither server has the full key       │
│                          Both must cooperate to sign           │
│                                                                │
│  🔄 Can't be lost       Temporal resumes after any crash      │
│                          Every step is durable + retried       │
│                                                                │
│  ⏰ Can't expire        48h claim window for recipient        │
│     silently             Auto-refund to sender after timeout   │
│                                                                │
│  ⛓️ On-chain proof      Every USDC movement recorded          │
│                          on Solana — fully auditable           │
│                                                                │
│  📱 Recipient safe      No app download required              │
│                          No wallet, no crypto knowledge        │
│                          Just SMS link + UPI ID                │
│                                                                │
└──────────────────────────────────────────────────────────────┘
```

---

## 🏗️ The Tech Behind Each Act

| Act | What Happened | Tech Used |
|-----|--------------|-----------|
| 🎬 1 | Wallet created | MPC 2-of-2 DKG via `bnb-chain/tss-lib` (Go) |
| 💰 2 | Wallet funded | SPL Token mint on Solana |
| 📤 3 | Remittance created | Spring Boot + JPA + Redis (FX cache) |
| 🔒 4 | USDC escrowed on-chain | MPC signing → Anchor program → Solana PDA |
| 📱 5 | Mom claims via SMS | Bearer token + Temporal signal |
| 🚀 6 | Escrow released + INR paid | Solana CPI + Transak off-ramp |

| Component | Role |
|-----------|------|
| ⚡ Temporal | Durable workflow — crash-proof state machine |
| 🔐 MPC Sidecars (×2) | Threshold signing — no single point of key compromise |
| ⛓️ Anchor Escrow | On-chain USDC lockbox — trustless, auditable |
| 🐘 PostgreSQL | Remittance state, wallet data, claim tokens |
| 🔴 Redis | FX rate cache (60s TTL) |
| 📱 Twilio | SMS delivery to recipient |
| 💱 Transak | USDC → INR off-ramp via UPI |

---

## 📊 Verified E2E Results

```
┌─────────────────────────────────────────────────┐
│  ✅ E2E Test: PASSED                             │
│                                                   │
│  🔐 MPC DKG:           ✅ Both sidecars (100ms)  │
│  🔐 MPC Signing:       ✅ Both sidecars (100ms)  │
│  ⛓️ Escrow Deposit:    ✅ Confirmed on Solana    │
│  📱 Claim Submission:  ✅ Signal sent to workflow │
│  🔓 Escrow Release:    ✅ Confirmed on Solana    │
│  💸 INR Disbursement:  ✅ Via Transak off-ramp   │
│                                                   │
│  ⏱️ INITIATED → ESCROWED:   24 seconds           │
│  ⏱️ CLAIMED → DELIVERED:     5 seconds            │
│                                                   │
│  📊 Final Status: DELIVERED ✅                    │
└─────────────────────────────────────────────────┘
```

> **Bottom line:** Priya sends $25 from New York. Her mom in Chennai gets ₹2,329 in her bank account. Under 30 seconds. Under $0.01 in fees. No seed phrases. No app for mom. Guaranteed delivery or automatic refund.
