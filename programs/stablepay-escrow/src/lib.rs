use anchor_lang::prelude::*;

declare_id!("7C2zsbhgDnxQuC1Nd2rzXQfsfnKazQWFpoUJNqS8zWij");

pub mod constants;
pub mod errors;
pub mod instructions;
pub mod state;

use instructions::*;

#[program]
pub mod stablepay_escrow {
    use super::*;

    /// Lock USDC in an escrow PDA for a cross-border remittance.
    pub fn deposit(ctx: Context<Deposit>, amount: u64, deadline: i64) -> Result<()> {
        instructions::deposit::handler(ctx, amount, deadline)
    }

    /// Release escrowed USDC to the recipient and close the escrow.
    pub fn claim(ctx: Context<Claim>) -> Result<()> {
        instructions::claim::handler(ctx)
    }

    /// Refund escrowed USDC to the sender after deadline expiry.
    pub fn refund(ctx: Context<Refund>) -> Result<()> {
        instructions::refund::handler(ctx)
    }

    /// Cancel an active escrow — sender-initiated, returns USDC.
    pub fn cancel(ctx: Context<Cancel>) -> Result<()> {
        instructions::cancel::handler(ctx)
    }
}
