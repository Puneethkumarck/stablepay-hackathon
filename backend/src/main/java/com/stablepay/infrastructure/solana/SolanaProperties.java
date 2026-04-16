package com.stablepay.infrastructure.solana;

import java.util.Objects;

import org.sol4k.PublicKey;

import lombok.Builder;

@Builder(toBuilder = true)
public record SolanaProperties(
    PublicKey escrowProgramId,
    PublicKey usdcMint,
    String claimAuthorityPrivateKey,
    String rpcUrl
) {
    public SolanaProperties {
        Objects.requireNonNull(escrowProgramId, "escrowProgramId must not be null");
        Objects.requireNonNull(usdcMint, "usdcMint must not be null");
        Objects.requireNonNull(claimAuthorityPrivateKey, "claimAuthorityPrivateKey must not be null");
        Objects.requireNonNull(rpcUrl, "rpcUrl must not be null");
    }
}
