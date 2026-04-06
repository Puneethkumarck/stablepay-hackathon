use anchor_lang::prelude::*;
use anchor_spl::token::{self, Mint, Token, TokenAccount, Transfer};

use crate::constants::*;
use crate::errors::EscrowError;
use crate::state::{Escrow, EscrowStatus};

/// Deposit USDC into a new escrow PDA.
///
/// Creates an escrow account and vault token account, then transfers
/// USDC from the sender's token account into the vault.
pub fn handler(ctx: Context<Deposit>, amount: u64, deadline: i64) -> Result<()> {
    require_gt!(amount, 0, EscrowError::AmountTooSmall);

    let clock = Clock::get()?;
    require!(deadline > clock.unix_timestamp, EscrowError::EscrowExpired);

    // Initialize escrow state
    let escrow = &mut ctx.accounts.escrow;
    escrow.sender = ctx.accounts.sender.key();
    escrow.claim_authority = ctx.accounts.claim_authority.key();
    escrow.mint = ctx.accounts.usdc_mint.key();
    escrow.amount = amount;
    escrow.deadline = deadline;
    escrow.status = EscrowStatus::Active;
    escrow.bump = ctx.bumps.escrow;
    escrow.remittance_id = ctx.accounts.remittance_id.key();

    // Transfer USDC from sender to vault
    let transfer_ctx = CpiContext::new(
        ctx.accounts.token_program.to_account_info(),
        Transfer {
            from: ctx.accounts.sender_token.to_account_info(),
            to: ctx.accounts.vault.to_account_info(),
            authority: ctx.accounts.sender.to_account_info(),
        },
    );
    token::transfer(transfer_ctx, amount)?;

    Ok(())
}

/// Accounts for the deposit instruction.
#[derive(Accounts)]
#[instruction(amount: u64, deadline: i64)]
pub struct Deposit<'info> {
    /// The sender who deposits funds and pays for account creation.
    #[account(mut)]
    pub sender: Signer<'info>,

    /// The escrow PDA that holds remittance metadata.
    #[account(
        init,
        payer = sender,
        space = 8 + Escrow::INIT_SPACE,
        seeds = [ESCROW_SEED, remittance_id.key().as_ref()],
        bump,
    )]
    pub escrow: Account<'info, Escrow>,

    /// The vault token account owned by the escrow PDA.
    #[account(
        init,
        payer = sender,
        token::mint = usdc_mint,
        token::authority = escrow,
        seeds = [VAULT_SEED, escrow.key().as_ref()],
        bump,
    )]
    pub vault: Account<'info, TokenAccount>,

    /// The sender's USDC token account.
    #[account(
        mut,
        constraint = sender_token.owner == sender.key() @ EscrowError::InvalidTokenOwner,
        constraint = sender_token.mint == usdc_mint.key() @ EscrowError::InvalidMint,
    )]
    pub sender_token: Account<'info, TokenAccount>,

    /// The SPL token mint (USDC on devnet). Stored in escrow for claim/refund validation.
    pub usdc_mint: Account<'info, Mint>,

    /// The claim authority pubkey — stored but not required to sign at deposit.
    /// CHECK: Only stored in escrow; validated during claim instruction.
    pub claim_authority: UncheckedAccount<'info>,

    /// Unique remittance ID used as PDA seed.
    /// CHECK: Used only as a PDA seed — any pubkey is valid.
    pub remittance_id: UncheckedAccount<'info>,

    pub system_program: Program<'info, System>,
    pub token_program: Program<'info, Token>,
}
