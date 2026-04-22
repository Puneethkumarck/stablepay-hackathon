# Solana Accounting Model: Where Every Dollar Goes

> A visual walkthrough of every wallet, PDA, and token account involved in a $25 remittance — from program deployment to the recipient receiving INR in their bank.

## The Players

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

## The Story of a $25 Remittance

### Act 0: Program Deployment (one-time — NOT per transaction)

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

### Act 1: Wallet Creation (MPC DKG)

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

### Act 2: Funding the Wallet

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

### Act 3: Escrow Deposit ($25 USDC)

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

### Act 4: SMS Notification

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

### Act 5: Recipient Claims

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

### Act 6: Escrow Release (Claim Transaction)

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

### Act 7: INR Disbursement

```
  Temporal Workflow                     Razorpay API
  ┌──────────────┐                     ┌──────────────────┐
  │ disburseInr  │────── API call ────►│ Create Payout    │
  │ activity     │                     │ Send ₹2,336 to   │
  └──────────────┘                     │ raj@upi via UPI  │
                                       └──────────────────┘
  Status: CLAIMED → DELIVERED ✅
```

## Final Ledger

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

## Alternative Path: Refund (No Claim Within 48h)

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

## Key Insight: Why PDAs?

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
