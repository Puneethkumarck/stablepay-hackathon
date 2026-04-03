# Coding Standards for StablePay Backend

Instructions for coding agents developing the StablePay Java/Spring Boot backend.

## 1. Architecture: Hexagonal (Ports and Adapters)

The backend follows a strict three-layer package structure under `com.stablepay`, with the **domain layer organized by subdomain/aggregate** (not by type):

```
com.stablepay
  ├── application/                 # Input adapters: REST controllers, DTOs, mappers, config
  │   ├── controller/
  │   │   └── {domain}/            # e.g., wallet/, fx/ — controller + mapper co-located
  │   │       ├── WalletController.java
  │   │       └── mapper/
  │   │           └── WalletApiMapper.java
  │   ├── dto/                     # Request/Response records shared across controllers
  │   └── config/                  # @Configuration, @RestControllerAdvice
  │
  ├── domain/                      # Core business logic — organized by subdomain
  │   ├── wallet/                  # Wallet aggregate
  │   │   ├── model/               # Wallet.java
  │   │   ├── handler/             # CreateWalletHandler, FundWalletHandler
  │   │   ├── port/                # WalletRepository, MpcWalletClient, TreasuryService
  │   │   └── exception/           # WalletNotFoundException, WalletAlreadyExistsException
  │   ├── remittance/              # Remittance aggregate
  │   │   ├── model/               # Remittance.java, RemittanceStatus.java
  │   │   ├── handler/             # CreateRemittanceHandler, etc.
  │   │   ├── port/                # RemittanceRepository
  │   │   └── exception/           # RemittanceNotFoundException
  │   ├── claim/                   # Claim aggregate
  │   │   ├── model/               # ClaimToken.java
  │   │   ├── handler/             # GetClaimQueryHandler, SubmitClaimHandler
  │   │   ├── port/                # ClaimTokenRepository
  │   │   └── exception/           # ClaimTokenExpiredException
  │   ├── fx/                      # FX aggregate
  │   │   ├── model/               # FxQuote.java, Corridor.java
  │   │   ├── handler/             # GetFxRateQueryHandler
  │   │   ├── port/                # FxRateProvider
  │   │   └── exception/           # UnsupportedCorridorException
  │   └── common/                  # Shared across subdomains
  │       ├── model/               # Shared value objects
  │       └── port/                # SmsProvider, shared ports
  │
  └── infrastructure/              # Output adapters — organized by concern
      ├── db/                      # Database adapters — by subdomain
      │   ├── wallet/              # WalletEntity, WalletEntityMapper, WalletJpaRepository, WalletRepositoryAdapter
      │   ├── remittance/          # RemittanceEntity, mapper, repo, adapter
      │   └── claim/               # ClaimTokenEntity, mapper, repo, adapter
      ├── temporal/                # Temporal workflows + activities
      ├── mpc/                     # gRPC client to MPC sidecar
      ├── solana/                  # SolanaRpcClient, treasury transfers
      ├── fx/                      # ExchangeRateApiAdapter, FxRateConfig
      ├── sms/                     # Twilio adapter
      └── config/                  # Infrastructure-wide config (Redis, etc.)
```

**Key principles (from stablebridge-tx-recovery reference):**
- **Domain organized by subdomain**, not by type. Each subdomain (wallet, remittance, claim, fx) is a self-contained package with its own model/, handler/, port/, exception/.
- **Infrastructure DB organized by subdomain** (`infrastructure/db/wallet/`, not `infrastructure/persistence/`).
- **Application controllers co-locate their mappers** (`controller/wallet/mapper/`).
- Cross-subdomain shared code goes in `domain/common/`.

**Rules:**
- `domain` MUST NOT import from `application` or `infrastructure`.
- `domain` handlers use Spring for DI (`@Service`) and transactions (`@Transactional`). This is an accepted pragmatic choice.
- `domain` models (records, value objects, enums) MUST NOT import Spring. Only Lombok is allowed on models.
- `application` depends on `domain`. It maps API models to domain models and delegates to handlers.
- `infrastructure` depends on `domain`. It implements domain ports and external integrations.
- Dependencies always point inward: `application` -> `domain` <- `infrastructure`.
- **Application MUST NOT call infrastructure directly.** The flow is always: `Controller` → `Domain Handler` → `Outbound Port` → `Infrastructure Adapter`. Never skip the domain layer.

