# CLAUDE.md

## Project Overview

**StablePay** — Cross-border stablecoin remittance on Solana for the Colosseum Frontier Hackathon (April 6 – May 11, 2026).

Consumer-facing app enabling instant, low-cost remittances via USDC on Solana with MPC wallet abstraction and guaranteed delivery.

Architecture: Hexagonal (Ports & Adapters) + DDD + Event-Driven.

## Mandatory Reading

Before writing or modifying any code, read the relevant doc:

| Task | Read first |
|---|---|
| Any production code | [docs/CODING_STANDARDS.md](docs/CODING_STANDARDS.md) |
| Any test code | [docs/TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) |
| Architecture or design decisions | [docs/ADR.md](docs/ADR.md) |
| Full requirements | [docs/brainstorms/2026-04-03-stablepay-cross-border-requirements.md](docs/brainstorms/2026-04-03-stablepay-cross-border-requirements.md) |
| Implementation plan | [docs/plans/2026-04-03-001-feat-cross-border-remittance-plan.md](docs/plans/2026-04-03-001-feat-cross-border-remittance-plan.md) |

These docs are the single source of truth. Do not guess conventions — look them up.

## Build Commands

### Backend (Java/Spring Boot)

```bash
./gradlew build              # compile + Spotless + all tests
./gradlew test               # unit tests only
./gradlew integrationTest    # integration tests (requires Docker)
./gradlew spotlessApply      # auto-format before committing
./gradlew bootRun            # run locally
```

### Anchor Program (Solana/Rust)

```bash
anchor build                 # compile Anchor program
anchor test                  # run program tests on localnet
anchor deploy                # deploy to devnet
```

### MPC Sidecar (Go)

```bash
cd mpc-sidecar && go build ./...   # compile
cd mpc-sidecar && go test ./...    # run tests
```

### Mobile App (React Native/Expo)

```bash
cd mobile && npx expo start        # start dev server
```

### Web Claim Page (Next.js)

```bash
cd web-claim && npm run dev        # start dev server
```

### Infrastructure

```bash
docker compose up -d               # start PostgreSQL, Temporal, Redis
docker compose down                # stop infrastructure
```

## Tech Stack

| Component | Version / Library |
|---|---|
| Java | 25 (LTS, Eclipse Temurin) |
| Spring Boot | 4.0.3 |
| HTTP Server | Tomcat (Spring MVC) |
| HTTP Layer | Spring MVC — controllers return standard types |
| Database | JPA + Hibernate + PostgreSQL 16 |
| Cache | Redis |
| Workflows | Temporal |
| Build | Gradle with Kotlin DSL |
| Migrations | Flyway |
| Mapping | MapStruct 1.6.3 |
| Testing | JUnit 5, BDDMockito, AssertJ, MockMvc |
| Solana Program | Anchor 0.30.x (Rust) |
| MPC Sidecar | Go 1.26, bnb-chain/tss-lib (fystack fork) |
| Mobile | React Native, Expo SDK 52 |
| Web Claim | Next.js, shadcn/ui |
| Solana SDK (Java) | sol4k |
| Solana SDK (JS) | @solana/web3.js v1.x |
| SMS | Twilio |

## Monorepo Structure

```
stablepay-hackathon/
├── programs/
│   └── stablepay-escrow/         # Anchor program (Rust)
├── backend/                       # Spring Boot 4.0.3 (Java 25, Spring MVC + JPA)
│   ├── src/main/java/com/stablepay/
│   │   ├── application/           # Controllers, config
│   │   ├── domain/                # Models, ports, services
│   │   └── infrastructure/        # Persistence, MPC, Solana, Stripe, Twilio, Temporal
│   └── src/test/
├── mpc-sidecar/                   # Go (forked mpcium EdDSA)
├── mobile/                        # React Native + Expo SDK 52
├── web-claim/                     # Next.js claim page
├── docker-compose.yml             # PostgreSQL, Redis, Temporal
├── Anchor.toml
└── docs/
```

### Package Root

