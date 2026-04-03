package com.stablepay.infrastructure.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import com.stablepay.domain.fx.model.FxQuote;
import com.stablepay.infrastructure.fx.ExchangeRateApiAdapter.ExchangeRateResponse;
import com.stablepay.testutil.FxQuoteFixtures;

@ExtendWith(MockitoExtension.class)
class ExchangeRateApiAdapterTest {

    @Mock
    private RestClient fxRestClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ExchangeRateApiAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ExchangeRateApiAdapter(fxRestClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldReturnLiveRate() {
        // given
        var apiResponse = new ExchangeRateResponse(
                "success",
                Map.of("INR", FxQuoteFixtures.SOME_RATE));

        given(fxRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri("/v6/latest/{from}", "USD")).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(ExchangeRateResponse.class)).willReturn(apiResponse);

        // when
        var result = adapter.getRate("USD", "INR");

        // then
        var expected = FxQuote.builder()
                .rate(FxQuoteFixtures.SOME_RATE)
                .source("open.er-api.com")
                .timestamp(result.timestamp())
                .expiresAt(result.timestamp().plusSeconds(60))
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("timestamp", "expiresAt")
                .isEqualTo(expected);

        assertThat(Duration.between(result.timestamp(), result.expiresAt()))
                .isEqualTo(Duration.ofSeconds(60));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldReturnFallbackWhenApiFails() {
        // given
        given(fxRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri("/v6/latest/{from}", "USD"))
                .willThrow(new RuntimeException("Connection refused"));

        // when
        var result = adapter.getRate("USD", "INR");

        // then
        var expected = FxQuote.builder()
                .rate(BigDecimal.valueOf(84.50))
                .source("fallback")
                .timestamp(result.timestamp())
                .expiresAt(result.timestamp().plusSeconds(60))
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("timestamp", "expiresAt")
                .isEqualTo(expected);

        assertThat(Duration.between(result.timestamp(), result.expiresAt()))
                .isEqualTo(Duration.ofSeconds(60));
    }
}
