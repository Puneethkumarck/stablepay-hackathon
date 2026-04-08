# Backend E2E Testing Guide

> How to test the StablePay backend end-to-end with full infrastructure.

---

## Prerequisites

| Requirement | Version | Check |
|---|---|---|
| Java | 25 (Temurin) | `java -version` |
| Docker | 20+ | `docker ps` |
| curl or Postman | any | `curl --version` |

---

## 1. Start the Full Stack

### Option A: Full Docker Stack (everything in containers)

```bash
# From repo root
make up
```

This builds the backend JAR, starts all 7 services, and waits for health checks:

```
postgres:18-alpine     → localhost:5432
redis:8-alpine         → localhost:6379
temporal:1.29.5        → localhost:7233
temporal-ui:2.48.1     → localhost:8088
mpc-sidecar-0          → localhost:50051 (gRPC)
mpc-sidecar-1          → localhost:50052 (gRPC)
backend (Spring Boot)  → localhost:8080
```

### Option B: Infrastructure in Docker + Backend Local (for development)

```bash
# Start infrastructure only
make infra

# Run backend locally with live reload
cd backend && ./gradlew bootRun
```

### Verify Everything Is Running

```bash
# Check all containers
make ps

# Health check
curl -s http://localhost:8080/actuator/health | jq .

# Expected: {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},...}}
```

### Service URLs

| Service | URL |
|---|---|
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Temporal UI | http://localhost:8088 |
| OpenAPI Spec | http://localhost:8080/v3/api-docs |

---

## 2. E2E Test Flow — Complete Remittance Lifecycle

The flow below tests every API endpoint in order, simulating a real USD→INR remittance from wallet creation to recipient delivery.

### Step 1: Create Wallet

```bash
curl -s -X POST http://localhost:8080/api/wallets \
  -H 'Content-Type: application/json' \
  -d '{"userId": "demo-user"}' | jq .
```

**Expected response (201 Created):**
```json
{
  "id": 1,
  "userId": "demo-user",
  "solanaAddress": "7Xf9...base58...",
  "availableBalance": 0,
  "totalBalance": 0,
  "createdAt": "2026-04-08T10:00:00Z"
}
```

**What to verify:**
- `solanaAddress` is a valid base58 Solana address (MPC DKG worked)
- Balance starts at zero
- Save the `id` for the next step

```bash
WALLET_ID=1  # from response
```

### Step 2: Fund Wallet

```bash
curl -s -X POST http://localhost:8080/api/wallets/${WALLET_ID}/fund \
  -H 'Content-Type: application/json' \
  -d '{"amount": 100.00}' | jq .
```

**Expected response (200 OK):**
```json
{
  "id": 1,
  "userId": "demo-user",
  "availableBalance": 100.000000,
  "totalBalance": 100.000000
}
```

**What to verify:**
- Both `availableBalance` and `totalBalance` show 100 USDC

### Step 3: Check FX Rate

```bash
curl -s http://localhost:8080/api/fx/USD-INR | jq .
```

**Expected response (200 OK):**
```json
{
  "rate": 83.25,
  "source": "open.er-api.com",
  "timestamp": "2026-04-08T10:00:00Z",
  "expiresAt": "2026-04-08T10:01:00Z"
}
```

**What to verify:**
- `rate` is a reasonable USD-INR rate (~83-85)
- `source` is either `"open.er-api.com"` (live) or `"fallback"` (if API unreachable, rate = 84.50)

### Step 4: Create Remittance

```bash
curl -s -X POST http://localhost:8080/api/remittances \
  -H 'Content-Type: application/json' \
  -d '{
    "senderId": "demo-user",
    "recipientPhone": "+919876543210",
    "amountUsdc": 25.00
  }' | jq .
```

**Expected response (201 Created):**
```json
{
  "id": 1,
  "remittanceId": "550e8400-...",
  "senderId": "demo-user",
  "recipientPhone": "+919876543210",
  "amountUsdc": 25.000000,
  "amountInr": 2081.25,
  "fxRate": 83.250000,
  "status": "INITIATED",
  "claimTokenId": "abc-123-...",
  "smsNotificationFailed": false,
  "createdAt": "2026-04-08T10:00:00Z"
}
```

