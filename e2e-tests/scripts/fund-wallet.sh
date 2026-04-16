#!/bin/bash
# Fund an MPC wallet on localnet with SOL and USDC for E2E testing
# Usage: ./fund-wallet.sh <solana-address> <usdc-amount> <usdc-mint>

set -e

WALLET_ADDR=$1
USDC_AMOUNT=${2:-100}
USDC_MINT=$3
RPC_URL=${SOLANA_RPC_URL:-http://localhost:8899}

if [ -z "$WALLET_ADDR" ] || [ -z "$USDC_MINT" ]; then
  echo "Usage: $0 <solana-address> <usdc-amount> <usdc-mint>"
  exit 1
fi

echo "Funding wallet $WALLET_ADDR on $RPC_URL"

# 1. Airdrop SOL for transaction fees
echo "  Airdropping 2 SOL..."
solana airdrop 2 "$WALLET_ADDR" --url "$RPC_URL" 2>&1

# 2. Create associated token account and mint USDC
echo "  Creating USDC token account and minting $USDC_AMOUNT USDC..."
spl-token create-account "$USDC_MINT" --owner "$WALLET_ADDR" --url "$RPC_URL" --fee-payer ~/.config/solana/id.json 2>&1 || true
spl-token mint "$USDC_MINT" "$USDC_AMOUNT" --recipient-owner "$WALLET_ADDR" --url "$RPC_URL" 2>&1

echo "  Done. Wallet $WALLET_ADDR funded with 2 SOL + $USDC_AMOUNT USDC"