`com.stablepay`

### Hexagonal Layers (Backend)

```
com.stablepay/
├── application/
│   ├── controller/     # Spring MVC @RestController
│   └── config/         # Spring @Configuration
├── domain/
│   ├── model/          # Records + @Builder — no Spring annotations
│   ├── port/
│   │   ├── inbound/    # Service interfaces
│   │   └── outbound/   # Repository, Client, Provider interfaces
│   └── service/        # Business logic
└── infrastructure/
    ├── persistence/    # JPA entities + Spring Data repositories
    ├── temporal/       # Temporal workflows + activities
    ├── mpc/            # gRPC client to MPC sidecar
    ├── solana/         # SolanaRpcClient, transaction construction
    ├── stripe/         # Stripe on-ramp adapter
    ├── fx/             # FX rate provider
    └── sms/            # Twilio adapter
```

**Dependency rule:** `domain` → nothing. `application` → `domain`. `infrastructure` → `domain`. Never `infrastructure` → `application.controller`.

### Error Code Prefix

`SP-XXXX` (e.g., `SP-0001`)

### Flyway Migration Naming

`V{N}__{description}.sql` (located in `backend/src/main/resources/db/migration/`)

## Style Rules (always applied)

### Code

- **Spring MVC (blocking)** — controllers return standard types, no Mono<>/Flux<>
- **JPA + Hibernate** — standard @Entity annotations in infrastructure layer
- **No wildcard imports** — every import fully qualified
- **No `@Autowired`** — use `@RequiredArgsConstructor` with `private final` fields
- **No `System.out`/`System.err`** — use `@Slf4j`
- **Use `var`** for local variables when the type is obvious
- **Java records** + `@Builder(toBuilder = true)` for all domain models, value objects, DTOs
- **MapStruct** `@Mapper(componentModel = "spring")` for all layer-boundary mapping
- **Functional style** — streams over loops, Optional pipelines over null checks
- **Domain exceptions** use structured error codes (`SP-XXXX`)
- **Minimal Javadoc** — code must be self-documenting

### Testing

- **BDDMockito ONLY** — `given()`/`then()`, never `when()`/`verify()`
- **Golden rule** — build expected object → single `assertThat(actual).usingRecursiveComparison().isEqualTo(expected)`
- **No generic matchers** — never `any()`, `anyString()`, `eq()` — use actual values or custom matchers
- **MockMvc** for controller tests
- **Test naming** — `should*` camelCase (e.g., `shouldCreateRemittanceWithLockedFxRate`)
- **`// given`, `// when`, `// then` comments** in every test method
- **AssertJ only** — no JUnit `assertEquals`/`assertTrue`

## Hackathon Context

- **Event:** Colosseum Frontier Hackathon (presented by Colosseum + Solana Foundation)
- **Dates:** April 6 – May 11, 2026
- **Prize:** $250K pre-seed from Colosseum's venture fund + accelerator admission
- **Corridor:** USD → INR only
- **Stablecoin:** USDC on Solana devnet only

## Design Principles

1. **One corridor, one persona** — don't build "payments for everyone"
2. **Wallet abstraction** — MPC Ed25519 threshold signing, no seed phrases
3. **Guaranteed delivery** — Temporal state machine with auto-retry
4. **Recipient doesn't need an app** — claim via SMS link + web page
5. **Demo on devnet** — working end-to-end flow, not slides

## Docs

| Doc | Purpose |
|---|---|
| [docs/BRAINSTORM.md](docs/BRAINSTORM.md) | Full research, competitive analysis |
| [docs/CODING_STANDARDS.md](docs/CODING_STANDARDS.md) | Coding conventions and patterns |
| [docs/TESTING_STANDARDS.md](docs/TESTING_STANDARDS.md) | Testing rules and patterns |
| [docs/ADR.md](docs/ADR.md) | Architecture decision records |
| [docs/brainstorms/](docs/brainstorms/) | Requirements documents |
| [docs/plans/](docs/plans/) | Implementation plans |
