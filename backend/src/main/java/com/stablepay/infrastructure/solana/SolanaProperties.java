package com.stablepay.infrastructure.solana;

import java.util.Objects;

import org.sol4k.PublicKey;

import lombok.Builder;

@Builder(toBuilder = true)
public record SolanaProperties(
    PublicKey escrowProgramId,
    PublicKey usdcMint,
    String claimAuthorityPrivateKey
) {
    public SolanaProperties {
        Objects.requireNonNull(escrowProgramId, "escrowProgramId must not be null");
        Objects.requireNonNull(usdcMint, "usdcMint must not be null");
    }
}
