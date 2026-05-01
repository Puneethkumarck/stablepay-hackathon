---
title: KMS Envelope Encryption for MPC Key Shares
status: approved
created: 2026-04-22
updated: 2026-04-22
issue: TBD
revision: 2
---

# KMS Envelope Encryption for MPC Key Shares — Spec

## 1. Objective

Encrypt MPC key shares at rest using AWS KMS envelope encryption. Currently `key_share_data` and `peer_key_share_data` are stored as plaintext BYTEA in PostgreSQL. A database compromise would expose both halves of every wallet's private key.

After this change, key shares are encrypted with a per-wallet data encryption key (DEK), and each DEK is wrapped by a single AWS KMS master key. An attacker needs both database access **and** KMS `Decrypt` permission to recover any key material.

## 2. Threat Model

| Threat | Before | After |
|---|---|---|
| DB dump (SQL injection, backup leak) | Full private key reconstruction possible | Ciphertext only — useless without KMS |
| DB + app server compromise | Full access | Still needs IAM role with KMS Decrypt permission |
| KMS key ARN leak | N/A | Useless without DB access + IAM credentials |
| Memory dump of running process | Key shares in memory during signing | Same — unavoidable during signing window |
| KMS master key deletion | N/A | All DEKs become permanently unwrappable — key shares irrecoverable. Mitigated by KMS key deletion waiting period (7–30 days) and IAM policy restricting `ScheduleKeyDeletion` |
| KMS master key rotation | N/A | Transparent — AWS KMS automatic annual rotation re-wraps new `GenerateDataKey` calls under the new key material; existing encrypted DEKs remain decryptable via the original key material stored by KMS |

## 3. Design Decisions (from grill session)

| # | Decision | Choice | Rationale |
|---|---|---|---|
| 1 | Encryption boundary | `WalletRepositoryAdapter` | Infrastructure layer in hexagonal arch; explicit, testable |
| 2 | What to encrypt | `key_share_data` + `peer_key_share_data` | Public key is public by definition |
| 3 | KMS key model | One shared master key, unique DEK per wallet | $1/mo total; standard envelope encryption pattern |
| 4 | DEK storage | Same row — new `key_share_dek` column | Always fetched together, no join overhead |
| 5 | Migration strategy | Big bang — encrypt 29 existing wallets | Seconds to run, no dual-read complexity |
| 6 | Local KMS | LocalStack in docker-compose | Real KMS implementation, not mocked |
| 7 | AWS SDK | AWS SDK v2 sync (`software.amazon.awssdk:kms`) | Current standard, minimal dependency |
| 8 | Encryption algorithm | AES-256-GCM | Authenticated encryption — tamper detection |
| 9 | KMS unavailability | Fail fast, Temporal retries | KMS 99.999% SLA; no plaintext caching |
| 10 | Configuration | `stablepay.kms.enabled` (required, no default) + no-op fallback | Fail-closed: missing property = startup failure. Explicit `false` required to disable |

## 4. Envelope Encryption Flow

### 4.1 Write path (wallet creation — first save only)

```text
CreateWalletHandler
  → MPC DKG ceremony → GeneratedKey{keyShareData, peerKeyShareData}
  → WalletRepositoryAdapter.save(wallet)
    → Detect: entity.keyShareDek == null → first save, encrypt key shares
    → KeyShareEncryptor.encrypt(keyShareData, peerKeyShareData)
      → KMS: GenerateDataKey(masterKeyArn, AES_256)
      → KMS returns: {plaintextDEK, encryptedDEK}
      → AES-256-GCM encrypt(keyShareData, plaintextDEK) → ciphertext1
      → AES-256-GCM encrypt(peerKeyShareData, plaintextDEK) → ciphertext2
      → zero out plaintextDEK
      → return EncryptedKeyMaterial{ciphertext1, ciphertext2, encryptedDEK, iv1, iv2}
    → Store: encrypted shares + encryptedDEK + IVs in wallets row
```

### 4.1.1 Subsequent saves (balance updates)

