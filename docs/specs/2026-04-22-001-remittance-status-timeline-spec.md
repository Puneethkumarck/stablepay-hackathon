---
title: Remittance Status Timeline
status: approved
created: 2026-04-22
updated: 2026-04-22
issue: STA-106 (#224), STA-107 (#225), STA-108 (#226), STA-109 (#227)
revision: 3 (handler logic trace fixes)
---

# Remittance Status Timeline — Spec

## 1. Objective

Add a status timeline to the remittance flow so the mobile app can display a step-by-step progress tracker showing where a remittance is in its lifecycle. The timeline shows four fixed steps (INITIATED, ESCROWED, CLAIMED, DELIVERED) with timestamps, contextual messages, and completion status.

This is a **read-side enhancement only** — no changes to the existing remittance business logic or state machine.

## 2. User Stories

### US-1: View remittance timeline

**As a** sender,
**I want to** see a step-by-step progress tracker for my remittance,
**So that** I know exactly where my money is and what's happening next.

**Acceptance criteria:**
- `GET /api/remittances/{remittanceId}/timeline` returns a structured 4-step progress response
- Each step has: `step` (enum name), `status` (COMPLETED/CURRENT/PENDING/FAILED), `message` (human-readable), `completedAt` (nullable timestamp)
- Steps are always returned in order: INITIATED, ESCROWED, CLAIMED, DELIVERED
- Steps completed before the current status show `status: "COMPLETED"` with their `completedAt` timestamp
- The current active step shows `status: "CURRENT"` with `completedAt: null`
- Steps not yet reached show `status: "PENDING"` with `completedAt: null`
- If the remittance is in a failure or terminal non-delivery state, `failed: true` is set on the response — no failure-specific steps are exposed to the client
- Requires Bearer JWT authentication
- Ownership check: only the sender who created the remittance can view its timeline (reuse `GetRemittanceQueryHandler` for validation)

### US-2: Contextual step messages

**As a** sender,
**I want to** see helpful descriptions for each step,
**So that** I understand what's happening without technical jargon.

**Acceptance criteria:**

| Step | Status | Message |
|---|---|---|
| INITIATED | COMPLETED | "Payment received" |
| INITIATED | CURRENT | "Processing payment..." (fallback for pre-migration data only) |
| INITIATED | PENDING | "Payment received" |
| ESCROWED | COMPLETED | "Funds secured on-chain" |
| ESCROWED | CURRENT | "Securing funds on-chain..." |
| ESCROWED | PENDING | "Funds secured on-chain" |
| CLAIMED | COMPLETED | "Recipient claimed" |
| CLAIMED | CURRENT (smsNotificationFailed=false) | "SMS sent, waiting for recipient" |
| CLAIMED | CURRENT (smsNotificationFailed=true) | "Claim link available, waiting for recipient" |
| CLAIMED | PENDING | "Recipient claimed" |
| DELIVERED | COMPLETED | "INR deposited to recipient's bank" |
| DELIVERED | CURRENT | "Depositing INR to recipient's bank..." |
| DELIVERED | PENDING | "INR deposited to recipient's bank" |

**Message selection rules:**
- PENDING steps use the COMPLETED message (describes what the step will achieve)
- CURRENT steps use the in-progress message (describes what is actively happening)
- COMPLETED steps use the completion message (describes what happened)
- INITIATED/CURRENT is unreachable in normal flow since `CreateRemittanceHandler` inserts the INITIATED event in the same transaction as creation — included only as a defensive fallback for pre-migration remittances without events

**Step-to-status mapping:**
- When remittance status is INITIATED, the ESCROWED step is CURRENT
- When remittance status is ESCROWED, the CLAIMED step is CURRENT with the SMS-aware message
- When remittance status is CLAIMED, the DELIVERED step is CURRENT

### US-3: Automatic event logging

**As the** system,
**I want** every remittance status transition to be logged with a timestamp,
**So that** the timeline has accurate data for when each step completed.

**Acceptance criteria:**
- Every call to `UpdateRemittanceStatusHandler.handle()` inserts a row into `remittance_status_events`
- The event insert happens in the same transaction as the status update (no eventual consistency)
- If the transaction rolls back, no orphan event is created
- The initial INITIATED status is logged when `CreateRemittanceHandler` creates the remittance (after the first `remittanceRepository.save()` on line 62, using `saved.remittanceId()`)
- Events are append-only — never updated or deleted

## 3. Scope

### In scope
- New `remittance_status_events` database table (Flyway migration)
- Domain model: `RemittanceStatusEvent` record
- Port: `RemittanceStatusEventRepository` interface
- Infrastructure: JPA entity, mapper, repository adapter
- Handler change: `UpdateRemittanceStatusHandler` writes event in same transaction
- Handler change: `CreateRemittanceHandler` writes INITIATED event on creation
- New handler: `GetRemittanceTimelineHandler`
- New endpoint: `GET /api/remittances/{remittanceId}/timeline`
- New DTO: `RemittanceTimelineResponse` with `TimelineStep` records
- Controller mapper for timeline
- Unit tests for handler, mapper
- Controller test for new endpoint
- Integration test for event persistence

### Out of scope
- Funding order timeline (simple 2-step flow, not worth the complexity)
- WebSocket/SSE push notifications (client polls)
- Timeline for claim-side view (recipient doesn't need a progress tracker)
- Exposing failure states to frontend (hidden behind `failed: true` flag)
- Backfill migration for pre-existing remittances (hackathon — all demo data is freshly created)

## 4. Database Schema

### V9 Migration: `V9__remittance_status_events.sql`

```sql
CREATE TABLE remittance_status_events (
    id              BIGSERIAL       PRIMARY KEY,
    remittance_id   UUID            NOT NULL REFERENCES remittances(remittance_id),
    status          VARCHAR(30)     NOT NULL,
    message         VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_remittance_status_events_remittance_id
    ON remittance_status_events(remittance_id, created_at, id);
```

**Design decisions:**
- No `previous_status` column — derivable from row ordering
- No `metadata` JSONB column — the frontend only needs step name, message, and timestamp; escrow PDA and tx signature already live on the remittance row
- FK to `remittances(remittance_id)` ensures referential integrity
- Composite index on `(remittance_id, created_at, id)` for efficient timeline queries with deterministic ordering — `id` serves as tiebreaker when two events share the same `created_at` timestamp

## 5. Failure & Terminal State Definitions

The `RemittanceStatus` enum contains 10 values. Only 4 are happy-path steps shown in the timeline. The remaining 6 are classified as:

### Failure states (`failed: true`, step marked FAILED)

These represent processing errors. The happy-path step that was being attempted is marked FAILED:

| Failure Status | Transitions From | Failed Step (marked FAILED in timeline) |
|---|---|---|
| DEPOSIT_FAILED | INITIATED | ESCROWED |
| CLAIM_FAILED | ESCROWED | CLAIMED |
| DISBURSEMENT_FAILED | CLAIMED | DELIVERED |

### Terminal non-delivery states (`failed: true`, no step marked FAILED)

These are not processing errors — the remittance was stopped voluntarily or a non-happy-path operation failed. All completed steps stay COMPLETED, no CURRENT step, remaining steps PENDING:

| Status | Transitions From | Timeline Behavior |
|---|---|---|
| REFUNDED | ESCROWED | Completed steps stay COMPLETED; no CURRENT step; `failed: true` |
| CANCELLED | INITIATED or ESCROWED | Completed steps stay COMPLETED; no CURRENT step; `failed: true` |
| REFUND_FAILED | ESCROWED | Completed steps stay COMPLETED; no CURRENT step; `failed: true` |

**Why REFUND_FAILED is not in the failure-step group:** Refund is not a happy-path step (INITIATED → ESCROWED → CLAIMED → DELIVERED). If we mapped REFUND_FAILED → ESCROWED, the ESCROWED step — which *succeeded* and has a timestamp — would be marked FAILED, telling the user "securing funds failed" when actually the refund failed. Since there's no timeline step for "refund", we treat it like REFUNDED: all completed steps stay COMPLETED, `failed: true`.

**Rationale for collapsing to `failed: true`:** Distinguishing refund/cancel from failure would require additional response fields (`refunded`, `cancelled`) and frontend branching. For a hackathon 4-step tracker, `failed: true` is sufficient — the sender sees "this remittance stopped" and can contact support. A future iteration could add a `terminalReason` enum (FAILED, REFUNDED, CANCELLED) if the mobile app needs richer messaging.

## 6. API Contract

### GET /api/remittances/{remittanceId}/timeline

**Auth:** Bearer JWT (ownership check — remittance must belong to authenticated user)

**Response (200 OK) — in progress (status: ESCROWED):**

```json
{
  "steps": [
    {
      "step": "INITIATED",
      "status": "COMPLETED",
      "message": "Payment received",
      "completedAt": "2026-04-22T06:53:15.632Z"
    },
    {
      "step": "ESCROWED",
      "status": "COMPLETED",
      "message": "Funds secured on-chain",
      "completedAt": "2026-04-22T06:53:20.506Z"
    },
    {
      "step": "CLAIMED",
      "status": "CURRENT",
      "message": "SMS sent, waiting for recipient",
      "completedAt": null
    },
    {
      "step": "DELIVERED",
      "status": "PENDING",
      "message": "INR deposited to recipient's bank",
      "completedAt": null
    }
  ],
  "failed": false
}
```

**Response (200 OK) — in progress (status: CLAIMED):**

```json
{
  "steps": [
    {
      "step": "INITIATED",
      "status": "COMPLETED",
      "message": "Payment received",
      "completedAt": "2026-04-22T06:53:15.632Z"
    },
    {
      "step": "ESCROWED",
      "status": "COMPLETED",
      "message": "Funds secured on-chain",
      "completedAt": "2026-04-22T06:53:20.506Z"
    },
    {
      "step": "CLAIMED",
      "status": "COMPLETED",
      "message": "Recipient claimed",
      "completedAt": "2026-04-22T07:15:42.100Z"
    },
    {
      "step": "DELIVERED",
      "status": "CURRENT",
      "message": "Depositing INR to recipient's bank...",
      "completedAt": null
    }
  ],
  "failed": false
}
```

**Response (200 OK) — failed remittance (CLAIM_FAILED from ESCROWED):**

```json
{
  "steps": [
    {
      "step": "INITIATED",
      "status": "COMPLETED",
      "message": "Payment received",
      "completedAt": "2026-04-22T06:53:15.632Z"
    },
    {
      "step": "ESCROWED",
      "status": "COMPLETED",
      "message": "Funds secured on-chain",
      "completedAt": "2026-04-22T06:53:20.506Z"
    },
    {
      "step": "CLAIMED",
      "status": "FAILED",
      "message": "Recipient claimed",
      "completedAt": null
    },
    {
      "step": "DELIVERED",
      "status": "PENDING",
      "message": "INR deposited to recipient's bank",
      "completedAt": null
    }
  ],
  "failed": true
}
```

**Response (200 OK) — refunded/cancelled remittance:**

```json
{
  "steps": [
    {
      "step": "INITIATED",
      "status": "COMPLETED",
      "message": "Payment received",
      "completedAt": "2026-04-22T06:53:15.632Z"
    },
    {
      "step": "ESCROWED",
      "status": "COMPLETED",
      "message": "Funds secured on-chain",
      "completedAt": "2026-04-22T06:53:20.506Z"
    },
    {
      "step": "CLAIMED",
      "status": "PENDING",
      "message": "Recipient claimed",
      "completedAt": null
    },
    {
      "step": "DELIVERED",
      "status": "PENDING",
      "message": "INR deposited to recipient's bank",
      "completedAt": null
    }
  ],
  "failed": true
}
```

**Error responses:**

| Condition | Error Code | HTTP |
|---|---|---|
| Remittance not found or belongs to another user | SP-0010 | 404 |
| Authentication required | SP-0040 | 401 |

## 7. Package Structure

Following hexagonal architecture conventions:

```
com.stablepay/
  application/
    controller/remittance/
      RemittanceController.java          # Add GET /{remittanceId}/timeline
      mapper/
        RemittanceTimelineMapper.java    # New — maps RemittanceTimeline → RemittanceTimelineResponse
    dto/
      RemittanceTimelineResponse.java    # New — top-level response with steps + failed
      TimelineStep.java                  # New — individual step record

  domain/
    remittance/
      model/
        RemittanceStatusEvent.java       # New — persisted domain record
        TimelineStepStatus.java          # New — enum: COMPLETED, CURRENT, PENDING, FAILED
        RemittanceTimeline.java          # New — handler return type (not persisted)
        RemittanceTimelineStep.java      # New — single step in timeline
      handler/
        UpdateRemittanceStatusHandler.java  # Modified — insert event on transition
        CreateRemittanceHandler.java        # Modified — insert INITIATED event
        GetRemittanceTimelineHandler.java   # New — build timeline from events
      port/
        RemittanceStatusEventRepository.java  # New — port interface

  infrastructure/
    db/remittance/
      RemittanceStatusEventEntity.java       # New — JPA entity
      RemittanceStatusEventJpaRepository.java # New — Spring Data repo
      RemittanceStatusEventEntityMapper.java  # New — MapStruct mapper
      RemittanceStatusEventRepositoryAdapter.java  # New — port adapter
```

## 8. Domain Model

### RemittanceStatusEvent (persisted)

```java
@Builder(toBuilder = true)
public record RemittanceStatusEvent(
    Long id,
    UUID remittanceId,
    RemittanceStatus status,
    String message,
    Instant createdAt
) {}
```

### TimelineStepStatus

```java
public enum TimelineStepStatus {
    COMPLETED,
    CURRENT,
    PENDING,
    FAILED
}
```

### RemittanceTimeline (handler return type — not persisted)

Domain-level model returned by `GetRemittanceTimelineHandler`. The controller mapper converts this to the `RemittanceTimelineResponse` DTO. This follows the existing pattern (`FundingInitiationResult` → `FundingOrderResponse`).

```java
@Builder
public record RemittanceTimeline(
    List<RemittanceTimelineStep> steps,
    boolean failed
) {}
```

### RemittanceTimelineStep

```java
@Builder
public record RemittanceTimelineStep(
    RemittanceStatus step,
    TimelineStepStatus status,
    String message,
    Instant completedAt
) {}
```

Both live in `domain/remittance/model/`.

## 9. Handler Logic

### GetRemittanceTimelineHandler

```
Input: remittanceId (UUID), principalId (UUID)
Output: RemittanceTimeline (domain model — controller mapper converts to DTO)

1. Call getRemittanceQueryHandler.handle(remittanceId, principalId)
   → validates existence + ownership, returns Remittance

2. Call remittanceStatusEventRepository.findByRemittanceId(remittanceId)
   → returns List<RemittanceStatusEvent> ordered by createdAt ASC, id ASC

3. Determine failure/terminal state:
   - FAILURE_STATES = {DEPOSIT_FAILED, CLAIM_FAILED, DISBURSEMENT_FAILED}
   - TERMINAL_NON_DELIVERY = {REFUNDED, CANCELLED, REFUND_FAILED}
   - isFailed = FAILURE_STATES.contains(remittance.status())
                 || TERMINAL_NON_DELIVERY.contains(remittance.status())

4. If remittance.status() is in FAILURE_STATES, determine which happy-path
   step failed:
   - FAILURE_TO_FAILED_STEP mapping:
     - DEPOSIT_FAILED       → ESCROWED
     - CLAIM_FAILED         → CLAIMED
     - DISBURSEMENT_FAILED  → DELIVERED
   - For TERMINAL_NON_DELIVERY (REFUNDED, CANCELLED, REFUND_FAILED):
     no step is marked FAILED — all completed steps stay COMPLETED,
     remaining steps are PENDING

5. Build 4-step timeline:
   - Define HAPPY_PATH_STEPS = [INITIATED, ESCROWED, CLAIMED, DELIVERED]
   - Convert events to a Map<RemittanceStatus, RemittanceStatusEvent> for lookup
     (filter to happy-path statuses only; on duplicate keys, keep first:
      Collectors.toMap(..., (first, second) -> first))
   - Track whether a CURRENT step has been assigned (boolean foundCurrent = false)
   - For each step in HAPPY_PATH_STEPS:
     a. If an event exists for this status → COMPLETED with event.createdAt
     b. Else if NOT isFailed AND NOT foundCurrent
        → CURRENT with completedAt: null; set foundCurrent = true
     c. Else if isFailed AND this step is the failed step (from step 4)
        → FAILED with completedAt: null
     d. Otherwise → PENDING with completedAt: null

6. Determine message for each step:
   - COMPLETED → use completed message (e.g., "Payment received")
   - CURRENT → use in-progress message (e.g., "Securing funds on-chain...")
   - PENDING → use completed message (describes what the step will achieve)
   - FAILED → use completed message (describes what the step was trying to do)
   - Special case: CLAIMED/CURRENT checks remittance.smsNotificationFailed()
     - false → "SMS sent, waiting for recipient"
     - true → "Claim link available, waiting for recipient"

7. Return RemittanceTimeline(steps, failed=isFailed)
```

### UpdateRemittanceStatusHandler (modified)

```
Existing logic unchanged. After remittanceRepository.save(updated):

1. Determine message based on target status:
   - ESCROWED → "Funds secured on-chain"
   - CLAIMED → "Recipient claimed"
   - DELIVERED → "INR deposited to recipient's bank"
   - REFUNDED → "Refunded to sender"
   - CANCELLED → "Remittance cancelled"
   - DEPOSIT_FAILED → "Processing failed"
   - CLAIM_FAILED → "Processing failed"
   - DISBURSEMENT_FAILED → "Processing failed"
   - REFUND_FAILED → "Processing failed"

2. Insert RemittanceStatusEvent(remittanceId, targetStatus, message, now())
```

### CreateRemittanceHandler (modified)

```
After the FIRST remittanceRepository.save(remittance) (line 62, the creation save):

1. Insert RemittanceStatusEvent(saved.remittanceId(), INITIATED, "Payment received", now())

Note: The second save on line 72 only updates claimTokenId — do NOT insert a second event.
```

## 10. Frontend Integration Guide

### Polling Strategy

```
Poll GET /api/remittances/{id}/timeline every 3-5 seconds while any step is CURRENT.
Stop polling when:
  - All steps are COMPLETED (terminal: DELIVERED)
  - failed == true
  - CLAIMED step is CURRENT (long wait — switch to 30s polling or manual refresh)
```

### Rendering

```
COMPLETED → green dot + timestamp
CURRENT   → purple animated dot + "in progress" indicator
PENDING   → grey dot
FAILED    → red dot
```

The response is a fixed 4-element array, always in order. The frontend is a stateless renderer — no business logic needed on the client.

## 11. Testing Plan

### Unit Tests

| Test | What it verifies |
|---|---|
| `GetRemittanceTimelineHandlerTest` | Builds correct 4-step timeline for each happy-path status (INITIATED, ESCROWED, CLAIMED, DELIVERED) |
| `GetRemittanceTimelineHandlerTest` | Sets `failed: true` and marks correct step FAILED for DEPOSIT_FAILED |
| `GetRemittanceTimelineHandlerTest` | Sets `failed: true` and marks correct step FAILED for CLAIM_FAILED |
| `GetRemittanceTimelineHandlerTest` | Sets `failed: true` and marks correct step FAILED for DISBURSEMENT_FAILED |
| `GetRemittanceTimelineHandlerTest` | Sets `failed: true` with no FAILED step for REFUNDED |
| `GetRemittanceTimelineHandlerTest` | Sets `failed: true` with no FAILED step for CANCELLED |
| `GetRemittanceTimelineHandlerTest` | Sets `failed: true` with no FAILED step for REFUND_FAILED (ESCROWED stays COMPLETED) |
| `GetRemittanceTimelineHandlerTest` | SMS-aware message when CLAIMED step is CURRENT (smsNotificationFailed=false) |
| `GetRemittanceTimelineHandlerTest` | SMS-aware message when CLAIMED step is CURRENT (smsNotificationFailed=true) |
| `GetRemittanceTimelineHandlerTest` | DELIVERED step shows CURRENT with "Depositing INR..." when status is CLAIMED |
| `UpdateRemittanceStatusHandlerTest` | Inserts event on status transition |
| `CreateRemittanceHandlerTest` | Inserts INITIATED event on creation |
| `RemittanceTimelineMapperTest` | Maps domain timeline to response DTOs |
| `RemittanceControllerTest` | GET timeline returns 200 with correct structure |
| `RemittanceControllerTest` | GET timeline returns 401 without JWT |
| `RemittanceControllerTest` | GET timeline returns 404 for another user's remittance |

### Integration Tests

| Test | What it verifies |
|---|---|
| `RemittanceStatusEventRepositoryIntegrationTest` | Save + findByRemittanceId ordered by createdAt ASC, id ASC |
| `RemittanceLifecycleE2EIntegrationTest` | Timeline populates correctly through full lifecycle |

## 12. Migration Checklist

- [ ] Flyway V9 migration creates `remittance_status_events` table
- [ ] Domain model: `RemittanceStatusEvent` record (persisted)
- [ ] Domain model: `TimelineStepStatus` enum
- [ ] Domain model: `RemittanceTimeline` + `RemittanceTimelineStep` records (handler return type)
- [ ] Port: `RemittanceStatusEventRepository` interface
- [ ] Infrastructure: entity, JPA repo, mapper, adapter
- [ ] `CreateRemittanceHandler` logs INITIATED event (after first save, line 62)
- [ ] `UpdateRemittanceStatusHandler` logs event on every transition (including CANCELLED)
- [ ] `GetRemittanceTimelineHandler` builds timeline from events
- [ ] DTOs: `RemittanceTimelineResponse`, `TimelineStep`
- [ ] Mapper: `RemittanceTimelineMapper`
- [ ] Controller: `GET /api/remittances/{remittanceId}/timeline`
- [ ] Unit tests for handler (all 6 non-happy-path statuses), mapper, controller
- [ ] Integration test for event repository
- [ ] `./gradlew build` passes
- [ ] Spotless formatting applied
