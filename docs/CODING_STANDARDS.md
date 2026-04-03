# Coding Standards for StablePay Backend

Instructions for coding agents developing the StablePay Java/Spring Boot backend.

## 1. Architecture: Hexagonal (Ports and Adapters)

The backend follows a strict three-layer package structure under `com.stablepay`:

```
com.stablepay
  ├── domain/           # Core business logic, services, models, ports
  ├── application/      # Input adapters: REST controllers, config
  └── infrastructure/   # Output adapters: database, Temporal, MPC, Solana, Stripe, Twilio
```

**Rules:**
- `domain` MUST NOT import from `application` or `infrastructure`.
- `domain` services use Spring for DI (`@Service`) and transactions (`@Transactional`). This is an accepted pragmatic choice.
- `domain` models (records, value objects, enums) MUST NOT import Spring. Only Lombok is allowed on models.
- `application` depends on `domain`. It maps API models to domain models and delegates.
- `infrastructure` depends on `domain`. It implements domain ports and external integrations.
- Dependencies always point inward: `application` -> `domain` <- `infrastructure`.
- **Application MUST NOT call infrastructure directly.** The flow is always: `Controller` → `Domain Handler/Service` → `Outbound Port` → `Infrastructure Adapter`. Never skip the domain layer.

### 1.1 Anti-Corruption Layer

Each layer has its own models. MapStruct mappers exist at **each** layer boundary to prevent model leakage:

```
Application Layer          Domain Layer              Infrastructure Layer
─────────────────          ────────────              ────────────────────
CreateWalletRequest   →    (domain model)       →    WalletEntity
WalletResponse        ←    Wallet               ←    WalletEntity
FxRateResponse        ←    FxQuote              ←    (external API response)

WalletApiMapper            (no mapper needed      WalletEntityMapper
(application/mapper/)       when same model)      (infrastructure/persistence/)
```

**Rules:**
- Application DTOs (request/response records) live in `application/dto/`
- Application mappers (API ↔ Domain) live in `application/mapper/`
- Infrastructure entities live in `infrastructure/persistence/`
- Infrastructure mappers (Domain ↔ Entity) live in `infrastructure/persistence/`
- Domain models are the canonical representation — all other layers map to/from them

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

### 2.3 Command and Query Handlers

Domain logic is organized into **handlers** that separate read and write operations. Handlers are the domain's inbound ports — controllers call them, never outbound ports directly.

**Command Handlers** — mutating operations (create, update, delete):

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateWalletHandler {
    private final WalletRepository walletRepository;
    private final MpcWalletClient mpcWalletClient;
    private final TreasuryService treasuryService;

    @Transactional
    public Wallet handle(CreateWalletCommand command) {
        // orchestrate domain logic
    }
}
```

**Query Handlers** — read-only operations (get, list, search):

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class GetFxRateQueryHandler {
    private final FxRateProvider fxRateProvider;

    public FxQuote handle(String fromCurrency, String toCurrency) {
        return fxRateProvider.getRate(fromCurrency, toCurrency);
    }
}
```

**Naming conventions:**
- Commands: `{Action}{Entity}Handler` (e.g., `CreateWalletHandler`, `FundWalletHandler`)
- Queries: `{Get|List}{Entity}QueryHandler` (e.g., `GetFxRateQueryHandler`, `ListRemittancesQueryHandler`)
- Command records: `{Action}{Entity}Command` (e.g., `CreateWalletCommand`)

**Rules:**
- Controllers call handlers. Handlers call outbound ports. **Never skip the handler layer.**
- Command handlers use `@Transactional`. Query handlers typically do not.
- Each handler has a single `handle()` method — one handler per use case.
- Handlers depend only on outbound ports (interfaces), never on infrastructure classes.

**Allowed Spring annotations in domain handlers:**
- `@Service`, `@Component` — for DI registration
- `@Transactional` — for transaction management (commands)

