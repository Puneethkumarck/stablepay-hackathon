# Backend Testing Guide

> How to run, write, and reason about tests for the StablePay Java/Spring Boot backend.

## Quick Start

```bash
cd backend/

# Unit tests only (no Docker required)
./gradlew test

# Integration tests (requires Docker for TestContainers)
./gradlew integrationTest

# Full build: compile + Spotless + unit tests + ArchUnit
./gradlew build

# Everything: build + integration tests
./gradlew build integrationTest

# Format before committing
./gradlew spotlessApply
```

## Prerequisites

| Requirement | Version | Why |
|---|---|---|
| Java | 25 (Temurin) | Compile and run |
| Gradle | 9.4+ (wrapper included) | Build tool |
| Docker | 20+ (Docker Desktop, OrbStack, or Colima) | TestContainers PostgreSQL |

**TestContainers + OrbStack users:** If integration tests fail with `DockerClientProviderStrategy` errors, add to `~/.testcontainers.properties`:

```properties
docker.host=unix:///Users/<you>/.orbstack/run/docker.sock
```

---

## Test Architecture

```
src/
├── test/                    # Unit tests (44 files) — no Spring context, no Docker
├── integration-test/        # Integration tests (8 test classes) — real PostgreSQL, Temporal
└── testFixtures/            # Shared fixtures across both source sets (7 files)
```

### Three-Tier Strategy

| Tier | Source Set | Context | Database | External Services | Speed |
|---|---|---|---|---|---|
| **Unit** | `src/test/` | None (`@ExtendWith(MockitoExtension)`) or `@WebMvcTest` | Mocked | Mocked | ~50s total |
| **Integration** | `src/integration-test/` | `@SpringBootTest` via `@PgTest` | Real PostgreSQL (TestContainers) | Mocked (`IntegrationTestConfig`) or WireMock | ~35s total |
| **E2E** | `src/integration-test/` | `@PgTest + @AutoConfigureMockMvc` | Real PostgreSQL | Mocked (MPC, SMS, Disbursement) | Runs with integration |

### What Gets Mocked at Each Tier

| Dependency | Unit Tests | Integration Tests |
|---|---|---|
| PostgreSQL | Mocked via `@Mock` repository | Real (TestContainers `postgres:16-alpine`) |
| Temporal | `TestWorkflowEnvironment` | `TestWorkflowEnvironment` (in-process) |
| MPC Sidecar (gRPC) | `@Mock MpcWalletClient` | `Mockito.mock()` via `IntegrationTestConfig` |
| Twilio SMS | `@Mock SmsProvider` | `Mockito.mock()` via `IntegrationTestConfig` |
| Fiat Disbursement | `@Mock FiatDisbursementProvider` | `Mockito.mock()` via `IntegrationTestConfig` |
| ExchangeRate API | `@Mock RestClient` | WireMock (`WireMockExtension`) |
| Redis | Excluded via `application-test.yml` | Excluded via `application-test.yml` |

---

## Test Infrastructure

### Custom Annotations

**`@PgTest`** — PostgreSQL integration tests:
```java
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(PostgresContainerExtension.class)
@Import(IntegrationTestConfig.class)
```

**`@TemporalTest`** — Temporal workflow integration tests:
```java
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(PostgresContainerExtension.class)
@Import({IntegrationTestConfig.class, TemporalTestConfig.class})
```

### PostgresContainerExtension

Starts a single `postgres:16-alpine` container, reused across all test classes:

```
Container startup → Flyway V1, V2, V3 → Hibernate validate → Tests run
```

The extension injects `spring.datasource.url/username/password` as system properties dynamically from the container's random port.

### IntegrationTestConfig

Provides mock beans for external services that cannot run in CI:

```java
@TestConfiguration
public class IntegrationTestConfig {
    @Bean MpcWalletClient mpcWalletClient()           → Mockito.mock()
    @Bean SmsProvider smsProvider()                     → Mockito.mock()
    @Bean FiatDisbursementProvider disbursementProvider() → Mockito.mock()
}
```

### TemporalTestConfig

Provides an in-process Temporal test server with mocked activities:

```java
@TestConfiguration
public class TemporalTestConfig {
    @Bean TestWorkflowEnvironment testWorkflowEnvironment()
    @Bean WorkflowClient workflowClient(testEnv)
    @Bean @Primary RemittanceLifecycleActivities activities() → Mockito.mock()
    @Bean Worker worker(testEnv, activities)    → registers workflow + activities, starts env
}
```

