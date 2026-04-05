package com.stablepay.infrastructure.solana;

import org.sol4k.PublicKey;

import lombok.Builder;

@Builder(toBuilder = true)
public record SolanaProperties(
    PublicKey escrowProgramId,
    PublicKey usdcMint,
    String claimAuthorityPrivateKey
) {}
