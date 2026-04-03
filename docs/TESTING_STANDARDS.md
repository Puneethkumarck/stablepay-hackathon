# Testing Standards for StablePay

> Mandatory testing rules for the StablePay Java/Spring Boot backend.
> Coding agents must follow these rules exactly.

## GOLDEN RULE: Build Expected Object + Single Recursive Comparison (MANDATORY)

**Every test that verifies an object result MUST construct an expected object and compare with a single `assertThat(...).usingRecursiveComparison()`.** Multiple `assertThat` calls on individual fields are FORBIDDEN.

### The Pattern

```java
// 1. Build expected object using toBuilder(), factory, or constructor
var expected = input.toBuilder()
        .status(ESCROWED)
        .build();

// 2. Single assertion with recursive comparison
assertThat(result)
        .usingRecursiveComparison()
        .ignoringFields("id", "createdAt")
        .isEqualTo(expected);
```

### FORBIDDEN vs REQUIRED

```java
// FORBIDDEN: multiple asserts on individual fields
assertThat(result.status()).isEqualTo(ESCROWED);
assertThat(result.amountUsdc()).isEqualTo(amount);

// REQUIRED: build expected object + single recursive comparison
var expected = remittance.toBuilder().status(ESCROWED).build();
assertThat(result).usingRecursiveComparison()
        .ignoringFields("updatedAt").isEqualTo(expected);
```

### Narrow Exceptions

| Case | Why allowed |
|---|---|
| Exception assertions | No object to compare: `assertThatThrownBy(...).isInstanceOf(...)` |
| Single boolean/primitive | Trivial: `assertThat(result.isValid()).isTrue()` |
| Collection size + containment | `assertThat(list).hasSize(3).containsOnly(a, b, c)` |
| Optional presence | `assertThat(result).isPresent().hasValue(expected)` |

## 1. Test Strategy Overview

| Source Set | Directory | Scope | Speed |
|---|---|---|---|
| **Unit** | `src/test/java/` | Single class, mocked deps | Fast |
| **Test Fixtures** | `src/testFixtures/java/` | Shared data, stubs, utilities | N/A |
| **Integration** | `src/integration-test/java/` | Spring context, DB, Temporal | Medium |

### Test Pyramid

```
       /\         Integration Tests
      /  \        Spring context + TestContainers + MockMvc
     /----\
    /      \      Unit Tests
   /--------\     Pure Mockito, no Spring context
```

## 2. Test Naming Conventions

Use `should*` in camelCase:

```java
void shouldCreateRemittanceWithLockedFxRate()
void shouldThrowIfInsufficientBalance()
void shouldReturnRemittancesByWalletId()
void shouldClaimEscrowAndUpdateStatus()
```

## 3. Test Structure: Given / When / Then

**Every test** follows the Given/When/Then pattern with explicit comment markers:

```java
@Test
void shouldCreateRemittanceWithLockedFxRate() {
    // given
    var wallet = WalletFixtures.walletWithBalance(BigDecimal.valueOf(100));
    var request = RemittanceFixtures.createRequest();
    given(walletRepository.findById(WALLET_ID)).willReturn(Optional.of(wallet));
    given(fxRateProvider.getRate("USD", "INR")).willReturn(FX_QUOTE);

    // when
    var result = remittanceService.create(request);

    // then
    var expected = Remittance.builder()
            .status(INITIATED)
            .amountUsdc(request.amountUsdc())
            .fxRate(FX_QUOTE.rate())
            .build();
    assertThat(result).usingRecursiveComparison()
            .ignoringFields("id", "remittanceId", "createdAt")
            .isEqualTo(expected);
}
```

## 4. Mocking Approach

### 4.1 BDD Mockito ONLY

```java
// REQUIRED: BDD style
given(repository.findById(id)).willReturn(Optional.of(entity));
then(repository).should().save(expectedEntity);

// FORBIDDEN: classic Mockito
when(repository.findById(id)).thenReturn(Optional.of(entity));  // NEVER
verify(repository).save(expectedEntity);                         // NEVER
```

