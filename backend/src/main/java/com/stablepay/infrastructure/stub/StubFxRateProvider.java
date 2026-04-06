package com.stablepay.infrastructure.stub;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.stablepay.domain.fx.model.FxQuote;
import com.stablepay.domain.fx.port.FxRateProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Primary
@Profile("stub")
public class StubFxRateProvider implements FxRateProvider {

    private static final BigDecimal STUB_USD_INR_RATE = new BigDecimal("83.25");
    private static final long QUOTE_TTL_SECONDS = 300;

    @Override
    public FxQuote getRate(String fromCurrency, String toCurrency) {
        log.info("STUB: Returning fixed FX rate {}={} for {}/{}",
                STUB_USD_INR_RATE, toCurrency, fromCurrency, toCurrency);

        var now = Instant.now();
        return FxQuote.builder()
                .rate(STUB_USD_INR_RATE)
                .source("stub")
                .timestamp(now)
                .expiresAt(now.plusSeconds(QUOTE_TTL_SECONDS))
                .build();
    }
}
