package com.stablepay.domain.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.exception.UnsupportedCorridorException;
import com.stablepay.domain.model.FxQuote;
import com.stablepay.domain.port.outbound.FxRateProvider;

@ExtendWith(MockitoExtension.class)
class GetFxRateQueryHandlerTest {

    @Mock
    private FxRateProvider fxRateProvider;

    @InjectMocks
    private GetFxRateQueryHandler handler;

    @Test
    void shouldReturnFxQuoteForSupportedCorridor() {
        // given
        var timestamp = Instant.parse("2026-04-03T10:00:00Z");
        var expiresAt = Instant.parse("2026-04-03T10:01:00Z");
        var quote = FxQuote.builder()
                .rate(new BigDecimal("83.25"))
                .source("live")
                .timestamp(timestamp)
                .expiresAt(expiresAt)
                .build();

        given(fxRateProvider.getRate("USD", "INR")).willReturn(quote);

        // when
        var result = handler.handle("USD", "INR");

        // then
        var expected = FxQuote.builder()
                .rate(new BigDecimal("83.25"))
                .source("live")
                .timestamp(timestamp)
                .expiresAt(expiresAt)
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowForUnsupportedCorridor() {
        // when / then
        assertThatThrownBy(() -> handler.handle("EUR", "GBP"))
                .isInstanceOf(UnsupportedCorridorException.class)
                .hasMessageContaining("SP-0009")
                .hasMessageContaining("EUR/GBP");
    }
}
