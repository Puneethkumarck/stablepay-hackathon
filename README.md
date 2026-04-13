# StablePay

[![CI](https://github.com/Puneethkumarck/stablepay-hackathon/actions/workflows/ci.yml/badge.svg)](https://github.com/Puneethkumarck/stablepay-hackathon/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://jdk.java.net/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Solana](https://img.shields.io/badge/Solana-devnet-9945FF?logo=solana&logoColor=white)](https://solana.com/)
[![Anchor](https://img.shields.io/badge/Anchor-0.32.1-blue)](https://www.anchor-lang.com/)
[![Go](https://img.shields.io/badge/Go-1.26-00ADD8?logo=go&logoColor=white)](https://go.dev/)

Cross-border stablecoin remittance on Solana. USD to INR corridor via USDC with MPC wallet abstraction and guaranteed delivery through Temporal workflows.

Senders create MPC-backed wallets, send USDC, and recipients claim funds via an SMS link -- no seed phrases, no app download, no crypto knowledge required.

> Built for the [Colosseum Frontier Hackathon](https://www.colosseum.org/) (April 6 -- May 11, 2026)

## How It Works

1. **Sender signs up** -- MPC key generation creates a Solana wallet (no seed phrase)
2. **Fund wallet** -- Treasury transfers demo USDC to the sender's wallet
3. **Send remittance** -- Lock FX rate, reserve balance, start Temporal workflow
4. **USDC escrowed on-chain** -- MPC-signed transaction deposits into Anchor escrow PDA
5. **Recipient gets SMS** -- Claim link with token, opens web page
6. **Recipient claims** -- Enters UPI ID, escrow releases, INR disbursement initiated
7. **Auto-refund** -- If unclaimed after 48 hours, USDC returns to sender

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    REST API (Spring MVC)                  │
│   /api/wallets  /api/remittances  /api/fx  /api/claims  │
└──────────────────────────┬──────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │      Domain Layer       │
              │  Handlers · Models      │
              │  Ports · Exceptions     │
              └──┬───┬───┬───┬───┬─────┘
                 │   │   │   │   │
     ┌───────┐ ┌┴─┐ ┌┴─┐ ┌┴┐ ┌─┴──────┐
     │  JPA  │ │MPC│ │Sol│ │FX│ │Temporal│
     │Postgres│ │gRPC│ │ana│ │API│ │Workflow│
     │Flyway │ │   │ │RPC│ │  │ │       │
     └───────┘ └─┬─┘ └──┘ └──┘ └───────┘
                 │
        ┌────────▼────────┐
        │ MPC Sidecar x2  │   Go + tss-lib
        │ 2-of-2 Ed25519  │   DKG + Signing
        └─────────────────┘

        ┌─────────────────┐
        │ Anchor Escrow   │   Rust / Solana
        │ deposit · claim │   PDA-based USDC
        │ refund · cancel │   escrow
        └─────────────────┘
```

**Dependency rule:** `domain` depends on nothing. `application` depends on `domain`. `infrastructure` depends on `domain`. Never the reverse.

## Installation

### Prerequisites

- Java 25 (`sdk install java 25-tem`)
- Docker + Docker Compose
- Go 1.26 (for MPC sidecar)
- Solana CLI 2.2.7 + Anchor CLI 0.32.1 (for on-chain program)
- Node.js 22+ (for Anchor tests)

### Full Stack

```bash
make up
```

Starts PostgreSQL, Redis, Temporal, two MPC sidecars, and the backend. Services available at:

| Service | URL |
|---|---|
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health Check | http://localhost:8080/actuator/health |
| Temporal UI | http://localhost:8088 |

### Infrastructure Only (Local Backend Dev)

```bash
make infra
cd backend && ./gradlew bootRun
```

### Individual Components

```bash
# Backend
cd backend && ./gradlew build

# Anchor program
anchor build
anchor test

# MPC sidecar
cd mpc-sidecar && go build ./...
cd mpc-sidecar && go test ./...
```

## API

Base URL: `http://localhost:8080`

Interactive docs: `http://localhost:8080/swagger-ui.html`

### Create Wallet

```bash
curl -X POST http://localhost:8080/api/wallets \
  -H "Content-Type: application/json" \
  -d '{"userId": "user-123"}'
```

### Fund Wallet

```bash
curl -X POST http://localhost:8080/api/wallets/1/fund \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00}'
```

### Get FX Rate

```bash
curl http://localhost:8080/api/fx/USD-INR
```

### Create Remittance

```bash
curl -X POST http://localhost:8080/api/remittances \
  -H "Content-Type: application/json" \
  -d '{"senderId": "user-123", "recipientPhone": "+919876543210", "amountUsdc": 100.00}'
```

### Get Remittance

```bash
curl http://localhost:8080/api/remittances/{remittanceId}
```

### List Remittances

```bash
curl "http://localhost:8080/api/remittances?senderId=user-123&page=0&size=10"
```

### Get Claim

```bash
curl http://localhost:8080/api/claims/{token}
```

### Submit Claim

```bash
curl -X POST http://localhost:8080/api/claims/{token} \
  -H "Content-Type: application/json" \
  -d '{"upiId": "recipient@upi"}'
```

A Postman collection is available at [`docs/StablePay.postman_collection.json`](docs/StablePay.postman_collection.json).

## On-Chain Escrow

Program ID: `6G9X8RArxw6f6n41wRKZMsgzRtHuUgPSkYipyjQu8NXD`

Custom Anchor program managing USDC escrow on Solana devnet with four instructions:

| Instruction | Caller | Description |
|---|---|---|
| `deposit` | Sender (MPC-signed) | Lock USDC in PDA vault, set 48h deadline |
| `claim` | Backend (claim authority) | Release USDC to recipient, close accounts |
| `refund` | Anyone (after deadline) | Return USDC to sender after expiry |
| `cancel` | Sender only | Cancel before claim, return USDC |

PDA seeds: `["escrow", remittance_id]` for escrow account, `["vault", escrow_pubkey]` for token vault.

## MPC Wallet

2-of-2 threshold signing (Ed25519) via two Go sidecars using `bnb-chain/tss-lib` (fystack fork). The full private key never exists in memory.

- **DKG**: Both sidecars run a distributed key generation ceremony to produce an Ed25519 public key
- **Signing**: Backend sends unsigned transaction bytes; sidecars coordinate over P2P to produce a valid signature
- **gRPC API**: `GenerateKey`, `Sign`, and `HealthCheck` RPCs defined in protobuf

## Temporal Workflows

The remittance lifecycle is a durable Temporal workflow:

| Activity | Timeout | Retries | Description |
|---|---|---|---|
| `depositEscrow` | 60s | 3 (2s initial, 2x backoff) | Build + MPC-sign + submit escrow deposit |
| `sendClaimSms` | 30s | 3 (5s initial, 2x backoff) | Send SMS via Twilio with claim link |
| `releaseEscrow` | 60s | 3 (2s initial, 2x backoff) | Release USDC from escrow to recipient |
| `disburseInr` | 45s | 1 (no retry) | INR payout via off-ramp |
| `refundEscrow` | 60s | 3 (2s initial, 2x backoff) | Return USDC to sender after timeout |
| `updateRemittanceStatus` | 10s | 3 (1s initial, 2x backoff) | Update status in PostgreSQL |

Solana blockhashes expire in ~60s, so retried transactions get a fresh MPC signature. Disbursement does not retry to prevent duplicate payouts.

Workflow visible at: http://localhost:8088

## Testing

```bash
# Backend: all tests + formatting
cd backend && ./gradlew build

# Unit tests only (34 test files)
cd backend && ./gradlew test

# Integration tests with TestContainers (6 test files)
cd backend && ./gradlew integrationTest

# Format code
cd backend && ./gradlew spotlessApply

# Anchor program tests (799 lines, TypeScript)
anchor test

# MPC sidecar tests
cd mpc-sidecar && go test ./... -v -count=1 -timeout 120s
```

## CI

GitHub Actions runs 7 jobs on every push to `main` and every PR:

| Job | Description |
|---|---|
| Spotless Check | Code formatting verification |
| Unit Tests | JUnit 5 tests (needs lint) |
| Integration Tests | TestContainers + PostgreSQL (needs lint) |
| Build | Assemble JAR (needs unit + integration) |
| MPC Sidecar Tests | Go build + test |
| Anchor Build | Compile Solana program |
| Anchor Tests | Run on localnet (needs anchor build) |

## Error Codes

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
| SP-0018 | 502 | INR disbursement failed |

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
| SMS | Twilio 11.3.6 |
| Mapping | MapStruct 1.6.3 |
| Resilience | Resilience4j 2.3.0 |
| API Docs | springdoc-openapi 3.0.2 |
| Build | Gradle 9.4.1 (Kotlin DSL), Jib |
| Testing | JUnit 5, BDDMockito, AssertJ, ArchUnit 1.4.1, TestContainers 1.21.4 |
| CI | GitHub Actions (7 jobs) |

## Project Structure

```
stablepay-hackathon/
├── backend/                          # Spring Boot API
│   └── src/
│       ├── main/java/com/stablepay/
│       │   ├── application/          # Controllers, DTOs, config
│       │   ├── domain/               # Models, handlers, ports
│       │   │   ├── wallet/
│       │   │   ├── remittance/
│       │   │   ├── claim/
│       │   │   ├── fx/
│       │   │   └── common/
│       │   └── infrastructure/       # Adapters
│       │       ├── db/               # JPA + Flyway (3 migrations)
│       │       ├── temporal/         # Workflow + activities
│       │       ├── mpc/              # gRPC client
│       │       ├── solana/           # RPC + escrow instructions
│       │       ├── fx/               # ExchangeRate-API
│       │       └── sms/              # Twilio
│       ├── test/                     # 34 unit test files
│       └── integration-test/         # 6 integration test files
├── programs/stablepay-escrow/        # Anchor program (Rust)
│   └── src/
│       ├── lib.rs
│       ├── instructions/             # deposit, claim, refund, cancel
│       ├── state/                    # Escrow account
│       ├── errors.rs
│       └── constants.rs
├── mpc-sidecar/                      # MPC threshold signing (Go)
│   ├── cmd/sidecar/                  # Entry point
│   ├── internal/
│   │   ├── tss/                      # DKG + signing
│   │   ├── p2p/                      # Ceremony coordination
│   │   ├── server/                   # gRPC endpoint
│   │   └── config/
│   └── proto/                        # Protobuf definitions
├── tests/                            # Anchor E2E tests (TypeScript)
├── docs/                             # Architecture, standards, ADRs
├── docker-compose.yml                # 7 services
├── Makefile                          # Build + orchestration
└── Anchor.toml
```

## Documentation

| Document | Description |
|---|---|
| [CODING_STANDARDS.md](docs/CODING_STANDARDS.md) | Java/Spring Boot conventions |
| [TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) | BDDMockito, recursive comparison, no generic matchers |
| [SOLANA_CODING_STANDARDS.md](docs/SOLANA_CODING_STANDARDS.md) | Anchor program standards |
| [ADR.md](docs/ADR.md) | Architecture decision records |
| [E2E_FLOW.md](docs/E2E_FLOW.md) | End-to-end user journey |
| [ROADMAP.md](docs/ROADMAP.md) | Implementation timeline |
| [BRAINSTORM.md](docs/BRAINSTORM.md) | Market research and strategy |
| [COMPETITIVE_ANALYSIS.md](docs/COMPETITIVE_ANALYSIS.md) | Market positioning analysis |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution workflow |

## Contributing

All work goes through feature branches and pull requests. Never commit directly to `main`.

```bash
git checkout -b feature/STA-42-add-claim-page
# implement + test
cd backend && ./gradlew build
git push -u origin feature/STA-42-add-claim-page
gh pr create --title "STA-42: Add claim page"
```

Branch naming: `feature/STA-{N}-description`

Commit messages: `feat(STA-{N}): description`
