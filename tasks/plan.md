# Fix MPC 2-of-2 Threshold Signing — Implementation Plan

## Problem Statement

MPC wallet creation (DKG) works because both sidecars participate via P2P. However, MPC **signing** fails because:

1. **Gap 1 — Key share lost**: During DKG, sidecar-1 generates its key share but the backend never retrieves it. Only sidecar-0's share is stored in `wallets.key_share_data`.
2. **Gap 2 — Peer not triggered**: `signTransaction()` sends a `SignRequest` to sidecar-0 only. There is no `triggerPeerSigning()` — unlike DKG which has `triggerPeerKeygen()`.
3. **Gap 3 — Incomplete party list**: `addSigningPartyIds(partyId)` adds only party 0. For 2-of-2, both party IDs must be listed.

Result: sidecar-0 starts the signing protocol but sidecar-1 never joins. The P2P ceremony hangs until the 30s gRPC deadline.

## Constraints

- **Go sidecar needs zero changes.** `signing.go` already accepts `keyShareData` per-party and coordinates via P2P. The protocol is correct — only the Java orchestration is wrong.
- **Proto definitions need zero changes.** `SignRequest` already has `key_share_data`, `signing_party_ids[]`, and `peer_addresses`.
- **All changes are in the Java backend.**

## Design Decision

Store **both** parties' key shares in the backend during DKG. During signing, send each sidecar its own key share. This mirrors the DKG pattern (primary blocking call + `triggerPeer*` async calls).

Alternative considered: have each sidecar persist its own key share. Rejected because it adds state to stateless sidecars, complicates container restarts, and requires sidecar code changes.

## Dependency Graph

```
V4 migration (DB column)
       │
       ▼
GeneratedKey + Wallet (domain models) ──► WalletEntity + Mapper (infra DB)
       │                                          │
       ▼                                          ▼
CreateWalletHandler (stores both shares)   WalletFixtures (test)
       │
       ▼
MpcWalletClient port (new signature)
       │
       ├──► MpcWalletGrpcClient (trigger peer signing)
       │         │
       │         ▼
       │    MpcWalletGrpcClientTest (update tests)
       │
       └──► SolanaTransactionServiceAdapter (pass both shares)
                 │
                 ▼
            E2E test (make e2e)
```

## Tasks

### Task 1 — Add `peerKeyShareData` column to wallets table

**What:** Flyway migration `V4__add_peer_key_share_to_wallets.sql`

**Files:**
- `backend/src/main/resources/db/migration/V4__add_peer_key_share_to_wallets.sql` (new)

**Changes:**
```sql
ALTER TABLE wallets ADD COLUMN peer_key_share_data BYTEA;
```

**Acceptance criteria:**
- Migration runs without error on clean DB and on existing DB with data
- Column is nullable (existing wallets have NULL, which is fine)

**Verification:**
- `./gradlew test` passes (TestContainers applies migrations)

---

### Task 2 — Add `peerKeyShareData` to domain models

**What:** Thread the new field through `GeneratedKey` → `Wallet` → `WalletEntity` → mapper.

**Files:**
- `backend/src/main/java/com/stablepay/domain/wallet/model/GeneratedKey.java` — add `byte[] peerKeyShareData` field (nullable, no `requireNonNull`)
- `backend/src/main/java/com/stablepay/domain/wallet/model/Wallet.java` — add `byte[] peerKeyShareData` field
- `backend/src/main/java/com/stablepay/infrastructure/db/wallet/WalletEntity.java` — add `@Column(name = "peer_key_share_data") private byte[] peerKeyShareData`
- `backend/src/main/java/com/stablepay/infrastructure/db/wallet/WalletEntityMapper.java` — no code change needed (MapStruct auto-maps matching field names)

**Acceptance criteria:**
- `GeneratedKey` accepts optional `peerKeyShareData`
- `Wallet` record includes `peerKeyShareData`
- `WalletEntity` maps to/from `peer_key_share_data` column
- Existing tests compile (field defaults to null where not set)

