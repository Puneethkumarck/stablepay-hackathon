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
) {}
