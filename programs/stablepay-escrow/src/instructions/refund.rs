use anchor_lang::prelude::*;
use anchor_spl::token::{self, CloseAccount, Token, TokenAccount, Transfer};

use crate::constants::*;
use crate::errors::EscrowError;
use crate::state::{Escrow, EscrowStatus};

/// Refund USDC from an expired escrow back to the sender.
///
/// Anyone can call this instruction after the escrow deadline has passed.
/// The vault tokens are transferred back to the sender's token account,
/// and the escrow account is closed with rent returned to the sender.
pub fn handler(ctx: Context<Refund>) -> Result<()> {
    let escrow = &ctx.accounts.escrow;

    // Validate that the escrow deadline has passed
    require!(
        Clock::get()?.unix_timestamp > escrow.deadline,
        EscrowError::EscrowNotExpired
    );

    // Build PDA signer seeds for the escrow authority
    let signer_seeds: &[&[&[u8]]] =
        &[&[ESCROW_SEED, escrow.remittance_id.as_ref(), &[escrow.bump]]];

    // Transfer vault USDC back to sender's token account
    let transfer_ctx = CpiContext::new_with_signer(
        ctx.accounts.token_program.to_account_info(),
        Transfer {
            from: ctx.accounts.vault.to_account_info(),
            to: ctx.accounts.sender_token.to_account_info(),
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

/// Accounts for the refund instruction.
///
/// The escrow must be active and past its deadline. Anyone can invoke
/// this instruction — the payer simply covers the transaction fee.
/// Funds are returned to the original sender, and the escrow account
/// is closed with rent reclaimed to the sender.
#[derive(Accounts)]
pub struct Refund<'info> {
    /// The caller who pays the transaction fee. Anyone can call this.
    #[account(mut)]
    pub payer: Signer<'info>,

    /// The escrow PDA — must be active and owned by the sender.
    /// Closed after refund, rent returned to sender.
    #[account(
        mut,
        close = sender,
        has_one = sender @ EscrowError::UnauthorizedSender,
        constraint = escrow.status == EscrowStatus::Active @ EscrowError::InvalidEscrowStatus,
        seeds = [ESCROW_SEED, escrow.remittance_id.as_ref()],
        bump = escrow.bump,
    )]
    pub escrow: Account<'info, Escrow>,

    /// The vault token account holding escrowed USDC.
    #[account(
        mut,
        seeds = [VAULT_SEED, escrow.key().as_ref()],
        bump,
        token::authority = escrow,
    )]
    pub vault: Account<'info, TokenAccount>,

    /// The original sender who receives rent from the closed escrow.
    /// CHECK: Validated via `has_one = sender` on the escrow account.
    #[account(mut)]
    pub sender: SystemAccount<'info>,

    /// The sender's USDC token account that receives the refund.
    #[account(
        mut,
        constraint = sender_token.owner == sender.key() @ EscrowError::InvalidTokenOwner,
        constraint = sender_token.mint == escrow.mint @ EscrowError::InvalidMint,
    )]
    pub sender_token: Account<'info, TokenAccount>,

    /// SPL Token program for CPI transfers.
    pub token_program: Program<'info, Token>,
}