**Verification:**
- `./gradlew compileJava` succeeds
- MapStruct generates mapper with `peerKeyShareData` field

---

### Task 3 — Store peer key share during DKG

**What:** In `MpcWalletGrpcClient.generateKey()`, read sidecar-1's `keyShareData` from the peer future response. Return it in `GeneratedKey.peerKeyShareData`. In `CreateWalletHandler`, store it in `Wallet.peerKeyShareData`.

**Files:**
- `backend/src/main/java/com/stablepay/infrastructure/mpc/MpcWalletGrpcClient.java`
  - In `generateKey()` (line ~98): change peer futures from fire-and-forget to `.thenApply(r -> r.getKeyShareData().toByteArray())`. Collect the first peer's key share bytes.
  - Set `peerKeyShareData` on the returned `GeneratedKey`
- `backend/src/main/java/com/stablepay/domain/wallet/handler/CreateWalletHandler.java`
  - Line 38: add `.peerKeyShareData(generatedKey.peerKeyShareData())` to the wallet builder

**Acceptance criteria:**
- After DKG, `wallet.peerKeyShareData` contains sidecar-1's `LocalPartySaveData` JSON bytes
- If peer response fails, `peerKeyShareData` is null (degraded but non-breaking)
- `wallet.keyShareData` still contains sidecar-0's share (unchanged)

**Verification:**
- `./gradlew test` passes
- Docker `make up` + create wallet → query DB → both `key_share_data` and `peer_key_share_data` are non-null

---

### Task 4 — Implement 2-party signing orchestration

**What:** Mirror the DKG pattern for signing. Add `triggerPeerSigning()` to `MpcWalletGrpcClient`. Update `signTransaction()` to include all party IDs and trigger peers.

**Files:**
- `backend/src/main/java/com/stablepay/domain/wallet/port/MpcWalletClient.java`
  - Change signature: `byte[] signTransaction(byte[] transactionBytes, byte[] keyShareData, byte[] peerKeyShareData)`
- `backend/src/main/java/com/stablepay/infrastructure/mpc/MpcWalletGrpcClient.java`
  - Update `signTransaction()`:
    1. Add all party IDs to `signingPartyIds` (both 0 and 1)
    2. Add new method `triggerPeerSigning(ceremonyId, peer, peerKeyShareData, transactionBytes)` — sends `SignRequest` to sidecar-1 with sidecar-1's key share, correct partyId, peer addresses, and the same ceremonyId
    3. Fire peer signing async before the primary blocking call (same pattern as DKG)
- `backend/src/main/java/com/stablepay/infrastructure/solana/SolanaTransactionServiceAdapter.java`
  - Line 60: pass `wallet.peerKeyShareData()` as third argument to `signTransaction()`

**Key details for `triggerPeerSigning`:**
```java
private CompletableFuture<SignResponse> triggerPeerSigning(
        String ceremonyId, PeerSidecar peer, byte[] peerKeyShareData, byte[] message) {
    return CompletableFuture.supplyAsync(() -> {
        var peerRequest = SignRequest.newBuilder()
            .setCeremonyId(ceremonyId)
            .setPartyId(peer.partyId())
            .setThreshold(threshold)
            .addAllSigningPartyIds(allPartyIds())  // [0, 1]
            .setKeyShareData(ByteString.copyFrom(peerKeyShareData))
            .setMessage(ByteString.copyFrom(message))
            .putAllPeerAddresses(peer.peerAddresses())
            .build();
        return stubWithDeadline(peer.stub()).sign(peerRequest);
    });
}
```

**Primary request changes:**
```java
// Before: .addSigningPartyIds(partyId)         — only party 0
// After:  .addAllSigningPartyIds(allPartyIds()) — [0, 1]
```

**Acceptance criteria:**
- Both sidecars receive `SignRequest` with their own key share
- Both sidecars have the full `signingPartyIds` list [0, 1]
- P2P signing ceremony completes between the two sidecars
- Primary call returns 64-byte Ed25519 signature

