package com.stablepay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record Wallet(
    Long id,
    String userId,
    String solanaAddress,
    BigDecimal availableBalance,
    BigDecimal totalBalance,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {
    public Wallet reserveBalance(BigDecimal amount) {
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                "SP-0002: Insufficient balance. Requested: " + amount + ", Available: " + availableBalance);
        }
        return toBuilder()
                .availableBalance(availableBalance.subtract(amount))
                .build();
    }

    public Wallet releaseBalance(BigDecimal amount) {
        return toBuilder()
                .availableBalance(availableBalance.add(amount))
                .build();
    }
}
