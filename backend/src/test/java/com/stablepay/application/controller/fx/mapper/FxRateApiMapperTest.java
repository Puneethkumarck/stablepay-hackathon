package com.stablepay.application.controller.fx.mapper;

import static com.stablepay.testutil.FxQuoteFixtures.fxQuoteBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.application.dto.FxRateResponse;

class FxRateApiMapperTest {

    private final FxRateApiMapper mapper = new FxRateApiMapperImpl();

    @Test
    void shouldMapFxQuoteToResponse() {
        // given
        var quote = fxQuoteBuilder().build();

        // when
        var response = mapper.toResponse(quote);

        // then
        var expected = FxRateResponse.builder()
                .rate(quote.rate())
                .source(quote.source())
                .timestamp(quote.timestamp())
                .expiresAt(quote.expiresAt())
                .build();

        assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
