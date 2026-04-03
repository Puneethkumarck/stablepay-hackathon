package com.stablepay.domain.fx.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.fx.exception.UnsupportedCorridorException;
import com.stablepay.domain.fx.port.FxRateProvider;
import com.stablepay.testutil.FxQuoteFixtures;

@ExtendWith(MockitoExtension.class)
class GetFxRateQueryHandlerTest {

    @Mock
    private FxRateProvider fxRateProvider;

    @InjectMocks
    private GetFxRateQueryHandler handler;

    @Test
    void shouldReturnFxQuoteForSupportedCorridor() {
        // given
        var expectedQuote = FxQuoteFixtures.fxQuoteBuilder()
                .source("open.er-api.com")
                .build();
        given(fxRateProvider.getRate("USD", "INR")).willReturn(expectedQuote);

        // when
        var result = handler.handle("USD", "INR");

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expectedQuote);
    }

    @Test
    void shouldThrowForUnsupportedCorridor() {
        // given
        var from = "EUR";
        var to = "INR";

        // when / then
        assertThatThrownBy(() -> handler.handle(from, to))
                .isInstanceOf(UnsupportedCorridorException.class)
                .hasMessageContaining("SP-0009");
    }
}