## 2. Domain Layer

### 2.1 Domain Models

Use **Java records** with `@Builder(toBuilder = true)` for all domain models, value objects, and DTOs.

```java
// Domain model (record)
@Builder(toBuilder = true)
public record Remittance(
    Long id,
    UUID remittanceId,
    String senderId,
    String recipientPhone,
    BigDecimal amountUsdc,
    BigDecimal amountInr,
    BigDecimal fxRate,
    RemittanceStatus status,
    String escrowPda,
    String claimTokenId,
    boolean smsNotificationFailed,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt
) {}

// Enum for statuses
public enum RemittanceStatus {
    INITIATED, ESCROWED, CLAIMED, DELIVERED, REFUNDED, CANCELLED
}
```

**Rules:**
- Domain models are immutable. State transitions return new instances (via `toBuilder()`).
- No JPA annotations in domain models. Entity mapping belongs in `infrastructure`.
- No Spring annotations on domain models. Only Lombok (`@Builder`, `@Getter`, `@RequiredArgsConstructor`, `@Slf4j`).
- Records with 3+ fields MUST use `@Builder` — no positional constructors.

### 2.2 Repository Ports

Define repository interfaces in the domain layer. Implementation is in `infrastructure`.

```java
// domain/port/outbound/RemittanceRepository.java — plain interface, no annotations
public interface RemittanceRepository {
    Optional<Remittance> findByRemittanceId(UUID remittanceId);
    Remittance save(Remittance remittance);
    List<Remittance> findBySenderId(String senderId);
}
```

### 2.3 Domain Services

Services orchestrate domain logic. They use Spring for DI and transactions.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RemittanceServiceImpl implements RemittanceService {
    private final RemittanceRepository remittanceRepository;
    private final WalletRepository walletRepository;
    private final FxRateProvider fxRateProvider;
}
```

**Allowed Spring annotations in domain services:**
- `@Service`, `@Component` — for DI registration
- `@Transactional` — for transaction management

**NOT allowed in domain:**
- `@RestController`, `@GetMapping`, etc. (application layer only)
- `@Entity`, `@Table`, `@Column` (infrastructure layer only)
- `@Autowired` (use `@RequiredArgsConstructor` instead)

### 2.4 Error Handling

- Define domain-specific exceptions extending `RuntimeException`.
- Use structured error codes: `SP-XXXX` (4-digit, zero-padded).
- Use static factory methods on exceptions.

```java
public class RemittanceNotFoundException extends RuntimeException {
    public static RemittanceNotFoundException byId(UUID id) {
        return new RemittanceNotFoundException("SP-0001: Remittance not found: " + id);
    }
}

public class InsufficientBalanceException extends RuntimeException {
    public static InsufficientBalanceException forAmount(BigDecimal requested, BigDecimal available) {
        return new InsufficientBalanceException(
            "SP-0002: Insufficient balance. Requested: " + requested + ", Available: " + available);
    }
}
```

## 3. Application Layer

### 3.1 REST Controllers

- Delegate all business logic to domain services. Controllers are thin.
- Use Spring MVC annotations (`@RestController`, `@GetMapping`, etc.).
- Use `@Valid` for request validation.
- Return standard types — no `Mono<>` or `Flux<>`.

```java
@RestController
@RequestMapping("/api/remittances")
@RequiredArgsConstructor
public class RemittanceController {
    private final RemittanceService remittanceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RemittanceResponse createRemittance(@Valid @RequestBody CreateRemittanceRequest request) {
        return remittanceService.create(request);
    }
}
```

## 4. Infrastructure Layer

### 4.1 Database (JPA)

- JPA entities are **separate** from domain models. Use `@Entity` classes with `@Table`.
- Mappers (MapStruct) convert between JPA entities and domain models.
- Use Spring Data JPA repositories wrapped by an adapter that implements the domain port.

```java
// infrastructure/persistence/RemittanceEntity.java (JPA entity)
@Entity
@Table(name = "remittances")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
public class RemittanceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...
}

