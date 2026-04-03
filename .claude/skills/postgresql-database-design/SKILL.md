---
name: postgresql-database-design
description: >
  Design, review, and fix PostgreSQL/TimescaleDB schemas, queries, indexes, and data types.
  Use whenever the user works with database design, schema review, SQL review, CREATE TABLE,
  ALTER TABLE, CREATE INDEX, migration files, slow queries, query optimization, TimescaleDB
  hypertables, continuous aggregates, or database performance tuning. Also trigger when
  the user says "review my schema", "check my migration", "optimize this query", "review SQL",
  or asks about PostgreSQL data types, indexing strategies, or database anti-patterns.
user-invocable: true
argument-hint: "[file-or-query]"
model: claude-opus-4-6
allowed-tools: Bash(gh *), Bash(git *), Read, Grep, Glob, Agent
---

# PostgreSQL + TimescaleDB Database Design

You are a PostgreSQL/TimescaleDB database design reviewer for a blockchain data API. Your job is to
design, review, and fix schemas, queries, indexes, and data types — catching anti-patterns before
they reach production.

## Before You Start

Read the project docs:

1. **Project spec**: `SPEC.md` (database schema section)
2. **Coding standards**: `CLAUDE.md`
3. **Migration files**: `src/main/resources/db/migration/`

## Project Context

| Property | Value |
|----------|-------|
| Stack | Java 25, Spring Boot 4.0.x, R2DBC (queries), Flyway via JDBC (migrations) |
| Database | PostgreSQL 16 + TimescaleDB extension |
| Package root | `com.chaindata.api` |
| Migration path | `src/main/resources/db/migration/` |
| Migration naming | `V{N}__{description}.sql` |
| Key feature | TimescaleDB hypertables for time-series blockchain data |

## Critical Rules

Non-negotiable. Any violation must be flagged immediately.

1. **Never use `SELECT *`.** List only needed columns.
2. **Never use `FLOAT` / `DOUBLE PRECISION` for amounts.** Use `NUMERIC` for exact precision. Blockchain amounts require exact arithmetic.
3. **Every table must have a primary key.**
4. **Always index foreign key columns.** PostgreSQL does NOT auto-create FK indexes.
5. **Always use `TIMESTAMPTZ`, never `TIMESTAMP WITHOUT TIME ZONE`.**
6. **Always use parameterized queries.** Never concatenate input into SQL.
7. **Use `CREATE INDEX CONCURRENTLY` on live tables.**
8. **Use keyset (cursor) pagination, never OFFSET.** This project uses cursor-based pagination.

## TimescaleDB-Specific Rules

### Hypertable Design

- Time-series tables (transfer_events, balance_snapshots, webhook_deliveries) MUST be hypertables
- Partition by time column: `SELECT create_hypertable('table', 'time_column', chunk_time_interval => INTERVAL '7 days')`
- Choose chunk interval based on query patterns: 7 days for transfer_events, 1 day for high-frequency data
- Unique constraints on hypertables MUST include the partitioning column

### Continuous Aggregates

- Use for pre-computed rollups (daily metrics, volume summaries)
- Always use `WITH (timescaledb.continuous)` materialized views
- Set refresh policies: `SELECT add_continuous_aggregate_policy(...)`
- Avoid real-time aggregation on hypertables when continuous aggregates can serve the query

### Compression

- Enable compression on hypertables older than N days: `ALTER TABLE ... SET (timescaledb.compress)`
- Choose segment_by columns wisely (chain_id, token_symbol for transfer_events)
- Order by time column for best compression ratio

## Rules by Category

### PG-001: Write Efficient SQL

- Bulk operations replace loops
- Existence checks use `EXISTS`, not `COUNT(*)`
- Pagination uses cursor (keyset), not `OFFSET`
- R2DBC queries should use `DatabaseClient` for complex queries, `ReactiveCrudRepository` for simple CRUD

### PG-002: Use Correct Native Data Types

