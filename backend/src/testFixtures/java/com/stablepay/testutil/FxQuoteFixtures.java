package com.stablepay.testutil;

import java.math.BigDecimal;
import java.time.Instant;

import com.stablepay.domain.fx.model.FxQuote;

public final class FxQuoteFixtures {

    private FxQuoteFixtures() {}

    public static final BigDecimal SOME_RATE = new BigDecimal("83.25");
    public static final String SOME_SOURCE = "live";
    public static final Instant SOME_TIMESTAMP = Instant.parse("2026-04-03T10:00:00Z");
    public static final Instant SOME_EXPIRES_AT = Instant.parse("2026-04-03T10:01:00Z");

    public static FxQuote.FxQuoteBuilder fxQuoteBuilder() {
        return FxQuote.builder()
                .rate(SOME_RATE)
                .source(SOME_SOURCE)
                .timestamp(SOME_TIMESTAMP)
                .expiresAt(SOME_EXPIRES_AT);
    }
}