```text
FundWalletHandler / other balance mutations
  → WalletRepositoryAdapter.save(wallet)
    → Detect: entity.keyShareDek != null → key shares already encrypted
    → Skip encryption — persist only non-key-share fields (balance, status, etc.)
    → Key share columns and DEK/IV columns are NOT overwritten
```

This avoids unnecessary KMS calls and DEK regeneration on balance-only updates.

### 4.2 Read path (transaction signing)

```text
SolanaTransactionServiceAdapter.depositEscrow()
  → walletRepository.findBySolanaAddress(address)
    → WalletRepositoryAdapter loads WalletEntity from DB
    → KeyShareEncryptor.decrypt(encryptedKeyShareData, encryptedDEK)
      → KMS: Decrypt(encryptedDEK)
      → KMS returns: {plaintextDEK}
      → AES-256-GCM decrypt(ciphertext, plaintextDEK) → keyShareData
      → zero out plaintextDEK
      → return keyShareData
    → KeyShareEncryptor.decrypt(encryptedPeerKeyShareData, encryptedDEK)
    → Return Wallet domain model with decrypted key shares
  → mpcWalletClient.signTransaction(keyShareData, peerKeyShareData)
```

### 4.3 DEK reuse per wallet

Both `key_share_data` and `peer_key_share_data` in the same wallet use the **same DEK**. One `GenerateDataKey` call per wallet creation, one `Decrypt` call per signing operation to unwrap the DEK, then decrypt both shares.

**Critical invariant:** Each share MUST have a unique 12-byte GCM nonce (IV). Reusing a nonce with the same DEK would break GCM's confidentiality guarantees. The `key_share_iv` and `peer_key_share_iv` columns enforce this — each is generated independently via `SecureRandom`.

## 5. Schema Changes

### V10 migration

```sql
-- Step 1: Add columns for encrypted storage
ALTER TABLE wallets ADD COLUMN key_share_dek BYTEA;
ALTER TABLE wallets ADD COLUMN key_share_iv BYTEA;
ALTER TABLE wallets ADD COLUMN peer_key_share_iv BYTEA;

-- Step 2: After Java migration job runs, enforce NOT NULL
-- (applied in V11 after backfill)
```

### V11 migration (after backfill)

```sql
ALTER TABLE wallets ALTER COLUMN key_share_dek SET NOT NULL;
ALTER TABLE wallets ALTER COLUMN key_share_iv SET NOT NULL;
ALTER TABLE wallets ALTER COLUMN peer_key_share_iv SET NOT NULL;
```

### Column semantics after migration

| Column | Before | After |
|---|---|---|
| `key_share_data` | Plaintext key share | AES-256-GCM ciphertext (includes auth tag) |
| `peer_key_share_data` | Plaintext peer key share | AES-256-GCM ciphertext (includes auth tag) |
| `key_share_dek` | _(new)_ | KMS-encrypted DEK (shared by both shares in the row) |
| `key_share_iv` | _(new)_ | 12-byte GCM nonce for key_share_data |
| `peer_key_share_iv` | _(new)_ | 12-byte GCM nonce for peer_key_share_data |

## 6. Domain Port

```java
package com.stablepay.domain.wallet.port;

public interface KeyShareEncryptor {
    EncryptedKeyMaterial encrypt(byte[] keyShareData, byte[] peerKeyShareData);
    DecryptedKeyMaterial decrypt(byte[] encKeyShare, byte[] encPeerKeyShare,
                                 byte[] encDek, byte[] iv, byte[] peerIv);
}
```

```java
package com.stablepay.domain.wallet.model;

@Builder(toBuilder = true)
public record EncryptedKeyMaterial(
    byte[] encryptedKeyShareData,
    byte[] encryptedPeerKeyShareData,
    byte[] encryptedDek,
    byte[] keyShareIv,
    byte[] peerKeyShareIv
) {}
```

```java
@Builder(toBuilder = true)
public record DecryptedKeyMaterial(
    byte[] keyShareData,
    byte[] peerKeyShareData
) {}
```

