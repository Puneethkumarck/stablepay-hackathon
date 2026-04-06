use anchor_lang::prelude::*;

/// Centralized error enum for the StablePay escrow program.
#[error_code]
pub enum EscrowError {
    /// Escrow amount must be greater than zero.
    #[msg("Escrow amount must be greater than zero")]
    AmountTooSmall,

    /// Escrow has already been initialized.
    #[msg("Escrow has already been initialized")]
    AlreadyInitialized,

    /// Unauthorized: caller is not the escrow sender.
    #[msg("Unauthorized: caller is not the escrow sender")]
    UnauthorizedSender,

    /// Unauthorized: caller is not the claim authority.
    #[msg("Unauthorized: caller is not the claim authority")]
    UnauthorizedClaimAuthority,

    /// Arithmetic overflow.
    #[msg("Arithmetic overflow")]
    Overflow,

    /// Escrow is not in the expected status.
    #[msg("Escrow is not in the expected status")]
    InvalidEscrowStatus,

    /// Escrow has expired.
    #[msg("Escrow has expired")]
    EscrowExpired,

    /// Escrow has not yet expired.
    #[msg("Escrow has not yet expired")]
    EscrowNotExpired,

    /// Token mint does not match expected USDC mint.
    #[msg("Token mint does not match expected USDC mint")]
    InvalidMint,

    /// Token account owner mismatch.
    #[msg("Token account owner mismatch")]
    InvalidTokenOwner,
}
