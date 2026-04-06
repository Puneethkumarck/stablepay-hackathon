use anchor_lang::prelude::*;
use anchor_spl::token::{self, CloseAccount, Token, TokenAccount, Transfer};

use crate::constants::*;
use crate::errors::EscrowError;
use crate::state::{Escrow, EscrowStatus};

/// Claim USDC from an active escrow, transferring vault funds to the recipient.
///
/// The claim authority (backend-controlled signer) authorizes the release.
/// Vault USDC is transferred to the recipient's token account via CPI,
/// and the escrow account is closed with rent returned to the sender.
pub fn handler(ctx: Context<Claim>) -> Result<()> {
    let escrow = &ctx.accounts.escrow;

    // Build PDA signer seeds for the CPI transfer
    let signer_seeds: &[&[&[u8]]] =
        &[&[ESCROW_SEED, escrow.remittance_id.as_ref(), &[escrow.bump]]];

    // Transfer all vault USDC to the recipient token account
    let transfer_ctx = CpiContext::new_with_signer(
        ctx.accounts.token_program.to_account_info(),
        Transfer {
            from: ctx.accounts.vault.to_account_info(),
            to: ctx.accounts.recipient_token.to_account_info(),
            authority: ctx.accounts.escrow.to_account_info(),
        },
        signer_seeds,
    );
    token::transfer(transfer_ctx, ctx.accounts.vault.amount)?;

    // Reload vault after CPI to reflect updated balance
    ctx.accounts.vault.reload()?;

    // Close the vault token account — return rent to sender
    let close_ctx = CpiContext::new_with_signer(
        ctx.accounts.token_program.to_account_info(),
        CloseAccount {
            account: ctx.accounts.vault.to_account_info(),
            destination: ctx.accounts.sender.to_account_info(),
            authority: ctx.accounts.escrow.to_account_info(),
        },
        signer_seeds,
    );
    token::close_account(close_ctx)?;

    Ok(())
}

/// Accounts for the claim instruction.
///
/// Validates that the caller is the authorized claim authority,
/// the escrow is active, and the recipient token account matches
/// the escrow mint. Closes the escrow and returns rent to the sender.
#[derive(Accounts)]
pub struct Claim<'info> {
    /// The backend-controlled signer that authorizes the claim.
    pub claim_authority: Signer<'info>,

    /// The escrow PDA holding remittance metadata.
    /// Closed after claim — rent returned to sender.
    #[account(
        mut,
        close = sender,
        has_one = claim_authority @ EscrowError::UnauthorizedClaimAuthority,
        has_one = sender @ EscrowError::UnauthorizedSender,
        constraint = escrow.status == EscrowStatus::Active @ EscrowError::InvalidEscrowStatus,
        seeds = [ESCROW_SEED, escrow.remittance_id.as_ref()],
        bump = escrow.bump,
    )]
    pub escrow: Account<'info, Escrow>,

    /// The vault token account owned by the escrow PDA.
    #[account(
        mut,
        seeds = [VAULT_SEED, escrow.key().as_ref()],
        bump,
        token::authority = escrow,
    )]
    pub vault: Account<'info, TokenAccount>,

    /// The recipient's USDC token account to receive the funds.
    #[account(
        mut,
        constraint = recipient_token.mint == escrow.mint @ EscrowError::InvalidMint,
    )]
    pub recipient_token: Account<'info, TokenAccount>,

    /// The original sender who receives rent from the closed escrow.
    /// CHECK: Validated via `has_one = sender` on the escrow account.
    #[account(mut)]
    pub sender: SystemAccount<'info>,

    /// SPL Token program for CPI transfers.
    pub token_program: Program<'info, Token>,
}
