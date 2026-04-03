package com.stablepay.domain.model;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record FxQuote(
    BigDecimal rate,
    String source,
    Instant timestamp,
    Instant expiresAt
) {
    public FxQuote {
        requireNonNull(rate, "rate cannot be null");
        requireNonNull(source, "source cannot be null");
        requireNonNull(timestamp, "timestamp cannot be null");
    }
}