---

## Test Coverage Map

### Domain Layer (Handlers)

| Handler | Test File | Tests |
|---|---|---|
| `CreateWalletHandler` | `CreateWalletHandlerTest` | happy path, wallet already exists |
| `FundWalletHandler` | `FundWalletHandlerTest` | happy path, wallet not found, treasury depleted, balance update |
| `CreateRemittanceHandler` | `CreateRemittanceHandlerTest` | happy path, insufficient balance, wallet not found, FX locking, workflow start |
| `GetRemittanceQueryHandler` | `GetRemittanceQueryHandlerTest` | found, not found, field mapping |
| `ListRemittancesQueryHandler` | `ListRemittancesQueryHandlerTest` | pagination, empty results |
| `UpdateRemittanceStatusHandler` | `UpdateRemittanceStatusHandlerTest` | valid transitions, invalid transitions, not found |
| `SubmitClaimHandler` | `SubmitClaimHandlerTest` | happy path, already claimed, expired, token not found, workflow signal |
| `GetClaimQueryHandler` | `GetClaimQueryHandlerTest` | found, not found |
| `GetFxRateQueryHandler` | `GetFxRateQueryHandlerTest` | supported corridor, unsupported corridor |

### Domain Models

| Model | Test File | Tests |
|---|---|---|
| `Wallet` | `WalletTest` | reserveBalance, releaseBalance, insufficient balance |
| `Remittance` | `RemittanceTest` | state transitions, null safety |
| `RemittanceStatus` | `RemittanceStatusTest` | valid/invalid transitions |
| `ClaimToken` | `ClaimTokenTest` | construction, null checks |
| `Corridor` | `CorridorTest` | USD-INR lookup, case insensitivity |
| `PiiMasking` | `PiiMaskingTest` | null, empty, short, boundary, normal |

### Controllers (MockMvc Unit Tests)

| Controller | Test File | Endpoints Tested |
|---|---|---|
| `WalletController` | `WalletControllerTest` | POST /api/wallets (201, 400, 409), POST /api/wallets/{id}/fund (200, 404, 503) |
| `RemittanceController` | `RemittanceControllerTest` | POST /api/remittances (201, 400), GET /api/remittances/{id} (200, 404), GET /api/remittances (200) |
| `ClaimController` | `ClaimControllerTest` | GET /api/claims/{token} (200, 404, 410), POST /api/claims/{token} (200, 404, 409, 410) |
| `FxRateController` | `FxRateControllerTest` | GET /api/fx/{corridor} (200, 400) |

### Mappers (Bidirectional Mapping)

| Mapper | Test File | Pattern |
|---|---|---|
| `WalletEntityMapper` | `WalletEntityMapperTest` | domain → entity → domain roundtrip |
| `RemittanceEntityMapper` | `RemittanceEntityMapperTest` | domain → entity → domain roundtrip |
| `ClaimTokenEntityMapper` | `ClaimTokenEntityMapperTest` | domain → entity → domain roundtrip |
| `WalletApiMapper` | `WalletApiMapperTest` | domain → DTO |
| `RemittanceApiMapper` | `RemittanceApiMapperTest` | domain → DTO |
| `ClaimApiMapper` | `ClaimApiMapperTest` | composite (ClaimDetails → DTO with @Mapping) |
| `FxRateApiMapper` | `FxRateApiMapperTest` | domain → DTO |

### Infrastructure Adapters

| Adapter | Test File | What's Tested |
|---|---|---|
| `SolanaTransactionServiceAdapter` | `SolanaTransactionServiceAdapterTest` | deposit, claim, refund escrow, wallet lookup, MPC signing errors |
| `EscrowInstructionBuilder` | `EscrowInstructionBuilderTest` | 16 tests: deposit/claim/refund instructions, PDA derivation, USDC conversion |
| `ExchangeRateApiAdapter` | `ExchangeRateApiAdapterTest` | API call, response parsing |
| `MpcWalletGrpcClient` | `MpcWalletGrpcClientTest` | key generation, signing, gRPC errors |
| `TwilioSmsAdapter` | `TwilioSmsAdapterTest` | SMS sending, error handling |
| `TransakDisbursementAdapter` | `TransakDisbursementAdapterTest` | quote + order creation |
| `LoggingDisbursementAdapter` | `LoggingDisbursementAdapterTest` | no-op execution |
| `LoggingSmsAdapter` | `LoggingSmsAdapterTest` | no-op execution |
| `TreasuryServiceAdapter` | `TreasuryServiceAdapterTest` | stub balance, transfer no-op |

