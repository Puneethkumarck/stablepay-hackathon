package com.stablepay.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record FxRateResponse(
    BigDecimal rate,
    String source,
    Instant timestamp,
    Instant expiresAt
) {}
