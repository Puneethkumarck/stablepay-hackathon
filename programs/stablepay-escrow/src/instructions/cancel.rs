use anchor_lang::prelude::*;
use anchor_spl::token::{self, CloseAccount, Token, TokenAccount, Transfer};

use crate::constants::*;
use crate::errors::EscrowError;
use crate::state::{Escrow, EscrowStatus};

/// Cancel an active escrow and return USDC to the sender.
///
/// Only the original sender may cancel. The vault tokens are transferred
/// back to the sender's token account, and the escrow account is closed
/// with rent returned to the sender.
pub fn handler(ctx: Context<Cancel>) -> Result<()> {
    let escrow = &ctx.accounts.escrow;

    // Build PDA signer seeds for CPI
    let signer_seeds: &[&[&[u8]]] =
        &[&[ESCROW_SEED, escrow.remittance_id.as_ref(), &[escrow.bump]]];

    // Transfer vault USDC back to sender
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

    // Reload vault after CPI to reflect zero balance
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

/// Accounts for the cancel instruction.
///
/// Closes the escrow and returns all vault tokens plus rent to the sender.
#[derive(Accounts)]
pub struct Cancel<'info> {
    /// The original sender who cancels and receives refunded tokens plus rent.
    #[account(mut)]
    pub sender: Signer<'info>,

    /// The escrow PDA — must be active and owned by sender.
    /// Closed after cancel; rent returned to sender.
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

    /// The sender's USDC token account that receives the refunded tokens.
    #[account(
        mut,
        constraint = sender_token.owner == sender.key() @ EscrowError::InvalidTokenOwner,
        constraint = sender_token.mint == escrow.mint @ EscrowError::InvalidMint,
    )]
    pub sender_token: Account<'info, TokenAccount>,

    /// SPL Token program for CPI transfers.
    pub token_program: Program<'info, Token>,
}