### 4.2 Unit Test Setup

```java
@ExtendWith(MockitoExtension.class)
class RemittanceServiceImplTest {
    @Mock private RemittanceRepository remittanceRepository;
    @Mock private WalletRepository walletRepository;
    @InjectMocks private RemittanceServiceImpl remittanceService;
}
```

### 4.3 No Generic Argument Matchers (MANDATORY)

```java
// FORBIDDEN
given(repository.findById(any())).willReturn(Optional.of(entity));
then(repository).should().save(any(Remittance.class));

// REQUIRED: use actual values
given(repository.findById(REMITTANCE_ID)).willReturn(Optional.of(entity));
then(repository).should().save(expectedRemittance);
```

### 4.4 @Spy for Real Mappers

```java
@Spy private RemittanceMapper mapper = new RemittanceMapperImpl();
```

## 5. Test Fixtures & Builders

Shared test data lives in `src/testFixtures/java/com/stablepay/testutil/`:

```java
public final class RemittanceFixtures {
    public static final UUID SOME_REMITTANCE_ID = UUID.fromString("...");
    public static final BigDecimal SOME_AMOUNT = BigDecimal.valueOf(100);

    public static Remittance.RemittanceBuilder remittanceBuilder() {
        return Remittance.builder()
                .remittanceId(SOME_REMITTANCE_ID)
                .amountUsdc(SOME_AMOUNT)
                .status(INITIATED);
    }
}
```

**Rules:**
- Fixtures in `src/testFixtures/`, never private methods in test classes.
- Constants prefixed with `SOME_` for generic fixtures.
- Builder methods return builders, not built objects — callers customize.

## 6. Assertions

### AssertJ Only

```java
// REQUIRED
assertThat(result).isNotNull();
assertThat(list).hasSize(3).containsExactly(a, b, c);
assertThatThrownBy(() -> service.find(id))
        .isInstanceOf(RemittanceNotFoundException.class);

// FORBIDDEN
assertEquals(expected, result);  // JUnit — NEVER
assertTrue(result.isPresent());  // JUnit — NEVER
```

### Recursive Comparison for Domain Objects

```java
assertThat(result)
        .usingRecursiveComparison()
        .ignoringFields("id", "createdAt", "updatedAt")
        .isEqualTo(expected);
```

## 7. Integration Test Setup

Use TestContainers for infrastructure:

```java
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(PostgresContainerExtension.class)
class RemittanceIntegrationTest {
    @Autowired private MockMvc mockMvc;
    // ...
}
```

- `PostgresContainerExtension` starts PostgreSQL via TestContainers.
- Use `MockMvc` for controller integration tests (not WebTestClient).
- Use `@Transactional` on tests for automatic rollback.

## 8. Architecture Test (MANDATORY)

ArchUnit rules enforced in every build:

| # | Rule |
|---|------|
| 1 | `domain..` must NOT depend on `infrastructure..` |
| 2 | `domain..` must NOT depend on `application..` |
| 3 | `domain..` must NOT import Spring except `@Service`, `@Transactional` |
| 4 | `domain..` must NOT import `jakarta.persistence..` |
| 5 | `application..` must NOT depend on `infrastructure..` |
| 6 | `infrastructure..` must NOT depend on `application.controller..` |

## 9. Coverage & Quality Gates

| Metric | Minimum |
|---|---|
| Line coverage | 80% |
| Branch coverage | 70% |

## Anti-Patterns Summary

| Anti-Pattern | Correct Approach |
|---|---|
| `when()`/`verify()` | `given()`/`then()` (BDD Mockito) |
| `any()`, `anyString()` matchers | Use actual values |
| Multiple `assertThat` on fields | Single recursive comparison |
| `assertEquals` (JUnit) | `assertThat` (AssertJ) |
| Private fixture methods | `src/testFixtures/` shared fixtures |
| `@Autowired` in tests | `@Mock`/`@InjectMocks` for unit, `@Autowired` only for integration |
| WebTestClient | MockMvc |
