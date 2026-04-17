package com.stablepay.domain.funding.model;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record PaymentRequest(
    UUID fundingId,
    Long walletId,
    BigDecimal amountUsdc,
    String currency
) {}
