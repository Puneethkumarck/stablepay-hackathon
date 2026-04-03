package com.stablepay.infrastructure.fx;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.stablepay.domain.fx.port.FxRateProvider;
import com.stablepay.test.IntegrationTestConfig;
import com.stablepay.test.PostgresContainerExtension;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(PostgresContainerExtension.class)
@Import(IntegrationTestConfig.class)
class ExchangeRateApiIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("stablepay.fx.api.base-url", wireMock::baseUrl);
    }

    @Autowired
    private FxRateProvider fxRateProvider;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .forEach(org.springframework.cache.Cache::clear);
    }

    @Test
    void shouldReturnLiveRateFromApi() {
        // given
        wireMock.stubFor(get(urlPathEqualTo("/v6/latest/USD"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "result": "success",
                                    "rates": {
                                        "INR": 83.25
                                    }
                                }
                                """)));

        // when
        var quote = fxRateProvider.getRate("USD", "INR");

        // then
        assertThat(quote.rate()).isEqualByComparingTo(new BigDecimal("83.25"));
        assertThat(quote.source()).isEqualTo("open.er-api.com");
    }

    @Test
    void shouldReturnFallbackWhenApiReturns500() {
        // given
        wireMock.stubFor(get(urlPathEqualTo("/v6/latest/USD"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // when
        var quote = fxRateProvider.getRate("USD", "INR");

        // then
        assertThat(quote.rate()).isEqualByComparingTo(new BigDecimal("84.50"));
        assertThat(quote.source()).isEqualTo("fallback");
    }

    @Test
    void shouldReturnFallbackWhenApiTimesOut() {
        // given
        wireMock.stubFor(get(urlPathEqualTo("/v6/latest/USD"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(15000)
                        .withBody("""
                                {
                                    "result": "success",
                                    "rates": {
                                        "INR": 83.25
                                    }
                                }
                                """)));

        // when
        var quote = fxRateProvider.getRate("USD", "INR");

        // then
        assertThat(quote.rate()).isEqualByComparingTo(new BigDecimal("84.50"));
        assertThat(quote.source()).isEqualTo("fallback");
    }
}
