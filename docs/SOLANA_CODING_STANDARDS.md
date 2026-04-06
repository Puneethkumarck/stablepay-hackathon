# Solana Program Coding Standards for StablePay

> Mandatory coding and testing rules for the StablePay Anchor program (Rust).
> Coding agents must follow these rules exactly.

## 1. Project Structure

### Modular File Layout (MANDATORY)

```
programs/stablepay-escrow/
  Cargo.toml
  src/
    lib.rs                    # Entry point: declare_id!, mod declarations, #[program] dispatch
    instructions/             # One file per instruction + its Accounts context
      mod.rs
      deposit.rs
      claim.rs
      refund.rs
      cancel.rs
    state/                    # Account structs (#[account] types)
      mod.rs
      escrow.rs
    errors.rs                 # Single #[error_code] enum
    constants.rs              # Seeds, limits, numeric constants
```

**Rules:**

- **`lib.rs` is a thin dispatcher.** It declares modules, calls `declare_id!`, and in `#[program]` delegates to instruction handlers. No business logic lives here.
- **One instruction, one file.** Each file in `instructions/` contains the handler function AND its `#[derive(Accounts)]` context struct.
- **State is separate.** Account structs live in `state/`, reusable across instructions.
- **Errors are centralized.** Single `errors.rs` with one `#[error_code]` enum.
- **Constants are explicit.** PDA seed prefixes, numerical limits, and magic values go in `constants.rs`.

### `lib.rs` Pattern

```rust
use anchor_lang::prelude::*;

declare_id!("YourProgramId11111111111111111111111111111111");

pub mod constants;
pub mod errors;
pub mod instructions;
pub mod state;

use instructions::*;

#[program]
pub mod stablepay_escrow {
    use super::*;

    pub fn deposit(ctx: Context<Deposit>, amount: u64, deadline: i64) -> Result<()> {
        instructions::deposit::handler(ctx, amount, deadline)
    }

    pub fn claim(ctx: Context<Claim>) -> Result<()> {
        instructions::claim::handler(ctx)
    }

    pub fn refund(ctx: Context<Refund>) -> Result<()> {
        instructions::refund::handler(ctx)
    }
}
```

## 2. Security Rules (MANDATORY)

### 2.1 Account Validation

| Rule | Implementation |
|---|---|
| Never use raw `AccountInfo` for program-owned accounts | Always use `Account<'info, T>` |
| Verify signers | Use `Signer<'info>` for authority accounts |
| Verify PDAs | Always use `seeds` + `bump` constraints |
| Verify programs | Use `Program<'info, T>` for CPI targets |
| Verify ownership | `Account<'info, T>` checks this automatically |

```rust
// FORBIDDEN: raw AccountInfo for program data
pub escrow: AccountInfo<'info>,

// REQUIRED: typed Account wrapper
pub escrow: Account<'info, Escrow>,
```

### 2.2 PDA Safety — Store and Reuse Canonical Bump

```rust
// State: always store the bump
#[account]
#[derive(InitSpace)]
pub struct Escrow {
    pub bump: u8,
    // ...
}

// Init: save from ctx.bumps
escrow.bump = ctx.bumps.escrow;

// Subsequent instructions: reference stored bump
#[account(
    seeds = [ESCROW_SEED, escrow.sender.as_ref()],
    bump = escrow.bump,
)]
pub escrow: Account<'info, Escrow>,
```

### 2.3 Re-initialization Prevention

- **Use `init`, not `init_if_needed`** unless absolutely necessary.
- If `init_if_needed` is required, add explicit guards:

```rust
require!(escrow.amount == 0, EscrowError::AlreadyInitialized);
```

### 2.4 Integer Safety (MANDATORY)

**Never use raw arithmetic operators on token amounts or balances.**

```rust
// FORBIDDEN
let fee = amount * fee_bps / 10_000;

// REQUIRED: checked arithmetic
let fee = amount
    .checked_mul(fee_bps as u64)
    .ok_or(EscrowError::Overflow)?
    .checked_div(10_000)
    .ok_or(EscrowError::Overflow)?;
```

**Also required in `Cargo.toml`:**

```toml
[profile.release]
overflow-checks = true
```

**Casting rules:**
- Never use `as` for narrowing casts (`u64 as u32`)
- Use `try_from()` with error handling

### 2.5 Clock Usage

```rust
// REQUIRED: get clock without sysvar account
let clock = Clock::get()?;
require!(clock.unix_timestamp <= escrow.deadline, EscrowError::EscrowExpired);
```

- Use `Clock::get()?` instead of passing `Sysvar<'info, Clock>` as an account.
- Use `unix_timestamp` (not `slot`) for deadline logic.

