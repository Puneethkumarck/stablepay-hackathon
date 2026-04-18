package com.stablepay.infrastructure.solana;

import java.util.Objects;

import lombok.Builder;

@Builder(toBuilder = true)
public record TreasuryProperties(String privateKey) {
    public TreasuryProperties {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
    }
}