**NOT allowed in domain:**
- `@RestController`, `@GetMapping`, etc. (application layer only)
- `@Entity`, `@Table`, `@Column` (infrastructure layer only)
- `@Autowired` (use `@RequiredArgsConstructor` instead)
- `@Cacheable` (infrastructure concern — caching belongs in adapters)

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

Controllers are thin adapters: validate input, map to domain, delegate to handler, map response. **Controllers call domain handlers — never outbound ports or infrastructure directly.**

```java
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {
    private final CreateWalletHandler createWalletHandler;
    private final FundWalletHandler fundWalletHandler;
    private final WalletApiMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
        var command = mapper.toCommand(request);
        var wallet = createWalletHandler.handle(command);
        return mapper.toResponse(wallet);
    }
}
```

**Call chain (always):**
```
HTTP Request → Controller → Mapper.toDomain() → Handler.handle() → Outbound Port → Adapter
HTTP Response ← Controller ← Mapper.toResponse() ← Handler result
```

**Rules:**
- Use Spring MVC annotations (`@RestController`, `@GetMapping`, etc.)
- Use `@Valid` for request validation
- Return standard types — no `Mono<>` or `Flux<>`
- **Inject domain handlers**, not outbound ports or infrastructure beans
- Map API DTOs to domain commands/models using application-layer MapStruct mappers
- Map domain results to API responses using application-layer MapStruct mappers

### 3.2 Application Mappers

Application-layer mappers convert between API DTOs and domain models. They live in `application/mapper/`.

```java
@Mapper(componentModel = "spring")
public interface WalletApiMapper {
    CreateWalletCommand toCommand(CreateWalletRequest request);
    WalletResponse toResponse(Wallet wallet);
}
```

These are separate from infrastructure mappers (which convert domain ↔ entity).

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

Use **MapStruct** for all layer-boundary mapping. No manual field-by-field mapping. **Separate mappers per layer boundary** — never combine application and infrastructure mapping in one mapper.

### 5.1 Application-Layer Mappers (`application/mapper/`)

Convert between API DTOs and domain models:

| Direction | Method name |
|-----------|-------------|
| API Request → Domain Command | `toCommand(request)` |
| API Request → Domain Model | `toDomain(request)` |
| Domain Model → API Response | `toResponse(domainModel)` |

```java
@Mapper(componentModel = "spring")
public interface WalletApiMapper {
    CreateWalletCommand toCommand(CreateWalletRequest request);
    WalletResponse toResponse(Wallet wallet);
}
```

### 5.2 Infrastructure-Layer Mappers (`infrastructure/persistence/`)

Convert between domain models and JPA entities:

| Direction | Method name |
|-----------|-------------|
| Entity → Domain | `toDomain(entity)` |
| Domain → Entity | `toEntity(domainModel)` |

```java
@Mapper(componentModel = "spring")
interface RemittanceEntityMapper {
    Remittance toDomain(RemittanceEntity entity);
    RemittanceEntity toEntity(Remittance domain);
}
```

**Never combine these** — application mapper and infrastructure mapper are separate interfaces in separate packages.

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

- [ ] **Call chain**: Controller → Handler → Outbound Port → Adapter (never skip domain)
- [ ] **Controllers inject handlers**, not outbound ports or infrastructure beans
- [ ] **Separate mappers per boundary**: application mapper (API ↔ domain) and infrastructure mapper (domain ↔ entity)
- [ ] Domain models have zero Spring imports (Lombok only)
- [ ] Domain handlers use only `@Service`/`@Transactional` from Spring
- [ ] Domain layer does not import from `application` or `infrastructure`
- [ ] All mapping uses MapStruct, not manual field copying
- [ ] Repository interfaces are in `domain/port/outbound/`, implementations in `infrastructure/persistence/`
- [ ] Constructor injection via `@RequiredArgsConstructor`, no `@Autowired`
- [ ] Error codes follow `SP-XXXX` pattern
- [ ] Money values use `BigDecimal`
- [ ] Functional style: streams over loops, Optional pipelines over null checks
- [ ] Tests follow [TESTING_STANDARDS.md](TESTING_STANDARDS.md)
