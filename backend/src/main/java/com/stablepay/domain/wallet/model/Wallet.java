package com.stablepay.domain.wallet.model;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.time.Instant;

import com.stablepay.domain.wallet.exception.InsufficientBalanceException;

import lombok.Builder;

@Builder(toBuilder = true)
public record Wallet(
    Long id,
    String userId,
    String solanaAddress,
    byte[] publicKey,
    byte[] keyShareData,
    byte[] peerKeyShareData,
    BigDecimal availableBalance,
    BigDecimal totalBalance,
    Instant createdAt,
    Instant updatedAt
) {
    public Wallet {
        requireNonNull(userId, "userId cannot be null");
        requireNonNull(solanaAddress, "solanaAddress cannot be null");
        requireNonNull(availableBalance, "availableBalance cannot be null");
        requireNonNull(totalBalance, "totalBalance cannot be null");
    }

    public Wallet reserveBalance(BigDecimal amount) {
        requireNonNull(amount, "amount cannot be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("SP-0004: Reserve amount must be positive: " + amount);
        }
        if (availableBalance.compareTo(amount) < 0) {
            throw InsufficientBalanceException.forAmount(amount, availableBalance);
        }
        return toBuilder()
                .availableBalance(availableBalance.subtract(amount))
                .build();
    }

    public Wallet releaseBalance(BigDecimal amount) {
        requireNonNull(amount, "amount cannot be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("SP-0004: Release amount must be positive: " + amount);
        }
        var newAvailable = availableBalance.add(amount);
        if (newAvailable.compareTo(totalBalance) > 0) {
            throw new IllegalStateException(
                    "SP-0005: Released balance would exceed total. Available after release: "
                            + newAvailable + ", Total: " + totalBalance);
        }
        return toBuilder()
                .availableBalance(newAvailable)
                .build();
    }
}