### 2.6 Close Account — Rent Reclaim

Use Anchor's `close` constraint. Never close accounts manually.

```rust
#[account(
    mut,
    close = sender,  // Transfers lamports, zeros data, resets owner
    has_one = sender,
    seeds = [ESCROW_SEED, sender.key().as_ref()],
    bump = escrow.bump,
)]
pub escrow: Account<'info, Escrow>,
```

### 2.7 CPI Safety

- Always use `Program<'info, T>` to verify CPI targets.
- Call `.reload()?` on accounts after CPI if you read them again:

```rust
token::transfer(cpi_ctx, amount)?;
ctx.accounts.vault.reload()?;
```

### 2.8 Custom Errors on Constraints (MANDATORY)

Always attach custom errors to constraints for debuggability:

```rust
#[account(
    mut,
    has_one = sender @ EscrowError::UnauthorizedSender,
    constraint = escrow.status == EscrowStatus::Active @ EscrowError::EscrowNotActive,
)]
pub escrow: Account<'info, Escrow>,
```

## 3. Error Handling

### Single Error Enum

```rust
use anchor_lang::prelude::*;

#[error_code]
pub enum EscrowError {
    #[msg("Escrow amount must be greater than zero")]
    AmountTooSmall,

    #[msg("Escrow has already been initialized")]
    AlreadyInitialized,

    #[msg("Unauthorized: caller is not the escrow sender")]
    UnauthorizedSender,

    #[msg("Unauthorized: caller is not the claim authority")]
    UnauthorizedClaimAuthority,

    #[msg("Arithmetic overflow")]
    Overflow,

    #[msg("Escrow is not in the expected status")]
    InvalidEscrowStatus,

    #[msg("Escrow has expired")]
    EscrowExpired,

    #[msg("Escrow has not yet expired")]
    EscrowNotExpired,
}
```

### Validation Macros

| Macro | Use When |
|---|---|
| `require!(expr, Error)` | General boolean checks |
| `require_keys_eq!(a, b, Error)` | Comparing two `Pubkey` values |
| `require_keys_neq!(a, b, Error)` | Ensuring two pubkeys differ |
| `require_eq!(a, b, Error)` | Comparing non-pubkey values |
| `require_gt!(a, b, Error)` | Greater-than checks |
| `err!(Error)` | Returning an error directly |

**Use the most specific macro.** `require_keys_eq!` over `require!(a == b)` for pubkeys.

### Forbidden Patterns

```rust
// FORBIDDEN: unwrap/panic in production code
let value = some_option.unwrap();

// REQUIRED: propagate errors
let value = some_option.ok_or(EscrowError::InvalidState)?;
```

**Exception:** `unwrap()` and `expect()` are acceptable in `#[cfg(test)]` code for known-safe operations (e.g., hardcoded values that cannot fail). In production code, always propagate errors.

## 4. State Management

### Account Struct Design

Use `#[derive(InitSpace)]` for automatic space calculation:

```rust
#[account]
#[derive(InitSpace)]
pub struct Escrow {
    pub sender: Pubkey,          // 32
    pub recipient: Pubkey,       // 32
    pub claim_authority: Pubkey, // 32
    pub mint: Pubkey,            // 32
    pub amount: u64,             // 8
    pub deadline: i64,           // 8
    pub status: EscrowStatus,    // 1
    pub bump: u8,                // 1
}
```

### Space Calculation

Always `8 + T::INIT_SPACE` (8 bytes for discriminator):

```rust
#[account(
    init,
    payer = sender,
    space = 8 + Escrow::INIT_SPACE,
    seeds = [ESCROW_SEED, remittance_id.as_ref()],
    bump,
)]
pub escrow: Account<'info, Escrow>,
```

### Common Type Sizes

| Type | Bytes |
|---|---|
| `bool` | 1 |
| `u8` / `i8` | 1 |
| `u16` / `i16` | 2 |
| `u32` / `i32` | 4 |
| `u64` / `i64` | 8 |
| `u128` / `i128` | 16 |
| `Pubkey` | 32 |
| `String` | 4 + max_len |
| `Vec<T>` | 4 + (element_size * max_count) |
| `Option<T>` | 1 + T size |
| Enum (C-like) | 1 |

### Enum Representation

```rust
#[derive(AnchorSerialize, AnchorDeserialize, Clone, Copy, PartialEq, Eq, InitSpace)]
pub enum EscrowStatus {
    Active,
    Claimed,
    Refunded,
    Cancelled,
}
```

## 5. Code Style

### Naming Conventions

