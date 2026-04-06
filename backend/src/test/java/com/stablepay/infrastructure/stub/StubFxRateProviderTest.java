package com.stablepay.infrastructure.stub;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.stablepay.domain.fx.model.FxQuote;

class StubFxRateProviderTest {

    private final StubFxRateProvider stubProvider = new StubFxRateProvider();

    @Test
    void shouldReturnFixedUsdInrRate() {
        // given
        var before = Instant.now();

        // when
        var result = stubProvider.getRate("USD", "INR");

        // then
        var expected = FxQuote.builder()
                .rate(new BigDecimal("83.25"))
                .source("stub")
                .timestamp(result.timestamp())
                .expiresAt(result.expiresAt())
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        assertThat(result.timestamp()).isAfterOrEqualTo(before);
        assertThat(result.expiresAt()).isAfter(result.timestamp());
    }

    @Test
    void shouldReturnSameRateForAnyCurrencyPair() {
        // given — stub returns the same rate regardless of input

        // when
        var usdInr = stubProvider.getRate("USD", "INR");
        var eurGbp = stubProvider.getRate("EUR", "GBP");

        // then
        assertThat(usdInr.rate()).isEqualTo(eurGbp.rate());
        assertThat(usdInr.source()).isEqualTo("stub");
    }
}