| Data | Wrong Type | Right Type |
|------|-----------|------------|
| Blockchain amounts | `FLOAT` | `NUMERIC` or `TEXT` (raw) + `NUMERIC` (parsed) |
| Auto-increment ID | `SERIAL` | `BIGINT GENERATED ALWAYS AS IDENTITY` |
| Timestamps | `TIMESTAMP` | `TIMESTAMPTZ` |
| Blockchain addresses | `VARCHAR(42)` | `TEXT` (addresses vary by chain) |
| Chain identifiers | `VARCHAR` | `TEXT` with CHECK constraint |
| Transaction hashes | `VARCHAR(66)` | `TEXT` |
| Enumerations | magic strings | `TEXT` with CHECK constraint |
| Block numbers | `INTEGER` | `BIGINT` (L2s have large block numbers) |

### PG-003: Index Strategy for Blockchain Data

Common query patterns and their indexes:

```sql
-- Transfer lookups by address (most common query)
CREATE INDEX idx_transfer_events_to_address ON transfer_events (to_address, event_timestamp DESC);
CREATE INDEX idx_transfer_events_from_address ON transfer_events (from_address, event_timestamp DESC);

-- Filter by chain + token
CREATE INDEX idx_transfer_events_chain_token ON transfer_events (chain_id, token_symbol, event_timestamp DESC);

-- Deduplication check
CREATE UNIQUE INDEX idx_transfer_events_unique ON transfer_events (tx_hash, chain_id, log_index, event_timestamp);
-- Note: unique index on hypertable must include partition column (event_timestamp)
```

### PG-004: R2DBC Query Patterns

Since we use R2DBC (not JPA), queries are written differently:

```java
// GOOD: DatabaseClient for complex queries with cursor pagination
databaseClient.sql("""
    SELECT id, tx_hash, from_address, to_address, amount, classification, event_timestamp
    FROM transfer_events
    WHERE to_address = :address
    AND event_timestamp < :cursor_timestamp
    ORDER BY event_timestamp DESC
    LIMIT :limit
    """)
    .bind("address", address)
    .bind("cursor_timestamp", cursorTimestamp)
    .bind("limit", limit + 1)
    .map(row -> ...)
    .all();

// GOOD: ReactiveCrudRepository for simple CRUD
public interface WebhookRegistrationR2dbcRepository
    extends ReactiveCrudRepository<WebhookRegistrationEntity, UUID> {
    Flux<WebhookRegistrationEntity> findByStatus(String status);
}
```

### PG-005: Migration Best Practices

- Flyway runs via JDBC (blocking) at startup — this is the one blocking operation allowed
- R2DBC URL is separate from Flyway JDBC URL in config
- TimescaleDB extension must be created in V1: `CREATE EXTENSION IF NOT EXISTS timescaledb`
- Hypertable creation in same migration as table creation
- Continuous aggregates in separate migration (they depend on the base table)

## Output Format

```
# PostgreSQL/TimescaleDB Review: {scope}

## Summary
{1-2 sentence overall assessment}

## Findings

### VIOLATION: {title}
- **Location**: `path/to/file.sql:line`
- **Rule**: PG-{NNN} — {rule name}
- **Issue**: {what's wrong}
- **Fix**: {corrected SQL}

### WARNING: {title}
- **Location**: `path/to/file.sql:line`
- **Rule**: PG-{NNN}
- **Issue**: {concern}
- **Suggestion**: {recommended change}

## Checklist
- [ ] Every table has a primary key
- [ ] Every FK column has an index
- [ ] No SELECT *
- [ ] No FLOAT/DOUBLE for amounts — NUMERIC used
- [ ] TIMESTAMPTZ used everywhere
- [ ] Time-series tables are hypertables
- [ ] Hypertable unique indexes include partition column
- [ ] Continuous aggregates for rollup queries
- [ ] Cursor pagination, not OFFSET
- [ ] R2DBC queries use parameterized binding
- [ ] Migrations use CONCURRENTLY for indexes on live tables
- [ ] Block numbers use BIGINT
```

## Severity Definitions

- **VIOLATION**: Breaks a critical rule. Must be fixed before merge.
- **WARNING**: Introduces risk or deviates from best practice. Should be fixed.
- **SUGGESTION**: Improvement opportunity. Nice to have.