| Item | Convention | Example |
|---|---|---|
| Functions, variables | `snake_case` | `deposit_escrow`, `total_amount` |
| Structs, enums, traits | `PascalCase` | `Escrow`, `EscrowStatus`, `EscrowError` |
| Constants | `SCREAMING_SNAKE_CASE` | `ESCROW_SEED`, `MAX_AMOUNT` |
| Modules, files | `snake_case` | `instructions`, `deposit.rs` |
| Context structs | `PascalCase` (verb) | `Deposit`, `Claim`, `Refund` |

### Constants for Seeds and Magic Values (MANDATORY)

```rust
// constants.rs
pub const ESCROW_SEED: &[u8] = b"escrow";
pub const VAULT_SEED: &[u8] = b"vault";
pub const USDC_DECIMALS: u8 = 6;
pub const ESCROW_EXPIRY_SECONDS: i64 = 172_800; // 48 hours
```

**Never use string/byte literals directly in seeds or constraints.** Always reference constants.

### Import Organization

```rust
// 1. External crates
use anchor_lang::prelude::*;
use anchor_spl::token::{self, Token, TokenAccount, Transfer};

// 2. Local modules
use crate::constants::*;
use crate::errors::EscrowError;
use crate::state::Escrow;
```

- No wildcard imports from external crates except `anchor_lang::prelude::*`.
- Wildcard imports from `crate::constants::*` are allowed.

### Documentation

```rust
/// Escrow account holding USDC for a cross-border remittance.
///
/// Created by deposit instruction. Released to recipient on claim,
/// or refunded to sender after expiry.
#[account]
#[derive(InitSpace)]
pub struct Escrow {
    /// The sender who deposited funds
    pub sender: Pubkey,
    // ...
}
```

- Document every public struct and its fields.
- Document instruction handlers with a brief description of what they do.
- Use `///` doc comments, not `//`.

### Clippy and Formatting (MANDATORY)

Run before every commit:

```bash
cargo fmt -- --check
cargo clippy -- -D warnings
```

**Required in `Cargo.toml`:**

```toml
[lints.clippy]
cast_possible_truncation = "deny"
cast_sign_loss = "deny"
```

## 6. Anchor Constraints Reference

| Constraint | Purpose | When to Use |
|---|---|---|
| `init` | Create + initialize account | First-time setup; fails if exists |
| `mut` | Mark as mutable | Any account whose data or lamports change |
| `seeds` + `bump` | PDA derivation | Every PDA account |
| `has_one = field` | Data matching | Verify stored pubkey matches provided account |
| `close = dest` | Account closure | Reclaiming rent; zeros data automatically |
| `constraint = expr` | Custom validation | Business rules |
| `address = expr` | Pubkey check | Well-known addresses |
| `token::mint = expr` | Token account mint | Verify token account matches expected mint |
| `token::authority = expr` | Token authority | Verify token account authority |

### Token Account Patterns

```rust
// Vault: PDA-owned token account
#[account(
    init,
    payer = sender,
    token::mint = usdc_mint,
    token::authority = escrow,
    seeds = [VAULT_SEED, escrow.key().as_ref()],
    bump,
)]
pub vault: Account<'info, TokenAccount>,

// Sender's token account
#[account(
    mut,
    constraint = sender_token.owner == sender.key() @ EscrowError::InvalidTokenOwner,
    constraint = sender_token.mint == usdc_mint.key() @ EscrowError::InvalidMint,
)]
pub sender_token: Account<'info, TokenAccount>,
```

## 7. Testing Standards

### Framework

| Layer | Tool | Purpose |
|---|---|---|
| E2E | Anchor Mocha/Chai (TypeScript) | Full transaction testing on localnet |
| Unit | Rust `#[cfg(test)]` | Pure logic unit tests (math, validation) |

### Test Organization

```
tests/
  stablepay-escrow.ts         # Main test file
```

### Test Isolation

Each test must be independent. Use unique PDA seeds per test to avoid state leakage:

```typescript
// Generate unique remittance ID per test to derive unique PDAs
const remittanceId = anchor.web3.Keypair.generate().publicKey;
const [escrowPDA] = anchor.web3.PublicKey.findProgramAddressSync(
    [Buffer.from("escrow"), remittanceId.toBuffer()],
    program.programId
);
```

Never rely on shared mutable state between tests. Each test creates its own accounts.

### Test Structure: Arrange / Act / Assert

