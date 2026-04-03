# Architecture Decision Records — StablePay

## ADR-001: Monorepo Structure

**Status:** Accepted
**Context:** StablePay has 5 components (Anchor program, Spring Boot backend, Go MPC sidecar, React Native mobile, Next.js claim page) built in a 5-week hackathon.
**Decision:** Single monorepo with top-level directories per component.
**Rationale:** Simplifies hackathon development. Single git history. Shared Docker Compose. Easy demo setup.

## ADR-002: Hexagonal Architecture (Backend)

**Status:** Accepted
**Context:** Builder has 5 production projects all using hexagonal architecture.
**Decision:** Use Hexagonal (Ports & Adapters) for the Spring Boot backend. Three layers: `domain/`, `application/`, `infrastructure/`.
**Rationale:** Muscle memory from existing projects. Clean separation enables swapping infrastructure (e.g., mock off-ramp → real off-ramp). ArchUnit enforces layer boundaries.
**Consequences:** More files than a flat structure, but familiar patterns reduce development time.

## ADR-003: Spring MVC + JPA (Blocking Stack)

**Status:** Accepted
**Context:** All 5 existing stablebridge projects use blocking stack (spring-boot-starter-web, spring-boot-starter-data-jpa, RestClient). The plan initially considered WebFlux + R2DBC (reactive).
**Decision:** Use Spring MVC + JPA/Hibernate (blocking), not WebFlux + R2DBC.
**Rationale:**
- Direct code copy from stablebridge-tx-recovery and stablecoin-payments.
- Temporal activities are blocking by design — reactive wrapping adds complexity for zero benefit.
- Single-user demo — no concurrency benefit from reactive stack.
- Simpler debugging (real stack traces vs reactive traces).
**Consequences:** Cannot use non-blocking I/O patterns. Acceptable for hackathon demo scale.

## ADR-004: Domain Models as Immutable Records

**Status:** Accepted
**Context:** Need consistent domain model pattern across the backend.
**Decision:** All domain models are Java records with `@Builder(toBuilder = true)`. State transitions return new instances. No JPA annotations on domain models.
**Rationale:** Immutability prevents accidental state corruption. `toBuilder()` enables copy-and-modify for state transitions. Matches stablebridge-tx-recovery patterns.

## ADR-005: Temporal for Remittance Lifecycle

**Status:** Accepted
**Context:** Need durable orchestration across MPC signing, Solana submission, SMS delivery, and claim processing.
**Decision:** Use Temporal workflows for the remittance lifecycle (INITIATED → ESCROWED → CLAIMED → DELIVERED).
**Rationale:**
- Production-grade patterns already exist in stablebridge-tx-recovery.
- Durable execution survives backend restarts.
- Built-in retry policies, timers, and signals.
- Can demonstrate guaranteed delivery (SC3) by showing workflow recovery after failure.
**Consequences:** Requires Temporal server in Docker Compose. Activities must be idempotent.

## ADR-006: Custom Anchor Escrow Program

**Status:** Accepted
**Context:** Need on-chain USDC escrow for remittances on Solana.
**Decision:** Custom Anchor program with PDA-based escrow. Backend acts as claim authority (recipient has no wallet).
**Rationale:**
- Demonstrates Solana programming depth to judges.
- Full control over claim/refund/cancel logic.
- PDA seeds `[b"escrow", remittance_id]` enable deterministic escrow addresses.
- Closing accounts on terminal states prevents double-claim (on-chain idempotency).
**Consequences:** Must build and deploy Anchor program. Anchor 0.30.x stable.

## ADR-007: MPC Wallet via Forked mpcium Ed25519