// infrastructure/persistence/RemittanceJpaRepository.java (Spring Data)
interface RemittanceJpaRepository extends JpaRepository<RemittanceEntity, Long> {
    Optional<RemittanceEntity> findByRemittanceId(UUID remittanceId);
}

// infrastructure/persistence/RemittanceRepositoryAdapter.java (implements domain port)
@Component
@RequiredArgsConstructor
class RemittanceRepositoryAdapter implements RemittanceRepository {
    private final RemittanceJpaRepository jpaRepository;
    private final RemittanceEntityMapper mapper;
    // ...
}
```

### 4.2 Temporal Workflows

- Workflow interfaces in `infrastructure/temporal/`.
- Activities are blocking methods (Spring MVC stack, no reactive).
- Retry policies defined per activity type.
- Temporal SDK activities are synchronous — no need for reactive wrapping.

### 4.3 External Integrations

- MPC Sidecar: gRPC client in `infrastructure/mpc/`.
- Solana RPC: HTTP client (RestClient or java.net.http.HttpClient) in `infrastructure/solana/`.
- Stripe: RestClient-based adapter in `infrastructure/stripe/`.
- Twilio: SDK-based adapter in `infrastructure/sms/`.
- FX Rate: RestClient-based adapter in `infrastructure/fx/`.

## 5. Object Mapping

Use **MapStruct** for all layer-boundary mapping. No manual field-by-field mapping.

| Direction | Method name |
|-----------|-------------|
| API -> Domain | `toDomain(apiModel)` |
| Domain -> API | `toResponse(domainModel)` |
| Entity -> Domain | `toDomain(entity)` |
| Domain -> Entity | `toEntity(domainModel)` |

```java
@Mapper(componentModel = "spring")
public interface RemittanceMapper {
    Remittance toDomain(RemittanceEntity entity);
    RemittanceEntity toEntity(Remittance domain);
    RemittanceResponse toResponse(Remittance domain);
}
```

## 6. Java Conventions

### 6.1 Language Level

- Java 25 with Spring Boot 4.0.3.
- Use modern Java features: `var`, records, sealed interfaces, pattern matching `switch`, text blocks.

### 6.2 Lombok Usage

| Annotation | Where |
|------------|-------|
| `@RequiredArgsConstructor` | Services, handlers, adapters (constructor injection) |
| `@Slf4j` | Services, handlers (logging) |
| `@Builder(toBuilder = true)` | All records and domain models |
| `@Getter` | Enums with fields, JPA entities |

**Never use:** `@Autowired`, `@Data` in production code, `@AllArgsConstructor` in domain models.

### 6.3 Dependency Injection

Constructor injection via `@RequiredArgsConstructor` with `private final` fields. No `@Autowired` anywhere.

### 6.4 Functional Over Imperative

- Streams over loops for transformations and filtering.
- `Optional` pipelines over null checks.
- Method references over lambdas when clear.
- Immutable return types: `List.of()`, `Map.of()`, `Stream.toList()`.

### 6.5 Import Order

```java
import static ...;         // Static imports first
import com.stablepay...;   // Internal imports
import com.other...;       // Third-party imports
import java...;            // Java standard library
import jakarta...;         // Jakarta imports
import org...;             // Framework imports
```

No wildcard imports.

## 7. Quick Reference Checklist

Before submitting code, verify:

- [ ] Domain models have zero Spring imports (Lombok only)
- [ ] Domain services use only `@Service`/`@Transactional` from Spring
- [ ] Domain layer does not import from `application` or `infrastructure`
- [ ] All mapping uses MapStruct, not manual field copying
- [ ] Repository interfaces are in `domain/port/outbound/`, implementations in `infrastructure/persistence/`
- [ ] Constructor injection via `@RequiredArgsConstructor`, no `@Autowired`
- [ ] Error codes follow `SP-XXXX` pattern
- [ ] Money values use `BigDecimal`
- [ ] Functional style: streams over loops, Optional pipelines over null checks
- [ ] Tests follow [TESTING_STANDARDS.md](TESTING_STANDARDS.md)
