package com.stablepay.infrastructure.fx;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.stablepay.domain.fx.model.FxQuote;
import com.stablepay.domain.fx.port.FxRateProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiAdapter implements FxRateProvider {

    private final RestClient fxRestClient;

    @Override
    @Cacheable(value = "fxRate", unless = "#result.source().equals('fallback')")
    public FxQuote getRate(String fromCurrency, String toCurrency) {
        try {
            var response = fxRestClient.get()
                    .uri("/v6/latest/{from}", fromCurrency)
                    .retrieve()
                    .body(ExchangeRateResponse.class);

            if (response == null || response.rates() == null) {
                log.warn("Null response from exchange rate API, using fallback");
                return buildFallbackQuote();
            }

            var rate = response.rates().get(toCurrency);
            if (rate == null) {
                log.warn("No rate found for {} in API response, using fallback", toCurrency);
                return buildFallbackQuote();
            }

            var now = Instant.now();
            return FxQuote.builder()
                    .rate(rate)
                    .source("open.er-api.com")
                    .timestamp(now)
                    .expiresAt(now.plusSeconds(60))
                    .build();

        } catch (Exception ex) {
            log.warn("Failed to fetch FX rate from API: {}", ex.getMessage());
            return buildFallbackQuote();
        }
    }

    private FxQuote buildFallbackQuote() {
        var now = Instant.now();
        return FxQuote.builder()
                .rate(BigDecimal.valueOf(84.50))
                .source("fallback")
                .timestamp(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }

    record ExchangeRateResponse(
        String result,
        Map<String, BigDecimal> rates
    ) {}
}