## 7. Infrastructure Implementations

### 7.1 KmsKeyShareEncryptor

```java
@ConditionalOnProperty(name = "stablepay.kms.enabled", havingValue = "true")
```

- Calls `KmsClient.generateDataKey()` with `KeySpec.AES_256`
- Uses `javax.crypto.Cipher` with `AES/GCM/NoPadding` (12-byte IV, 128-bit auth tag)
- Each encrypt call generates a unique IV via `SecureRandom`
- Zeroes plaintext DEK after use (`Arrays.fill(dek, (byte) 0)`)
- **Known limitation:** `Arrays.fill` is best-effort in Java — the JIT compiler may optimize it away, and the GC may have already copied the byte array. There is no reliable way to wipe memory in Java without JNI. This is an accepted risk; the primary protection is KMS access control, not memory scrubbing.

### 7.2 NoOpKeyShareEncryptor (fallback)

```java
@ConditionalOnProperty(name = "stablepay.kms.enabled", havingValue = "false")
```

**No `matchIfMissing`** — if `stablepay.kms.enabled` is not set, the application fails to start. This prevents accidental plaintext storage in production due to a missing configuration value.

- Passes key shares through unchanged
- Returns null/empty for DEK and IV fields
- Used when KMS is explicitly disabled (local dev without LocalStack)
- Logs `WARN` on startup: "KMS disabled — key shares stored unencrypted"

## 8. Configuration

### application.yml

```yaml
stablepay:
  kms:
    enabled: ${KMS_ENABLED}          # Required — no default. Must be explicitly set.
    key-arn: ${KMS_KEY_ARN:}
    endpoint: ${KMS_ENDPOINT:}       # Only for LocalStack; validated to be empty in production
    region: ${KMS_REGION:us-east-1}
```

### KmsProperties

```java
@ConfigurationProperties(prefix = "stablepay.kms")
@Builder(toBuilder = true)
public record KmsProperties(
    @NotBlank String keyArn,
    String endpoint,
    String region
) {}
```

### KmsConfig endpoint guard

`KmsConfig` validates at startup that `endpoint` is blank when the active Spring profile is `production`. This prevents LocalStack from being accidentally used in production:

```java
@PostConstruct
void validateEndpoint() {
    if (environment.matchesProfiles("production") && StringUtils.hasText(kmsProperties.endpoint())) {
        throw new IllegalStateException("KMS endpoint override is not allowed in production profile");
    }
}
```

### docker-compose.yml additions

```yaml
localstack:
  image: localstack/localstack:4.0
  ports:
    - "4566:4566"
  environment:
    SERVICES: kms
  healthcheck:
    test: ["CMD-SHELL", "awslocal kms list-keys || exit 1"]
    interval: 5s
    timeout: 5s
    retries: 10
    start_period: 10s
```

Backend environment additions:

```yaml
KMS_ENABLED: ${KMS_ENABLED:-false}     # docker-compose default is false; .env overrides
KMS_KEY_ARN: ${KMS_KEY_ARN:-}
KMS_ENDPOINT: ${KMS_ENDPOINT:-}
KMS_REGION: ${KMS_REGION:-us-east-1}
```

Note: `docker-compose.yml` uses `:-false` default so `docker compose up` works without a `.env` file. The Spring Boot `application.yml` has no default — the value must be explicitly provided by the environment.

### LocalStack KMS key bootstrap

A LocalStack init hook script (`localstack/init/ready.d/create-kms-key.sh`) creates the KMS key on container startup and writes the ARN to a known location:

```bash
#!/bin/bash
KEY_ARN=$(awslocal kms create-key --key-spec SYMMETRIC_DEFAULT --key-usage ENCRYPT_DECRYPT --query 'KeyMetadata.Arn' --output text)
echo "KMS_KEY_ARN=${KEY_ARN}" > /tmp/kms-key-arn.env
echo "Created KMS key: ${KEY_ARN}"
```

The `docker-compose.yml` mounts this init directory:

