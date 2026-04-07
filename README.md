# StablePay

[![CI](https://github.com/Puneethkumarck/stablepay-hackathon/actions/workflows/ci.yml/badge.svg)](https://github.com/Puneethkumarck/stablepay-hackathon/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://jdk.java.net/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-8-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Temporal](https://img.shields.io/badge/Temporal-1.29.5-000000?logo=temporal&logoColor=white)](https://temporal.io/)
[![Solana](https://img.shields.io/badge/Solana-devnet-9945FF?logo=solana&logoColor=white)](https://solana.com/)
[![Anchor](https://img.shields.io/badge/Anchor-0.32.1-blue)](https://www.anchor-lang.com/)
[![Rust](https://img.shields.io/badge/Rust-stable-DEA584?logo=rust&logoColor=white)](https://www.rust-lang.org/)
[![Go](https://img.shields.io/badge/Go-1.26-00ADD8?logo=go&logoColor=white)](https://go.dev/)
[![React Native](https://img.shields.io/badge/React%20Native-Expo%20SDK%2052-61DAFB?logo=react&logoColor=white)](https://reactnative.dev/)
[![Next.js](https://img.shields.io/badge/Next.js-latest-000000?logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Twilio](https://img.shields.io/badge/Twilio-SMS-F22F46?logo=twilio&logoColor=white)](https://www.twilio.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Instant cross-border remittances on Solana. No seed phrases. No app for recipients. Guaranteed delivery.**

StablePay is a consumer-facing remittance application for the **USD to INR** corridor, built on USDC/Solana. It combines MPC wallet abstraction, a custom Anchor escrow program, and Temporal durable workflows to deliver a seamless sender-to-recipient experience where the recipient claims funds via an SMS link — no crypto knowledge required.

> Built for the [Colosseum Frontier Hackathon](https://www.colosseum.org/) (April 6 - May 11, 2026) presented by the Solana Foundation.

---

## Why StablePay?

18 million Indians in the US send **$125 billion** home annually. Banks charge 3-5% and take 1-3 days. Even Wise charges 1-2% and takes hours. Crypto rails can settle in seconds for under a penny — but today's stablecoin apps still expect users to manage wallets, seed phrases, and gas fees.

StablePay removes all of that.

| Problem | StablePay's Answer |
|---|---|
| Seed phrases are confusing and risky | MPC threshold signing — keys are split, never assembled |
| Recipients need wallets and apps | Claim via SMS link + web page, enter UPI ID |
| Transactions can fail silently | Temporal state machine with automatic retry and refund |
| Settlement takes hours or days | USDC on Solana — sub-second finality, <$0.01 fees |

---

## How It Works

```
Sender                          System                           Recipient
  |                               |                                |
  |  1. Sign up (email/phone)     |                                |
  |  ---- MPC key generation ---> |                                |
  |  <-- Solana wallet created -- |                                |
  |                               |                                |
  |  2. Fund wallet (demo USDC)   |                                |
  |  -----------------------------> Treasury transfer              |
  |                               |                                |
  |  3. Send $100 to +91...       |                                |
  |  ----> Lock FX rate (84.50)   |                                |
  |  ----> Reserve balance        |                                |
  |  ----> Create remittance      |                                |
  |        Start Temporal workflow |                                |
  |                               |                                |
  |        [SIGNING]              |                                |
  |        MPC signs escrow tx    |                                |
  |        [SUBMITTING]           |                                |
  |        USDC locked in PDA     |                                |
  |        Status: ESCROWED       |                                |
  |                               |                                |
  |                               |  4. SMS with claim link -----> |
  |                               |                                |
  |                               |     5. Opens web page          |
  |                               |     Sees: $100 = INR 8,450     |
  |                               |     Enters UPI ID              |
  |                               |  <---- Submit claim            |
  |                               |                                |
  |        [CLAIMING]             |                                |
  |        Release escrow on-chain|                                |
  |        [DELIVERING]           |                                |
  |        INR disbursement       |                                |
  |        Status: DELIVERED      |                                |
  |                               |                                |
  |  <-- "Delivered" notification |  <-- Confirmation page         |
```

### State Machine

```
INITIATED -----> ESCROWED -----> CLAIMED -----> DELIVERED
    |                |
    v                v (48h timeout)
  FAILED          REFUNDED
```

Every remittance is driven by a **Temporal durable workflow** — if the process crashes at any point, it resumes exactly where it left off. After 48 hours without a claim, funds automatically return to the sender.

---

## Architecture

StablePay follows **Hexagonal Architecture** (Ports & Adapters) with **Domain-Driven Design** and **Event-Driven** orchestration via Temporal.

```
                    +------------------+
                    |   Mobile App     |   React Native + Expo
                    |   (Sender)       |
                    +--------+---------+
                             |
                    +--------v---------+
                    |  REST API Layer   |   Spring MVC Controllers
                    |  /api/wallets    |   + MapStruct mappers
                    |  /api/remittances|   + Jakarta Validation
                    |  /api/fx         |
                    |  /api/claims     |
                    +--------+---------+
                             |
              +--------------v--------------+
              |       Domain Layer          |
              |  Handlers (use cases)       |
              |  Models (immutable records) |   Zero framework dependencies
              |  Ports (interfaces)         |
              |  Exceptions (SP-XXXX)       |
              +-+-----+-----+-----+-----+--+
                |     |     |     |     |
          +-----v-+ +-v---+ +-v--+ +--v--+ +--v--------+
          |  JPA  | | MPC | |Sol | | FX  | | Temporal  |
          |Postgres| |gRPC| |ana | |Rate | | Workflows |
          |Flyway | |Side | |RPC | |API  | | Activities|
          +-------+ |car  | +----+ +-----+ +-----------+
                     +-----+
                       |
              +--------v--------+
              | MPC Sidecar x2  |   Go + bnb-chain/tss-lib (fystack fork)
              | Ed25519 DKG     |   2-of-2 threshold signing
              | Transaction Sign|   gRPC + P2P coordination
              +-----------------+

              +------------------+
              |  Anchor Escrow   |   Rust / Anchor 0.32
              |  deposit/claim   |   PDA-based USDC escrow
              |  refund/cancel   |   On Solana devnet
              +------------------+

              +------------------+
              |  Web Claim Page  |   Next.js + shadcn/ui
              |  (Recipient)     |   No wallet required
              +------------------+
```

### Dependency Rule

```
domain --> nothing
application --> domain
infrastructure --> domain
```

The domain layer has minimal Spring imports — only `@Service`, `@Transactional`, and Spring Data pagination types (`Page`, `Pageable`). All external system access goes through domain-defined ports, implemented by infrastructure adapters.

---

## Tech Stack

| Component | Technology | Version |
|---|---|---|
| **Backend** | Java + Spring Boot (MVC + JPA) | 25 / 4.0.5 |
| **Database** | PostgreSQL + Flyway | 18 / 12.3 |
| **Cache** | Redis | 8 |
| **Workflows** | Temporal | 1.29.5 |
| **On-chain Program** | Rust + Anchor | 0.32.1 |
| **Blockchain** | Solana (devnet) | 2.2.7 |
| **Solana SDK (Java)** | sol4k | 0.7.0 |
| **MPC Sidecar** | Go + bnb-chain/tss-lib (fystack fork) | 1.26 |
| **Mobile** | React Native + Expo | SDK 52 |
| **Web Claim** | Next.js + shadcn/ui | - |
| **SMS** | Twilio | 11.3.6 |
| **Mapping** | MapStruct | 1.6.3 |
| **Testing** | JUnit 5, BDDMockito, AssertJ, ArchUnit, TestContainers | - |
| **Build** | Gradle (Kotlin DSL) | 9.4.1 |
| **CI** | GitHub Actions (7 parallel jobs) | - |

---

## Monorepo Structure

```
stablepay-hackathon/
├── programs/stablepay-escrow/     # Anchor program (Rust)
│   └── src/
│       ├── lib.rs                 # Thin dispatcher
│       ├── instructions/          # deposit, claim, refund, cancel
│       ├── state/                 # Escrow account struct
│       ├── errors.rs              # EscrowError enum
│       └── constants.rs           # PDA seeds
│
├── backend/                       # Spring Boot API (Java 25)
│   └── src/main/java/com/stablepay/
│       ├── application/           # Controllers, DTOs, config
│       ├── domain/                # Models, handlers, ports, exceptions
│       │   ├── wallet/            #   MPC wallet management
│       │   ├── remittance/        #   Core remittance flow
│       │   ├── claim/             #   SMS claim tokens
│       │   ├── fx/                #   FX rate quotes
│       │   └── common/            #   Shared ports
│       └── infrastructure/        # Adapters
│           ├── db/                #   JPA + Flyway
│           ├── temporal/          #   Workflow orchestration
│           ├── mpc/               #   gRPC to MPC sidecar
│           ├── solana/            #   Solana RPC + escrow instructions
│           ├── fx/                #   ExchangeRate-API adapter
│           └── sms/               #   Twilio SMS
│
├── mpc-sidecar/                   # MPC threshold signing (Go)
│   └── internal/
│       ├── tss/                   # DKG + EdDSA signing
│       ├── p2p/                   # Ceremony coordination
│       └── server/                # gRPC endpoint
│
├── mobile/                        # Sender app (React Native + Expo)
├── web-claim/                     # Recipient claim page (Next.js)
│
├── docker-compose.yml             # Full dev stack (7 services)
├── Makefile                       # Build + orchestration targets
├── Anchor.toml                    # Solana/Anchor config
└── docs/                          # Architecture docs, ADRs, standards
```

---

## Quick Start

### Prerequisites

| Tool | Version | Install |
|---|---|---|
| Java | 25 | `sdk install java 25-tem` |
| Docker + Docker Compose | latest | [docker.com](https://www.docker.com/) |
| Node.js | 22+ | `nvm install 22` |
| Solana CLI | 2.2.7 | `sh -c "$(curl -sSfL https://release.anza.xyz/v2.2.7/install)"` |
| Anchor CLI | 0.32.1 | [anchor-lang.com](https://www.anchor-lang.com/docs/installation) |
| Go | 1.26 | [go.dev](https://go.dev/dl/) |
| Rust | stable | `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \| sh` |

### Option 1: Full Stack (Docker Compose)

```bash
# Start everything — PostgreSQL, Redis, Temporal, MPC sidecars, backend
make up

# View service URLs
make urls
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

### Option 2: Infrastructure + Local Backend

```bash
# Start only infrastructure (Postgres, Redis, Temporal, MPC sidecars)
make infra

# Run backend locally with hot reload
cd backend && ./gradlew bootRun
```

### Option 3: Individual Components

```bash
# Backend
cd backend && ./gradlew build        # compile + format + tests
cd backend && ./gradlew bootRun      # run locally

# Anchor program
anchor build                          # compile
anchor test                           # test on localnet
anchor deploy                         # deploy to devnet

# MPC sidecar
cd mpc-sidecar && go build ./...     # compile
cd mpc-sidecar && go test ./...      # test

# Mobile app
cd mobile && npx expo start          # dev server

# Web claim page
cd web-claim && npm run dev           # dev server
```

### Makefile Targets

| Target | Description |
|---|---|
| `make up` | Build backend JAR + start full Docker Compose stack |
| `make down` | Stop all services |
| `make restart` | Restart the full stack |
| `make infra` | Start infrastructure only (for local backend dev) |
| `make logs` | Follow Docker Compose logs |
| `make ps` | Show running services |
| `make urls` | Print all service URLs |
| `make clean` | Stop all services and remove volumes |

---

## API Reference

Base URL: `http://localhost:8080`

### Wallets

```http
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
  "totalBalance": 0.000000
}
```

```http
POST /api/wallets/{id}/fund
Content-Type: application/json

{ "amount": 100.00 }
```

### FX Rates

```http
GET /api/fx/USD-INR
```

```json
{
  "rate": 84.500000,
  "source": "open.er-api.com",
  "timestamp": "2026-04-07T10:00:00Z",
  "expiresAt": "2026-04-07T10:01:00Z"
}
```

### Remittances

```http
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
  "expiresAt": "2026-04-09T10:00:00Z"
}
```

```http
GET /api/remittances/{remittanceId}
GET /api/remittances?senderId=user-123&page=0&size=10
```

### Claims

```http
GET  /api/claims/{token}
POST /api/claims/{token}
Content-Type: application/json

{ "upiId": "recipient@upi" }
```

### Swagger UI

Full interactive documentation available at: `http://localhost:8080/swagger-ui.html`

---

## On-Chain Escrow Program

**Program ID:** `6G9X8RArxw6f6n41wRKZMsgzRtHuUgPSkYipyjQu8NXD`

The custom Anchor program manages USDC escrow with four instructions:

| Instruction | Who Can Call | What It Does |
|---|---|---|
| `deposit` | Sender (MPC-signed) | Lock USDC in PDA vault, set 48h deadline |
| `claim` | Backend (claim authority) | Release USDC to recipient, close accounts |
| `refund` | Anyone (after deadline) | Return USDC to sender, close accounts |
| `cancel` | Sender only | Cancel before claim, return USDC |

### Escrow Account

```rust
pub struct Escrow {
    pub sender: Pubkey,           // Sender wallet
    pub claim_authority: Pubkey,  // Backend authority key
    pub mint: Pubkey,             // USDC mint
    pub amount: u64,              // Locked amount
    pub deadline: i64,            // Expiry timestamp
    pub status: EscrowStatus,     // Active | Claimed | Refunded | Cancelled
    pub bump: u8,                 // PDA bump
    pub remittance_id: Pubkey,    // Off-chain link
}
```

### PDA Derivation

```
Escrow: seeds = ["escrow", remittance_id]
Vault:  seeds = ["vault", escrow_pubkey]
```

### Security

- Account validation via typed Anchor constraints (`Account<'info, T>`, `Signer<'info>`)
- PDA bump stored and reused (canonical bump)
- Checked arithmetic on all amounts
- Account closure on terminal states (prevents double-claim, reclaims rent)
- Token mint and owner validation on every instruction

---

## MPC Wallet System

StablePay uses **2-of-2 threshold signing** (Ed25519) so that no single party ever holds a complete private key.

```
                +-------------------+
                |  Backend (Java)   |
                |  MpcWalletClient  |
                +--------+----------+
                         | gRPC
              +----------+----------+
              |                     |
     +--------v-------+   +--------v-------+
     | MPC Sidecar 0  |   | MPC Sidecar 1  |
     | Party ID: 0     |   | Party ID: 1     |
     | gRPC: 50051    |   | gRPC: 50052    |
     | P2P:  7000     |   | P2P:  7001     |
     +--------+-------+   +--------+-------+
              |      P2P (TCP)      |
              +---------------------+
```

**Key Generation (DKG):** Both sidecars participate in a distributed key generation ceremony to produce an Ed25519 public key. Each sidecar holds only its key share — the full private key never exists in memory.

**Transaction Signing:** When a remittance needs an on-chain transaction, the backend sends the unsigned transaction bytes to the MPC sidecars. They coordinate a signing ceremony over P2P to produce a valid Ed25519 signature — again, without reconstructing the full key.

---

## Temporal Workflows

The remittance lifecycle is orchestrated as a **durable Temporal workflow** with the following guarantees:

- **Exactly-once execution** — if the process crashes, it resumes from the last completed activity
- **Automatic retry** — Solana activities retry 3 times with exponential backoff (2s initial, 2x multiplier)
- **48-hour claim window** — workflow awaits a claim signal with timeout; auto-refunds on expiry
- **Observability** — full workflow history visible in Temporal UI at `http://localhost:8088`

### Workflow Activities

| Activity | Timeout | Retries | Description |
|---|---|---|---|
| `depositEscrow` | 60s | 3 | Build + MPC-sign + submit escrow deposit |
| `sendClaimSms` | 30s | 3 | Send SMS via Twilio with claim link |
| `releaseEscrow` | 60s | 3 | Release USDC from escrow to recipient |
| `simulateInrDisbursement` | 30s | 3 | Simulated INR payout (mock for hackathon) |
| `refundEscrow` | 60s | 3 | Return USDC to sender after timeout |
| `updateRemittanceStatus` | 10s | 3 | Update status in PostgreSQL |

### Re-Sign on Retry

Solana blockhashes expire in ~60 seconds. If a transaction submission fails and needs retry, the workflow requests a **fresh MPC signature** with a new blockhash rather than replaying the stale one.

---

## Testing

### Backend

```bash
cd backend

# All tests + formatting
./gradlew build

# Unit tests only
./gradlew test

# Integration tests (requires Docker for TestContainers)
./gradlew integrationTest

# Format code (Spotless)
./gradlew spotlessApply
```

**Testing standards enforced:**
- **BDDMockito only** — `given()`/`then()`, never `when()`/`verify()`
- **Recursive comparison** — build expected object, single `assertThat().usingRecursiveComparison().isEqualTo(expected)`
- **No generic matchers** — real values only, never `any()` or `anyString()`
- **ArchUnit** — enforces hexagonal layer boundaries at compile time
- **TestContainers** — integration tests run against real PostgreSQL

### Anchor Program

```bash
# TypeScript E2E tests on localnet
anchor test
```

### MPC Sidecar

```bash
cd mpc-sidecar
go test ./... -v -count=1 -timeout 120s
```

---

## CI/CD

GitHub Actions runs **7 jobs** on every push to `main` and every PR:

```
                 +----------+
                 | Spotless |  (format check)
                 +----+-----+
                      |
              +-------+-------+
              |               |
        +-----v----+   +-----v--------+
        |Unit Tests|   |Integration   |
        |          |   |Tests         |
        +-----+----+   +-----+--------+
              |               |
              +-------+-------+
                      |
                 +----v---+
                 | Build  |  (assemble JAR)
                 +--------+

  +----------------+     +--------------+
  | MPC Sidecar    |     | Anchor Build |
  | Tests (Go)     |     | (Rust)       |
  +----------------+     +------+-------+
                                |
                         +------v-------+
                         | Anchor Tests |
                         | (localnet)   |
                         +--------------+
```

---

## Error Codes

All API errors return a structured response with an `SP-XXXX` error code:

| Code | HTTP | Description |
|---|---|---|
| SP-0002 | 400 | Insufficient wallet balance |
| SP-0003 | 400 | Request validation failure |
| SP-0006 | 404 | Wallet not found |
| SP-0007 | 503 | Treasury depleted |
| SP-0008 | 409 | Wallet already exists for user |
| SP-0009 | 400 | Unsupported currency corridor |
| SP-0010 | 404 | Remittance not found |
| SP-0011 | 404 | Claim token not found |
| SP-0012 | 409 | Claim already submitted |
| SP-0013 | 410 | Claim token expired |
| SP-0014 | 409 | Invalid remittance state transition |

---

## Architecture Decisions

Key decisions are documented in [docs/ADR.md](docs/ADR.md). Highlights:

| # | Decision | Rationale |
|---|---|---|
| ADR-003 | Spring MVC (blocking) over WebFlux | Temporal activities are blocking by design |
| ADR-005 | Temporal for orchestration | Durable execution, built-in retry/timers, crash recovery |
| ADR-006 | Custom Anchor escrow | Demonstrates Solana depth, full control over lifecycle |
| ADR-007 | Own MPC via forked tss-lib | Real threshold signing, not a third-party wrapper |
| ADR-008 | 2-of-2 threshold | Simplest real threshold that demonstrates the concept |
| ADR-009 | Treasury pre-funding | Reliable demo without Stripe sandbox complexity |
| ADR-011 | Bearer token claim links | Frictionless — no login, no app download |
| ADR-015 | ExchangeRate-API + Redis cache | Free tier sufficient, 60s TTL, hardcoded fallback (84.50) |
| ADR-016 | 48-hour escrow expiry | Accommodates US-India timezone gap (up to 13.5h) |
| ADR-021 | Pessimistic locking (SELECT FOR UPDATE) | Prevents double-spend on concurrent transactions |

---

## Hackathon Success Criteria

| # | Criteria | How We Demonstrate It |
|---|---|---|
| SC1 | Working E2E flow on devnet | Send $100 from mobile, claim on web, USDC moves on-chain |
| SC2 | Sub-minute settlement | Escrow deposit + claim release in <60s |
| SC3 | Guaranteed delivery | Kill the backend mid-flow, restart, watch Temporal resume |
| SC4 | No seed phrases | Wallet created via MPC DKG — user never sees a key |
| SC5 | Recipient claims via link | SMS link opens web page, enter UPI ID, done |

---

## Development Workflow

```bash
# 1. Pick an issue (STA-N)
# 2. Create branch
git checkout -b feature/STA-42-add-claim-page

# 3. Implement + test
cd backend && ./gradlew build

# 4. Format before commit
cd backend && ./gradlew spotlessApply

# 5. Push
git push -u origin feature/STA-42-add-claim-page

# 6. Create PR
gh pr create --title "STA-42: Add claim page" --body "Closes #42"
```

**Branch naming:** `feature/STA-{N}-description`
**Commit messages:** `feat(STA-{N}): description` (conventional commits)
**PR titles:** `STA-{N}: Description`

Never commit directly to `main`. See [CONTRIBUTING.md](CONTRIBUTING.md) for full conventions.

---

## Documentation

| Document | Description |
|---|---|
| [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) | Java/Spring Boot conventions |
| [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) | Testing rules (BDDMockito, recursive comparison) |
| [SOLANA_CODING_STANDARDS.md](docs/SOLANA_CODING_STANDARDS.md) | Anchor program standards |
| [ADR.md](docs/ADR.md) | 21 architecture decision records |
| [E2E_FLOW.md](docs/E2E_FLOW.md) | Complete end-to-end user journey |
| [ROADMAP.md](docs/ROADMAP.md) | 5-week implementation timeline |
| [COMPETITIVE_ANALYSIS.md](docs/COMPETITIVE_ANALYSIS.md) | Market positioning analysis |
| [BRAINSTORM.md](docs/BRAINSTORM.md) | Market research and strategy |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution workflow |

---

## Design Principles

1. **One corridor, one persona** — USD to INR, consumer senders. Not "payments for everyone."
2. **Wallet abstraction** — MPC Ed25519 threshold signing. No seed phrases, no browser extensions, no gas management.
3. **Guaranteed delivery** — Temporal state machine with automatic retry. Every remittance reaches a terminal state (delivered or refunded).
4. **Recipient doesn't need an app** — Claim via SMS link and a web page. Enter a UPI ID. That's it.
5. **Demo on devnet** — A working end-to-end flow, not a slide deck.