**Status:** Accepted (with fallback)
**Context:** Existing stablebridge-mpc-wallet uses GG20 ECDSA (secp256k1). Solana requires Ed25519.
**Decision:** Fork fystack/mpcium's EdDSA keygen + signing code into the existing Go sidecar architecture. Use fystack/tss-lib fork which has EdDSA module.
**Rationale:**
- Same bnb-chain/tss-lib foundation as existing sidecar.
- EdDSA keygen/signing already implemented and tested in mpcium.
- Keeps MPC as a real differentiator (not just a roadmap item).
- Reuse sidecar scaffolding (gRPC, P2P, config) from existing project.
**Fallback:** If Ed25519 MPC spike fails by April 8, pivot to server-side custodial wallet with identical UX. No SC is blocked.
**Consequences:** Go sidecar crypto core is a rewrite (EdDSA imports), not an adaptation. Scaffolding reusable.

## ADR-008: 2-of-2 MPC Threshold for Hackathon

**Status:** Accepted
**Context:** Production MPC uses 2-of-3 threshold with 3 sidecar nodes. Hackathon demo needs simplicity.
**Decision:** Use 2-of-2 threshold with two sidecar instances in Docker Compose.
**Rationale:** Reduces infrastructure while preserving real threshold signing ceremony. Demonstrates MPC without 3-node cluster. Still requires P2P communication and multi-party DKG.
**Consequences:** Lower fault tolerance than 2-of-3 (both parties must participate). Acceptable for demo.

## ADR-009: Treasury Pre-Funding (P0) vs Stripe On-Ramp (P1)

**Status:** Accepted
**Context:** On-ramp funding needs to work for demo. Stripe ACH integration is real but adds risk.
**Decision:** P0 path: pre-funded devnet treasury wallet + "demo fund" endpoint. Stripe ACH on-ramp is P1 (stretch goal).
**Rationale:**
- No SC requires a judge to complete a Stripe payment flow.
- Treasury pre-funding is 100% reliable for demos.
- Stripe integration can be added if time permits.
**Consequences:** Demo shows "Fund Wallet" button that transfers devnet USDC from treasury. Real fiat on-ramp is a production feature.

## ADR-010: Simulated INR Off-Ramp

**Status:** Accepted
**Context:** Circle redeems USDC to USD, not INR. INR disbursement requires a local partner API (TransFi, Mudrex) that may not have sandbox access.
**Decision:** Simulated INR disbursement as primary hackathon path. Architecture shows the real flow (USDC → Circle → partner → INR) but executes a mock.
**Rationale:** Judges understand sandbox constraints. Real architecture in code demonstrates intent. No SC requires actual INR in a bank account.

## ADR-011: Bearer Token Claim Links

**Status:** Accepted
**Context:** Recipients claim via SMS link. Need to decide authentication model for the claim page.
**Decision:** Cryptographically random UUID as bearer token in URL. Recipient enters UPI ID as weak second factor. No OTP for hackathon.
**Rationale:** Simple, sufficient for demo. UPI entry prevents casual link forwarding. OTP verification is a pitch point for production.
**Consequences:** Anyone with the link + knowledge of recipient's UPI can claim. Acceptable for devnet demo.

## ADR-012: sol4k for Java Solana Integration

**Status:** Accepted
**Context:** Need to construct Anchor instructions (Borsh serialization, PDA derivation, account metas) from Java backend.
**Decision:** Use sol4k as the Java Solana SDK. Existing SolanaRpcClient from tx-recovery handles raw JSON-RPC.
**Rationale:** sol4k is the most active Java/Kotlin Solana library. Handles Borsh serialization and PDA derivation.

## ADR-013: No Kafka for Hackathon

**Status:** Accepted
**Context:** Production architecture uses Kafka for event streaming. Hackathon demo serves one user.
**Decision:** Drop Kafka from hackathon scope. Temporal workflows drive state transitions directly via Solana RPC polling.
**Rationale:** Eliminates infrastructure complexity (broker, topics, consumer groups) for zero demo benefit. Temporal provides durable orchestration. Production roadmap includes Kafka.

## ADR-014: React Native + Expo for Mobile

**Status:** Accepted
**Context:** Need a mobile sender app for the demo.
**Decision:** React Native with Expo SDK 52. @solana/web3.js v1.x for Solana interaction.
**Rationale:** Fastest path to working demo. Expo Router for navigation. Polyfills (buffer, crypto) are well-documented. No Mobile Wallet Adapter needed — transaction signing happens on backend.