```yaml
localstack:
  volumes:
    - ./localstack/init/ready.d:/etc/localstack/init/ready.d:ro
```

For local development, the `.env` file should contain:

```dotenv
KMS_ENABLED=true
KMS_KEY_ARN=arn:aws:kms:us-east-1:000000000000:key/local-dev-key
KMS_ENDPOINT=http://localhost:4566
```

The ARN value is deterministic in LocalStack when only one key exists, or can be retrieved after first boot via `awslocal kms list-keys`.

## 9. Migration Backfill Strategy

A one-time `ApplicationRunner` bean (conditional on `BACKFILL_KEY_SHARES=true`):

1. Query wallet IDs with `key_share_dek IS NULL` (IDs only — not key share data)
2. Process wallets one at a time in individual transactions:
   - Load single wallet by ID within a `@Transactional` boundary
   - Read plaintext `key_share_data` and `peer_key_share_data`
   - Call `KeyShareEncryptor.encrypt(keyShareData, peerKeyShareData)`
   - Write encrypted data + DEK + IVs back to the row
   - Commit transaction
   - Zero out local plaintext byte arrays (`Arrays.fill`)
3. Log progress: `Migrated wallet {id} ({n}/{total})`
4. On completion, log summary
5. If any wallet fails, log the error and continue with the remaining wallets (idempotent — re-running skips already-migrated wallets via the `IS NULL` check)

**Batch size:** One wallet per transaction. With 29 wallets this completes in seconds. No bulk loading of plaintext key material into memory.

**Coordination with V11:** The backfill and V11 migration are separate deployment steps. Sequence:
1. Deploy with V10 migration (adds nullable columns)
2. Run backfill (`BACKFILL_KEY_SHARES=true`)
3. Verify all wallets have `key_share_dek IS NOT NULL`
4. Deploy with V11 migration (enforces NOT NULL)

## 10. Affected Files

### New files

| File | Purpose |
|---|---|
| `domain/wallet/port/KeyShareEncryptor.java` | Port interface |
| `domain/wallet/model/EncryptedKeyMaterial.java` | Encrypt result record |
| `domain/wallet/model/DecryptedKeyMaterial.java` | Decrypt result record |
| `infrastructure/kms/KmsKeyShareEncryptor.java` | KMS implementation |
| `infrastructure/kms/KmsConfig.java` | KmsClient bean + conditional config |
| `infrastructure/kms/KmsProperties.java` | Config properties |
| `infrastructure/kms/NoOpKeyShareEncryptor.java` | Passthrough fallback |
| `db/migration/V10__add_key_share_encryption_columns.sql` | Add DEK + IV columns |
| `db/migration-pending/V11__enforce_key_share_encryption_not_null.sql` | NOT NULL after backfill (promote manually) |
| `infrastructure/db/wallet/KeyShareBackfillRunner.java` | One-time migration runner |
| `infrastructure/db/wallet/KeyShareReverseBackfillRunner.java` | Rollback runner — decrypts shares back to plaintext |
| `localstack/init/ready.d/create-kms-key.sh` | LocalStack KMS key bootstrap script |

### Modified files

| File | Change |
|---|---|
| `infrastructure/db/wallet/WalletEntity.java` | Add `keyShareDek`, `keyShareIv`, `peerKeyShareIv` fields |
| `infrastructure/db/wallet/WalletEntityMapper.java` | Map new fields; add `@Mapping(target = "keyShareDek", ignore = true)`, `@Mapping(target = "keyShareIv", ignore = true)`, `@Mapping(target = "peerKeyShareIv", ignore = true)` for domain→entity direction (encryption is handled by the adapter, not the mapper) |
| `infrastructure/db/wallet/WalletRepositoryAdapter.java` | Call encrypt on save, decrypt on read |
| `docker-compose.yml` | Add LocalStack service + KMS env vars |
| `application.yml` | Add `stablepay.kms` config block |
| `build.gradle.kts` | Add `software.amazon.awssdk:kms` dependency |

### Unchanged (no modifications needed)

