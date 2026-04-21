package com.stablepay.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record WalletResponse(
    Long id,
    String solanaAddress,
    BigDecimal availableBalance,
    BigDecimal totalBalance,
    Instant createdAt,
    Instant updatedAt
) {}
