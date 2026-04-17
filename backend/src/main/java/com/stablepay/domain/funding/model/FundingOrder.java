package com.stablepay.domain.funding.model;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record FundingOrder(
    Long id,
    UUID fundingId,
    Long walletId,
    BigDecimal amountUsdc,
    String stripePaymentIntentId,
    FundingStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public FundingOrder {
        requireNonNull(fundingId, "fundingId cannot be null");
        requireNonNull(walletId, "walletId cannot be null");
        requireNonNull(amountUsdc, "amountUsdc cannot be null");
        requireNonNull(status, "status cannot be null");
    }
}
