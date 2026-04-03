package com.stablepay.infrastructure.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import com.stablepay.domain.model.FxQuote;

@ExtendWith(MockitoExtension.class)
class ExchangeRateApiAdapterTest {

    @Mock
    private RestClient exchangeRateRestClient;

    @Mock
    private RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;

    @InjectMocks
    private ExchangeRateApiAdapter adapter;

    @SuppressWarnings("unchecked")
    @Test
    void shouldReturnLiveRateFromApi() {
        // given
        var apiResponse = Map.of(
                "result", "success",
                "rates", Map.of("INR", 83.25));

        given(exchangeRateRestClient.get()).willReturn((RequestHeadersUriSpec) requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri("/v6/latest/{from}", "USD")).willReturn((RequestHeadersSpec) requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(apiResponse);

        // when
        var result = adapter.getRate("USD", "INR");

        // then
        var expected = FxQuote.builder()
                .rate(new BigDecimal("83.25"))
                .source("live")
                .timestamp(result.timestamp())
                .expiresAt(result.expiresAt())
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldReturnFallbackRateWhenApiFails() {
        // given
        given(exchangeRateRestClient.get()).willReturn((RequestHeadersUriSpec) requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri("/v6/latest/{from}", "USD")).willThrow(new RuntimeException("API unavailable"));

        // when
        var result = adapter.getRate("USD", "INR");

        // then
        var expected = FxQuote.builder()
                .rate(new BigDecimal("84.50"))
                .source("fallback")
                .timestamp(result.timestamp())
                .expiresAt(result.expiresAt())
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
