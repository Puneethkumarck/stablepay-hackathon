use anchor_lang::prelude::*;

/// PDA seed for escrow accounts.
pub const ESCROW_SEED: &[u8] = b"escrow";

/// PDA seed for vault token accounts.
pub const VAULT_SEED: &[u8] = b"vault";

/// USDC decimals on Solana.
pub const USDC_DECIMALS: u8 = 6;

/// Escrow expiry duration in seconds (48 hours).
pub const ESCROW_EXPIRY_SECONDS: i64 = 172_800;

/// USDC devnet mint address.
/// This is the standard USDC devnet mint from Circle.
pub const USDC_MINT: Pubkey = pubkey!("4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU");