| File | Why unchanged |
|---|---|
| `domain/wallet/model/Wallet.java` | Still carries plaintext key shares — encryption is below this layer |
| `domain/wallet/handler/CreateWalletHandler.java` | Unchanged — adapter handles encryption transparently |
| `infrastructure/solana/SolanaTransactionServiceAdapter.java` | Unchanged — receives decrypted shares from adapter |
| `infrastructure/mpc/MpcWalletGrpcClient.java` | Unchanged — receives plaintext shares |

## 11. Testing Strategy

### Unit tests

| Test | What it verifies |
|---|---|
| `KmsKeyShareEncryptorTest` | Encrypt/decrypt round-trip, unique IVs per call, DEK zeroed after use |
| `NoOpKeyShareEncryptorTest` | Passthrough behavior |
| `WalletRepositoryAdapterTest` | Encrypt called on save, decrypt called on read |

### Integration tests

| Test | What it verifies |
|---|---|
| `KmsKeyShareEncryptorIntegrationTest` | Real LocalStack KMS — GenerateDataKey + Decrypt round-trip |
| `WalletRepositoryIntegrationTest` | Full persist + read cycle with encryption enabled |
| `KeyShareBackfillRunnerIntegrationTest` | Backfill migrates plaintext to encrypted |

## 12. Task Breakdown

| Task | Issue | Description |
|---|---|---|
| 1 | STA-111 | Add LocalStack to docker-compose, KMS config, properties, and `KmsClient` bean |
| 2 | STA-112 | Implement `KeyShareEncryptor` port + `KmsKeyShareEncryptor` + `NoOpKeyShareEncryptor` |
| 3 | STA-113 | V10 migration, `WalletEntity` changes, `WalletRepositoryAdapter` encrypt/decrypt wiring |
| 4 | STA-114 | Backfill runner + V11 NOT NULL enforcement |
| 5 | STA-115 | Integration tests with LocalStack |

## 13. Rollback Plan

**Important:** After backfill, key shares in the DB are ciphertext. Simply setting `KMS_ENABLED=false` would cause `NoOpKeyShareEncryptor` to return ciphertext as if it were plaintext, breaking MPC signing silently.

### Rollback procedure (after backfill has run)

1. **Run reverse backfill first:** Set `KMS_REVERSE_BACKFILL_KEY_SHARES=true` to activate the `ApplicationRunner` that:
   - Loads each wallet with `key_share_dek IS NOT NULL`
   - Decrypts key shares using KMS (KMS must still be enabled during reverse backfill)
   - Writes plaintext key shares back and nulls DEK/IV columns
2. **Then** set `KMS_ENABLED=false`
3. Optionally drop DEK/IV columns via a rollback migration

### Rollback procedure (before backfill)

If issues arise before the backfill runs, simply set `KMS_ENABLED=false` — all key shares are still plaintext, and new wallets will continue to store plaintext.

## 14. Security Checklist

- [ ] KMS key ARN never logged at INFO level (mask in logs)
- [ ] Plaintext DEK zeroed (`Arrays.fill`) immediately after use (best-effort — see §7.1 known limitation)
- [ ] Decrypted key shares zeroed after MPC signing completes
- [ ] KMS key has restrictive IAM policy (only backend role can `GenerateDataKey` + `Decrypt`; no `ScheduleKeyDeletion`)
- [ ] LocalStack KMS never used in production (`KmsConfig` validates endpoint is blank in production profile)
- [ ] `NoOpKeyShareEncryptor` logs a WARN on startup: "KMS disabled — key shares stored unencrypted"
- [ ] GCM nonces generated via `SecureRandom`, never reused (unique per share per wallet)
- [ ] Auth tag failure on decrypt throws `AEADBadTagException` — propagated as a clear domain error, not swallowed
- [ ] `stablepay.kms.enabled` has no default in `application.yml` — missing value = startup failure (fail-closed)
- [ ] Backfill runner processes one wallet per transaction; plaintext arrays zeroed after each wallet
- [ ] KMS automatic key rotation enabled on the master key (annual rotation, transparent to application)