```typescript
describe("stablepay-escrow", () => {
    // Shared setup
    const provider = anchor.AnchorProvider.env();
    anchor.setProvider(provider);
    const program = anchor.workspace.StablepayEscrow as Program<StablepayEscrow>;

    describe("deposit", () => {
        it("should create escrow with correct state", async () => {
            // Arrange
            const amount = new anchor.BN(1_000_000);

            // Act
            await program.methods
                .deposit(amount, deadline)
                .accountsStrict({ /* ... */ })
                .signers([sender])
                .rpc();

            // Assert
            const escrow = await program.account.escrow.fetch(escrowPDA);
            expect(escrow.sender.toBase58()).to.equal(sender.publicKey.toBase58());
            expect(escrow.amount.toNumber()).to.equal(1_000_000);
            expect(escrow.status).to.deep.equal({ active: {} });
        });
    });

    describe("error cases", () => {
        it("should reject zero amount", async () => {
            try {
                await program.methods
                    .deposit(new anchor.BN(0), deadline)
                    .accountsStrict({ /* ... */ })
                    .signers([sender])
                    .rpc();
                expect.fail("Expected error was not thrown");
            } catch (err) {
                expect(err.error.errorCode.code).to.equal("AmountTooSmall");
            }
        });
    });
});
```

### Test Naming

- Use descriptive `it("should ...")` strings.
- Group by instruction with `describe("instruction_name", ...)`.
- Separate happy path from error cases.

### Required Test Coverage

Every instruction must test:

1. **Happy path** — correct state changes, correct account data
2. **Authorization failures** — wrong signer, wrong authority
3. **State validation** — wrong status, expired deadline
4. **Amount validation** — zero, overflow
5. **PDA derivation** — correct seeds produce correct address
6. **Account closure** — rent returned, data zeroed (for terminal instructions)

### Rust Unit Tests (for pure logic)

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn should_calculate_fee_correctly() {
        let amount: u64 = 1_000_000;
        let fee_bps: u16 = 50;
        // unwrap() is acceptable in tests for known-safe hardcoded values
        let fee = amount
            .checked_mul(fee_bps as u64)
            .unwrap()
            .checked_div(10_000)
            .unwrap();
        assert_eq!(fee, 5_000);
    }
}
```

## 8. Performance Guidelines

| Resource | Limit |
|---|---|
| Default CU budget | 200,000 per instruction |
| Max CU (with request) | 1,400,000 per transaction |
| Stack | 4 KB |
| Heap | 32 KB |
| Max account size (init) | 10,240 bytes |

- **Store and reuse PDA bumps** — saves ~20 CU per derivation.
- **Use `Clock::get()?`** — saves one account slot vs sysvar.
- **Box large accounts** in context structs: `pub escrow: Box<Account<'info, Escrow>>`.
- **Prefer smallest type** that fits: `u8` for enums, `u16` for basis points.

## 9. Anti-Patterns (FORBIDDEN)

| Anti-Pattern | Correct Approach |
|---|---|
| Raw `AccountInfo` for program data | `Account<'info, T>` |
| `unwrap()` / `panic!()` in production | Propagate with `?` or `err!()` |
| `init_if_needed` without guards | Use `init` |
| Raw arithmetic on token amounts | `checked_*` methods |
| `as` for narrowing casts | `try_from()` |
| Arbitrary program IDs for CPI | `Program<'info, T>` |
| String literals in seeds | Constants from `constants.rs` |
| Business logic in `lib.rs` | Delegate to `instructions/*.rs` handlers |
| Missing discriminator in space | Always `8 + T::INIT_SPACE` |
| Missing `reload()` after CPI | Call `.reload()?` on affected accounts |
| Constraints without custom errors | Always `@ EscrowError::Variant` |
| Single monolithic `lib.rs` | Modular file layout per instruction |

## 10. Build & Deploy Commands

```bash
# Build
anchor build

# Test (localnet)
anchor test

# Format
cargo fmt --manifest-path programs/stablepay-escrow/Cargo.toml
cargo clippy --manifest-path programs/stablepay-escrow/Cargo.toml -- -D warnings

# Deploy to devnet
anchor deploy --provider.cluster devnet

# Get program ID
solana address -k target/deploy/stablepay_escrow-keypair.json
```

## 11. Cargo.toml Standards

```toml
[package]
name = "stablepay-escrow"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["cdylib", "lib"]
name = "stablepay_escrow"

[features]
default = []
cpi = ["no-entrypoint"]
no-entrypoint = []
no-idl = []
no-log-ix-name = []
idl-build = ["anchor-lang/idl-build"]

[dependencies]
anchor-lang = "0.30"
anchor-spl = "0.30"

[profile.release]
overflow-checks = true
lto = "fat"
codegen-units = 1

[lints.clippy]
cast_possible_truncation = "deny"
cast_sign_loss = "deny"
```