## ADR-015: FX Rate Source

**Status:** Accepted
**Context:** Need live USD→INR exchange rate for remittance quotes.
**Decision:** ExchangeRate-API.com (free tier, 1500 req/month) with Redis caching (60s TTL). Hardcoded fallback rate (84.50) for demo reliability.
**Rationale:** Simple REST API, no auth required for free tier. Fallback prevents demo failure if API is down. Redis cache prevents rate limiting.

## ADR-016: Escrow Expiry Window

**Status:** Accepted
**Context:** Need to define how long USDC stays locked if recipient doesn't claim.
**Decision:** 48-hour expiry window. FX rate locked at send time applies throughout.
**Rationale:** Accommodates US→India timezone difference (up to 13.5h offset). Recipients may not check SMS immediately. 48h balances urgency with accessibility.

## ADR-017: MapStruct for Object Mapping

**Status:** Accepted
**Context:** Need consistent mapping between domain models, JPA entities, and API DTOs.
**Decision:** MapStruct `@Mapper(componentModel = "spring")` for all layer-boundary mapping.
**Rationale:** Compile-time generated, type-safe, no reflection overhead. Matches all existing stablebridge projects.

## ADR-018: Testing Strategy

**Status:** Accepted
**Context:** Need testing approach that balances coverage with hackathon speed.
**Decision:** Two-tier testing: unit tests (Mockito, BDD style) + integration tests (TestContainers, MockMvc). Golden rule: single recursive comparison assertion.
**Rationale:** Matches stablebridge-tx-recovery patterns. BDDMockito for readability. AssertJ recursive comparison catches field-level regressions. ArchUnit enforces layer boundaries.

## ADR-019: SMS Delivery via Twilio

**Status:** Accepted
**Context:** Need to send claim links to recipients.
**Decision:** Twilio Programmable SMS. Single channel (SMS only, no WhatsApp).
**Rationale:** 20-minute integration. WhatsApp Business API requires approval and template setup. SMS is sufficient for demo. Claim link works regardless of delivery channel.

## ADR-020: Coding Conventions for Agents

**Status:** Accepted
**Context:** Multiple team members and AI agents contributing to the codebase.
**Decision:** Enforce conventions via CLAUDE.md, CODING_STANDARDS.md, TESTING_STANDARDS.md, and ArchUnit.
**Rules:**
- No `@Autowired` — use `@RequiredArgsConstructor` with `private final` fields
- No `System.out`/`System.err` — use `@Slf4j`
- BDDMockito only — `given()`/`then()`, never `when()`/`verify()`
- No generic matchers — `any()`, `anyString()` forbidden
- Records + `@Builder(toBuilder = true)` for all domain models
- Functional style — streams over loops, Optional over null checks
- Error codes: `SP-XXXX`

## ADR-021: Pessimistic Locking for Wallet Balance

**Status:** Accepted
**Context:** Wallet balance updates (reserve on send, release on cancel/refund) must be serialized to prevent lost updates. Optimistic locking (`@Version`) causes retry storms under concurrent access to the same wallet.
**Decision:** Use pessimistic locking (`@Lock(PESSIMISTIC_WRITE)` / `SELECT ... FOR UPDATE`) on all wallet read-for-update queries. 4-second lock timeout prevents indefinite waits.
**Rationale:**
- Matches production-grade pattern from stablebridge transfer-service (10-transfer).
- Guarantees serialized access to wallet balance — no lost updates.
- Lock timeout (4s) bounds worst-case wait, preventing thread starvation.
- `READ_COMMITTED` isolation is sufficient when combined with pessimistic locks.
**Consequences:** Wallet reads that precede updates will block concurrent transactions. Acceptable for a single-corridor remittance system. Future sorted-lock-acquisition pattern may be needed if multi-wallet transfers are introduced.