**What to verify:**
- `status` is `INITIATED`
- `amountInr` = `amountUsdc` × `fxRate`
- `claimTokenId` is present (generated UUID)
- Save `remittanceId` and `claimTokenId`:

```bash
REMITTANCE_ID="550e8400-..."  # from response
CLAIM_TOKEN="abc-123-..."     # from response
```

**What to check in Temporal UI (http://localhost:8088):**
- A workflow named `remittance-<remittanceId>` should appear
- It should be in `Running` state
- Activities: `depositEscrow` should execute (deposits USDC to Solana escrow PDA)

### Step 5: Poll Remittance Status

```bash
curl -s http://localhost:8080/api/remittances/${REMITTANCE_ID} | jq .status
```

**Expected:** Status progresses from `"INITIATED"` → `"ESCROWED"` as the Temporal workflow deposits the escrow.

Poll every few seconds until you see `"ESCROWED"`:
```bash
# Poll loop
while true; do
  STATUS=$(curl -s http://localhost:8080/api/remittances/${REMITTANCE_ID} | jq -r .status)
  echo "Status: $STATUS"
  [ "$STATUS" = "ESCROWED" ] && break
  sleep 3
done
```

**What to verify:**
- Status transitions to `ESCROWED` (means on-chain deposit succeeded)
- Check Temporal UI — `depositEscrow` activity should show as completed

### Step 6: Get Claim Details

```bash
curl -s http://localhost:8080/api/claims/${CLAIM_TOKEN} | jq .
```

**Expected response (200 OK):**
```json
{
  "remittanceId": "550e8400-...",
  "senderId": "demo-user",
  "amountUsdc": 25.000000,
  "amountInr": 2081.25,
  "fxRate": 83.250000,
  "status": "ESCROWED",
  "claimed": false,
  "expiresAt": "2026-04-10T10:00:00Z"
}
```

**What to verify:**
- `claimed` is `false`
- `expiresAt` is 48 hours from creation
- This is what the recipient sees on the claim web page

### Step 7: Submit Claim (Recipient Action)

```bash
curl -s -X POST http://localhost:8080/api/claims/${CLAIM_TOKEN} \
  -H 'Content-Type: application/json' \
  -d '{"upiId": "recipient@upi"}' | jq .
```

**Expected response (200 OK):**
```json
{
  "remittanceId": "550e8400-...",
  "claimed": true,
  "status": "ESCROWED"
}
```

**What to verify:**
- `claimed` is `true`
- Check Temporal UI — a `claimSubmitted` signal should appear on the workflow
- Workflow should proceed: `releaseEscrow` → `disburseInr` → status updates

### Step 8: Poll for Delivery

```bash
while true; do
  STATUS=$(curl -s http://localhost:8080/api/remittances/${REMITTANCE_ID} | jq -r .status)
  echo "Status: $STATUS"
  [ "$STATUS" = "DELIVERED" ] || [ "$STATUS" = "DISBURSEMENT_FAILED" ] && break
  sleep 3
done
```

**Expected:** Status progresses `ESCROWED` → `CLAIMED` → `DELIVERED`

**What to verify in Temporal UI:**
- `releaseEscrow` activity completed (USDC released from escrow PDA)
- `disburseInr` activity completed (simulated INR payout)
- `updateRemittanceStatus` called for each transition
- Workflow reaches `Completed` state

### Step 9: Verify Final State

```bash
curl -s http://localhost:8080/api/remittances/${REMITTANCE_ID} | jq .
```

**Expected:** `"status": "DELIVERED"` — terminal state.

### Step 10: List Sender's Remittances

```bash
curl -s "http://localhost:8080/api/remittances?senderId=demo-user&page=0&size=20" | jq .
```

**Expected:** Paginated list with the remittance visible.

---

## 3. Error Scenario Testing

### Insufficient Balance

```bash
# Try to send more than the wallet has
curl -s -X POST http://localhost:8080/api/remittances \
  -H 'Content-Type: application/json' \
  -d '{
    "senderId": "demo-user",
    "recipientPhone": "+919876543210",
    "amountUsdc": 999999.00
  }' | jq .
```

**Expected:** `400 Bad Request` with `"errorCode": "SP-0002"` (insufficient balance)

### Duplicate Wallet

```bash
curl -s -X POST http://localhost:8080/api/wallets \
  -H 'Content-Type: application/json' \
  -d '{"userId": "demo-user"}' | jq .
```

**Expected:** `409 Conflict` with `"errorCode": "SP-0008"` (wallet already exists)

### Invalid Claim Token

```bash
curl -s http://localhost:8080/api/claims/nonexistent-token | jq .
```

**Expected:** `404 Not Found` with `"errorCode": "SP-0011"`

### Double Claim

```bash
# Submit claim again with same token
curl -s -X POST http://localhost:8080/api/claims/${CLAIM_TOKEN} \
  -H 'Content-Type: application/json' \
  -d '{"upiId": "someone@upi"}' | jq .
```

**Expected:** `409 Conflict` with `"errorCode": "SP-0012"` (already claimed)

### Unsupported Corridor

```bash
curl -s http://localhost:8080/api/fx/EUR-GBP | jq .
```

**Expected:** `400 Bad Request` with `"errorCode": "SP-0009"`

### Fund Nonexistent Wallet

```bash
curl -s -X POST http://localhost:8080/api/wallets/999999/fund \
  -H 'Content-Type: application/json' \
  -d '{"amount": 100.00}' | jq .
```

**Expected:** `404 Not Found` with `"errorCode": "SP-0006"`

---

## 4. Temporal Workflow Verification

Open **http://localhost:8088** (Temporal UI) to inspect workflows.

### What to Look For

| Check | Where | Expected |
|---|---|---|
| Workflow created | Workflow list | `remittance-<uuid>` in Running state |
| Deposit escrow | Activity history | `depositEscrow` completed with tx signature |
| SMS sent | Activity history | `sendClaimSms` completed (or failed if Twilio not configured) |
| Status updates | Activity history | `updateRemittanceStatus` calls for ESCROWED, CLAIMED, DELIVERED |
| Claim signal | Event history | `claimSubmitted` signal received |
| Release escrow | Activity history | `releaseEscrow` completed with tx signature |
| INR disbursement | Activity history | `disburseInr` completed (simulated) |
| Workflow complete | Workflow status | `Completed` with result containing `finalStatus: DELIVERED` |

### Timeout/Refund Test

To test the auto-refund path (requires waiting 48h or modifying config):

1. Create a remittance (Steps 1-4 above)
2. Do NOT submit a claim
3. Wait for the claim expiry timeout (default: 48h)
4. The workflow should automatically refund: `ESCROWED` → `REFUNDED`

For faster testing, set a shorter timeout in `application.yml`:
```yaml
stablepay:
  temporal:
    claim-expiry-timeout: PT1M  # 1 minute for testing
```

---

## 5. Database Verification

Connect to PostgreSQL directly:

```bash
docker exec -it $(docker ps -qf name=postgres) psql -U stablepay -d stablepay
```

### Verify Tables

```sql
-- Check wallet was created
SELECT id, user_id, solana_address, available_balance, total_balance FROM wallets;

-- Check remittance and its status
SELECT remittance_id, sender_id, amount_usdc, amount_inr, fx_rate, status FROM remittances;

-- Check claim token
SELECT token, remittance_id, claimed, upi_id, expires_at FROM claim_tokens;

-- Check Flyway migrations applied
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

**Expected Flyway output:**
```
 version |        description        | success
---------+---------------------------+---------
 1       | initial schema            | t
 2       | add upi id to claim tokens| t
 3       | add key share to wallets  | t
```

---

## 6. Redis Verification

```bash
docker exec -it $(docker ps -qf name=redis) redis-cli
```

```redis
KEYS *
# Expected: FX rate cache keys (if rate was recently fetched)

GET fx:USD:INR
# Expected: cached FX rate JSON (TTL ~60s)
```

---

## 7. Using the Postman Collection

Import `docs/StablePay.postman_collection.json` into Postman.

### Setup

1. Set variable `baseUrl` = `http://localhost:8080`
2. Set variable `userId` = `demo-user`

### Run the E2E Flow

Open the **"E2E Flow"** folder and run requests in order (1-9). The collection uses Postman test scripts to auto-capture:
- `walletId` from Create Wallet response
- `remittanceId` from Create Remittance response
- `claimToken` from Create Remittance response

Each subsequent request uses these variables automatically.

---

## 8. Cleanup

```bash
# Stop everything
make down

# Stop and wipe all data (PostgreSQL volumes, etc.)
make clean
```

---

## Error Code Reference

| Code | HTTP | Meaning |
|---|---|---|
| SP-0002 | 400 | Insufficient wallet balance |
| SP-0003 | 400 | Request validation error |
| SP-0006 | 404 | Wallet not found |
| SP-0007 | 503 | Treasury depleted |
| SP-0008 | 409 | Wallet already exists for user |
| SP-0009 | 400 | Unsupported currency corridor |
| SP-0010 | 404 | Remittance not found |
| SP-0011 | 404 | Claim token not found |
| SP-0012 | 409 | Claim already submitted |
| SP-0013 | 410 | Claim token expired |
| SP-0014 | 409 | Invalid remittance state |

---

## Quick Reference: Full E2E Script

Copy-paste this to run the entire flow in one go:

```bash
#!/bin/bash
set -euo pipefail
BASE=http://localhost:8080/api
USER="e2e-$(date +%s)"

echo "=== Step 1: Create Wallet ==="
WALLET=$(curl -sf -X POST $BASE/wallets -H 'Content-Type: application/json' -d "{\"userId\":\"$USER\"}")
WALLET_ID=$(echo $WALLET | jq -r .id)
echo "Wallet ID: $WALLET_ID"

echo "=== Step 2: Fund Wallet ==="
curl -sf -X POST $BASE/wallets/$WALLET_ID/fund -H 'Content-Type: application/json' -d '{"amount":100.00}' | jq .availableBalance

echo "=== Step 3: Check FX Rate ==="
curl -sf $BASE/fx/USD-INR | jq .rate

echo "=== Step 4: Create Remittance ==="
REM=$(curl -sf -X POST $BASE/remittances -H 'Content-Type: application/json' \
  -d "{\"senderId\":\"$USER\",\"recipientPhone\":\"+919876543210\",\"amountUsdc\":25.00}")
REM_ID=$(echo $REM | jq -r .remittanceId)
TOKEN=$(echo $REM | jq -r .claimTokenId)
echo "Remittance: $REM_ID | Token: $TOKEN"

echo "=== Step 5: Poll for ESCROWED ==="
for i in $(seq 1 20); do
  STATUS=$(curl -sf $BASE/remittances/$REM_ID | jq -r .status)
  echo "  Status: $STATUS"
  [ "$STATUS" = "ESCROWED" ] && break
  sleep 3
done

echo "=== Step 6: Get Claim ==="
curl -sf $BASE/claims/$TOKEN | jq '{claimed, amountInr, expiresAt}'

echo "=== Step 7: Submit Claim ==="
curl -sf -X POST $BASE/claims/$TOKEN -H 'Content-Type: application/json' -d '{"upiId":"recipient@upi"}' | jq .claimed

echo "=== Step 8: Poll for DELIVERED ==="
for i in $(seq 1 20); do
  STATUS=$(curl -sf $BASE/remittances/$REM_ID | jq -r .status)
  echo "  Status: $STATUS"
  [ "$STATUS" = "DELIVERED" ] || [ "$STATUS" = "DISBURSEMENT_FAILED" ] && break
  sleep 3
done

echo "=== Step 9: Final State ==="
curl -sf $BASE/remittances/$REM_ID | jq '{status, amountUsdc, amountInr}'

echo "=== Step 10: List Remittances ==="
curl -sf "$BASE/remittances?senderId=$USER&page=0&size=20" | jq '.totalElements'

echo "=== DONE ==="
```