### Temporal Workflow

| Test File | What's Tested |
|---|---|
| `RemittanceLifecycleWorkflowImplTest` (unit) | Happy path, timeout/refund, SMS failure, SMS failure + claim, status query, disbursement failure, claim before timeout |
| `RemittanceLifecycleWorkflowIntegrationTest` (integration) | Happy path, timeout/refund, SMS failure, status query, disbursement failure |
| `RemittanceLifecycleActivitiesImplTest` | Activity delegation: deposit, release, refund, SMS, disbursement, status update |
| `TemporalRemittanceWorkflowStarterTest` | Workflow request construction, property injection |
| `TemporalRemittanceClaimSignalerTest` | Signal construction, workflow ID, destination address |

### Integration Tests (Real PostgreSQL)

| Test File | What's Tested |
|---|---|
| `WalletRepositoryIntegrationTest` | save, findById, findByUserId, findBySolanaAddress, not found |
| `RemittanceRepositoryIntegrationTest` | save, findByRemittanceId, findBySenderId (pagination, isolation), status updates |
| `ClaimTokenRepositoryIntegrationTest` | save, findByToken, UPI persistence, FK constraint |
| `WalletApiIntegrationTest` | Full HTTP: create wallet (201, 400, 409), fund wallet (200, 404) |
| `ExchangeRateApiIntegrationTest` | WireMock: live rate, 500 fallback, timeout fallback |
| `OpenApiIntegrationTest` | /v3/api-docs: spec, tags, error schema |

### E2E Integration Test (Full Remittance Lifecycle)

| Test File | What's Tested |
|---|---|
| `RemittanceLifecycleE2EIntegrationTest` | 10-step flow via HTTP: create wallet → fund → check FX → create remittance → verify INITIATED → advance to ESCROWED → get claim → submit claim → verify status → list remittances |

This test exercises the **complete API surface** in a single test, hitting Controller → Handler → Repository → DB for every endpoint.

---

## How to Write Tests

### Unit Test Template (Handler)

```java
@ExtendWith(MockitoExtension.class)
class MyHandlerTest {

    @Mock private MyRepository repository;
    @InjectMocks private MyHandler handler;

    @Test
    void shouldDoSomething() {
        // given
        var input = someFixtureBuilder().build();
        given(repository.findById(SOME_ID)).willReturn(Optional.of(input));

        // when
        var result = handler.handle(SOME_ID);

        // then
        var expected = input.toBuilder().status(DONE).build();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("updatedAt")
                .isEqualTo(expected);
    }
}
```

### Controller Test Template (MockMvc)

```java
@WebMvcTest(MyController.class)
@Import(TestClockConfig.class)
class MyControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MyHandler handler;
    @MockitoBean private MyApiMapper mapper;

    @Test
    @SneakyThrows
    void shouldReturnCreated() {
        // given
        given(handler.handle("input")).willReturn(domainObj);
        given(mapper.toResponse(domainObj)).willReturn(responseObj);

        // when / then
        mockMvc.perform(post("/api/things")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "field": "input" }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.field").value("input"));
    }
}
```

### Repository Integration Test Template

```java
@PgTest
@Transactional
class MyRepositoryIntegrationTest {

    @Autowired private MyRepository repository;

    @Test
    void shouldSaveAndFind() {
        // given
        var entity = buildEntity();

        // when
        var saved = repository.save(entity);
        var found = repository.findById(saved.id());

        // then
        assertThat(found).isPresent();
        assertThat(found.get())
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(saved);
    }
}
```

**Key rules:**
- Use `@Transactional` on repository integration tests for automatic rollback.
- Do NOT use `@Transactional` on API integration tests — let the controller transaction commit, verify via API response.
- Use unique IDs (`System.nanoTime()` suffix) to avoid cross-test collisions.

### API Integration Test Template

```java
@PgTest
@AutoConfigureMockMvc
class MyApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MpcWalletClient mpcWalletClient;  // mocked via IntegrationTestConfig

    @Test
    @SneakyThrows
    void shouldCreateAndReturnResponse() {
        // given
        given(mpcWalletClient.generateKey()).willReturn(generatedKey);

        // when
        var result = mockMvc.perform(post("/api/things")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // then — verify via API response only, no direct DB access
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("expected"));
    }
}
```