**Verification:**
- `./gradlew test` passes
- Unit tests for signing updated to match new request shape

---

### Task 5 — Update unit tests

**What:** Update all tests that touch the changed interfaces.

**Files:**
- `backend/src/testFixtures/java/com/stablepay/testutil/MpcFixtures.java` — add `SOME_PEER_KEY_SHARE_DATA`
- `backend/src/testFixtures/java/com/stablepay/testutil/WalletFixtures.java` — add `SOME_PEER_KEY_SHARE_DATA`, update `walletBuilder()`
- `backend/src/test/java/com/stablepay/infrastructure/mpc/MpcWalletGrpcClientTest.java`
  - Update `EXPECTED_SIGN_REQUEST` to include both party IDs in `signingPartyIds`
  - Update `signTransaction()` calls to pass 3 args
  - Update keygen test to verify `peerKeyShareData` in result (or null)
- `backend/src/test/java/com/stablepay/domain/wallet/handler/CreateWalletHandlerTest.java` — update expected wallet to include `peerKeyShareData`
- `backend/src/test/java/com/stablepay/infrastructure/solana/SolanaTransactionServiceAdapterTest.java` — update `signTransaction` mock to accept 3 args
- Any other test calling `signTransaction(bytes, bytes)` — grep for exact call sites

**Acceptance criteria:**
- All existing tests pass with updated signatures
- No `any()` or `anyString()` matchers (per testing standards)
- BDDMockito `given()`/`then()` only

**Verification:**
- `./gradlew test` — 0 failures
- `./gradlew spotlessCheck` — formatting clean

---

### Checkpoint: Unit Tests Green

Before proceeding to E2E, confirm:
```bash
cd backend && ./gradlew build   # compile + format + all tests
```

---

### Task 6 — E2E validation

**What:** Start the full Docker stack and run Newman E2E tests. The Temporal workflow should now progress past `depositEscrow` because MPC signing completes.

**Steps:**
1. `make down && make up` — rebuild with changes
2. Verify backend logs show `"Temporal WorkerFactory started"` and `"MPC signing completed"`
3. `make e2e` — all 9 assertions should pass
4. Check Temporal UI at http://localhost:8088 — workflow should reach ESCROWED status

**Acceptance criteria:**
- MPC signing completes in <5s (no timeout)
- Remittance status progresses: INITIATED → ESCROWED
- Claim submission returns 200 (not 409)
- Workflow continues to CLAIMED → DELIVERED (or DISBURSEMENT_FAILED if Transak is off)

**Verification:**
- `make e2e` — 9/9 assertions pass, 0 failures
- Backend logs show no `DEADLINE_EXCEEDED` errors
- Temporal UI shows completed workflow activities

---

## Files Changed (Summary)

| File | Change Type |
|---|---|
| `db/migration/V4__add_peer_key_share_to_wallets.sql` | New |
| `domain/wallet/model/GeneratedKey.java` | Add field |
| `domain/wallet/model/Wallet.java` | Add field |
| `domain/wallet/port/MpcWalletClient.java` | Change signature |
| `domain/wallet/handler/CreateWalletHandler.java` | Store peer share |
| `infrastructure/db/wallet/WalletEntity.java` | Add column |
| `infrastructure/mpc/MpcWalletGrpcClient.java` | DKG capture + signing trigger |
| `infrastructure/solana/SolanaTransactionServiceAdapter.java` | Pass peer share |
| `testutil/MpcFixtures.java` | Add fixture |
| `testutil/WalletFixtures.java` | Add fixture |
| `MpcWalletGrpcClientTest.java` | Update requests + signatures |
| `CreateWalletHandlerTest.java` | Update expected wallet |
| `SolanaTransactionServiceAdapterTest.java` | Update mock call |

**Go sidecar: 0 files changed**
**Proto definitions: 0 files changed**
