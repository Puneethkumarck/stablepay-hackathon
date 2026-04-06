use anchor_lang::prelude::*;

/// Escrow account holding USDC for a cross-border remittance.
///
/// Created by the deposit instruction. Released to the recipient on claim,
/// or refunded to the sender after expiry.
#[account]
#[derive(InitSpace)]
pub struct Escrow {
    /// The sender who deposited funds.
    pub sender: Pubkey,

    /// Backend-controlled signer that authorizes claim.
    pub claim_authority: Pubkey,

    /// SPL token mint (USDC).
    pub mint: Pubkey,

    /// Deposited amount in token base units.
    pub amount: u64,

    /// Unix timestamp after which refund is allowed.
    pub deadline: i64,

    /// Current lifecycle state.
    pub status: EscrowStatus,

    /// Stored canonical PDA bump.
    pub bump: u8,

    /// Unique ID linking on-chain escrow to off-chain remittance.
    pub remittance_id: Pubkey,
}

/// Lifecycle state of an escrow.
#[derive(AnchorSerialize, AnchorDeserialize, Clone, Copy, PartialEq, Eq, InitSpace)]
pub enum EscrowStatus {
    /// Funds locked, awaiting claim or expiry.
    Active,
    /// Released to recipient.
    Claimed,
    /// Returned to sender after expiry.
    Refunded,
    /// Returned to sender before claim.
    Cancelled,
}