### Temporal Workflow Integration Test Template

```java
@TemporalTest
class MyWorkflowIntegrationTest {

    @Autowired private WorkflowClient workflowClient;
    @Autowired private TestWorkflowEnvironment testEnv;
    @Autowired private RemittanceLifecycleActivities activities;  // mocked

    @Test
    void shouldComplete() {
        // given
        var workflow = startWorkflow();

        // when
        workflow.claimSubmitted(claimSignalBuilder().build());
        var result = WorkflowStub.fromTyped(workflow)
                .getResult(RemittanceWorkflowResult.class);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("escrowPda", "txSignature")
                .isEqualTo(expected);

        then(activities).should()
                .updateRemittanceStatus(id, RemittanceStatus.DELIVERED);
    }
}
```

---

## Test Fixtures

All shared test data lives in `src/testFixtures/java/com/stablepay/testutil/`:

| Fixture | Constants | Builder |
|---|---|---|
| `WalletFixtures` | `SOME_WALLET_ID`, `SOME_USER_ID`, `SOME_SOLANA_ADDRESS`, `SOME_BALANCE` | `walletBuilder()` |
| `RemittanceFixtures` | `SOME_REMITTANCE_ID`, `SOME_SENDER_ID`, `SOME_AMOUNT_USDC`, `SOME_FX_RATE` | `remittanceBuilder()` |
| `ClaimTokenFixtures` | `SOME_TOKEN`, `SOME_UPI_ID`, `SOME_EXPIRES_AT` | `claimTokenBuilder()` |
| `FxQuoteFixtures` | `SOME_RATE`, `SOME_SOURCE`, `SOME_TIMESTAMP` | `fxQuoteBuilder()` |
| `WorkflowFixtures` | `SOME_SENDER_ADDRESS`, `SOME_CLAIM_TOKEN`, `SOME_DEPOSIT_TX_SIGNATURE` | `workflowRequestBuilder()`, `claimSignalBuilder()` |
| `SolanaFixtures` | `SOME_PROGRAM_ID`, `SOME_USDC_MINT`, `SOME_SENDER_WALLET` | — |
| `MpcFixtures` | `SOME_CEREMONY_ID`, `SOME_SOLANA_ADDRESS`, `SOME_SIGNATURE` | — |

**Rules:**
- Constants prefixed with `SOME_`.
- Builders return `Builder`, not built objects — callers customize and `.build()`.
- Available to both `src/test/` and `src/integration-test/` via `testFixtures` dependency.

---

## Mandatory Rules (from TESTING_STANDARDS.md)

1. **Golden rule** — single `assertThat(actual).usingRecursiveComparison().isEqualTo(expected)`.
2. **BDD Mockito only** — `given()`/`then()`, never `when()`/`verify()`.
3. **No generic matchers** — never `any()`, `anyString()` — use actual values.
4. **AssertJ only** — no JUnit `assertEquals`/`assertTrue`.
5. **Test naming** — `should*` camelCase (e.g., `shouldCreateRemittanceWithLockedFxRate`).
6. **Given/When/Then comments** — in every test method.
7. **`@Spy` for real mappers** — `@Spy private Mapper mapper = new MapperImpl()`.

---

## Troubleshooting

### Integration tests fail with "DockerClientProviderStrategy"

Docker isn't reachable. Check:
```bash
docker ps   # must succeed
```

For OrbStack, add to `~/.testcontainers.properties`:
```properties
docker.host=unix:///Users/<you>/.orbstack/run/docker.sock
```

### "No active transaction" in API integration tests

Do NOT call repository methods directly in `@PgTest + @AutoConfigureMockMvc` tests. The controller runs in its own committed transaction. Verify state via the API response, not direct DB access.

### Temporal tests hang

Ensure `testEnv.start()` is called (happens automatically via `TemporalTestConfig`). For unit tests (`RemittanceLifecycleWorkflowImplTest`), call it explicitly in `@BeforeEach`.

### Flyway migration errors

Check that `src/main/resources/db/migration/V{N}__*.sql` files are valid. The test profile uses `ddl-auto: validate` — Hibernate validates the schema against entities after Flyway runs.
