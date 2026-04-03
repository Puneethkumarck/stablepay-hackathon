package com.stablepay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record FxQuote(
    BigDecimal rate,
    String source,
    Instant timestamp,
    Instant expiresAt
) {}
