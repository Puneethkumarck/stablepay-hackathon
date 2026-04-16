package com.stablepay.domain.wallet.model;

import static java.util.Objects.requireNonNull;

import lombok.Builder;

@Builder(toBuilder = true)
public record GeneratedKey(
    String solanaAddress,
    byte[] publicKey,
    byte[] keyShareData,
    byte[] peerKeyShareData
) {
    public GeneratedKey {
        requireNonNull(solanaAddress, "solanaAddress cannot be null");
        requireNonNull(publicKey, "publicKey cannot be null");
        requireNonNull(keyShareData, "keyShareData cannot be null");
    }
}
