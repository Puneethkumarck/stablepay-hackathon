package com.stablepay.infrastructure.fx;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.stablepay.domain.model.FxQuote;
import com.stablepay.domain.port.outbound.FxRateProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiAdapter implements FxRateProvider {

    private static final BigDecimal FALLBACK_RATE = new BigDecimal("84.50");
    private static final long CACHE_TTL_SECONDS = 60L;

    private final RestClient exchangeRateRestClient;

    @Override
    @Cacheable(value = "fxRate", unless = "#result.source().equals('fallback')")
    public FxQuote getRate(String fromCurrency, String toCurrency) {
        try {
            return fetchLiveRate(fromCurrency, toCurrency);
        } catch (Exception ex) {
            log.warn("Failed to fetch live FX rate for {}/{}, using fallback: {}", fromCurrency, toCurrency,
                    ex.getMessage());
            return buildFallbackQuote();
        }
    }

    private FxQuote fetchLiveRate(String fromCurrency, String toCurrency) {
        var response = exchangeRateRestClient.get()
                .uri("/v6/latest/{from}", fromCurrency)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Empty response from ExchangeRate API");
        }

        @SuppressWarnings("unchecked")
        var rates = (Map<String, Object>) response.get("rates");

        if (rates == null || !rates.containsKey(toCurrency)) {
            throw new IllegalStateException("Currency " + toCurrency + " not found in API response");
        }

        var rateValue = new BigDecimal(rates.get(toCurrency).toString());
        var now = Instant.now();

        log.info("Fetched live FX rate {}/{}: {}", fromCurrency, toCurrency, rateValue);

        return FxQuote.builder()
                .rate(rateValue)
                .source("live")
                .timestamp(now)
                .expiresAt(now.plusSeconds(CACHE_TTL_SECONDS))
                .build();
    }

    private FxQuote buildFallbackQuote() {
        var now = Instant.now();
        return FxQuote.builder()
                .rate(FALLBACK_RATE)
                .source("fallback")
                .timestamp(now)
                .expiresAt(now.plusSeconds(CACHE_TTL_SECONDS))
                .build();
    }
}
