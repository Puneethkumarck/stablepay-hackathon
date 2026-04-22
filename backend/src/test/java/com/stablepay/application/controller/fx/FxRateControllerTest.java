package com.stablepay.application.controller.fx;

import static com.stablepay.testutil.FxQuoteFixtures.SOME_RATE;
import static com.stablepay.testutil.FxQuoteFixtures.fxQuoteBuilder;
import static com.stablepay.testutil.SecurityTestBase.asUser;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.stablepay.application.controller.fx.mapper.FxRateApiMapper;
import com.stablepay.application.controller.fx.mapper.FxRateApiMapperImpl;
import com.stablepay.domain.fx.exception.UnsupportedCorridorException;
import com.stablepay.domain.fx.handler.GetFxRateQueryHandler;
import com.stablepay.testutil.TestClockConfig;
import com.stablepay.testutil.TestSecurityConfig;

import lombok.SneakyThrows;

@WebMvcTest(FxRateController.class)
@Import({TestClockConfig.class, TestSecurityConfig.class, FxRateApiMapperImpl.class})
class FxRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetFxRateQueryHandler getFxRateQueryHandler;

    @MockitoSpyBean
    private FxRateApiMapper fxRateApiMapper;

    @Test
    @SneakyThrows
    void shouldReturnLiveFxRate() {
        // given
        var quote = fxQuoteBuilder()
                .source("open.er-api.com")
                .build();

        given(getFxRateQueryHandler.handle("USD", "INR")).willReturn(quote);

        // when / then
        mockMvc.perform(get("/api/fx/USD-INR").with(asUser(SOME_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(SOME_RATE.doubleValue()))
                .andExpect(jsonPath("$.source").value("open.er-api.com"));
    }

    @Test
    @SneakyThrows
    void shouldReturnFallbackFxRate() {
        // given
        var quote = fxQuoteBuilder()
                .rate(BigDecimal.valueOf(84.50))
                .source("fallback")
                .build();

        given(getFxRateQueryHandler.handle("USD", "INR")).willReturn(quote);

        // when / then
        mockMvc.perform(get("/api/fx/usd-inr").with(asUser(SOME_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(84.50))
                .andExpect(jsonPath("$.source").value("fallback"));
    }

    @Test
    @SneakyThrows
    void shouldReturn400ForUnsupportedCorridor() {
        // given
        given(getFxRateQueryHandler.handle("EUR", "INR"))
                .willThrow(UnsupportedCorridorException.forPair("EUR", "INR"));

        // when / then
        mockMvc.perform(get("/api/fx/EUR-INR").with(asUser(SOME_USER_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0009"))
                .andExpect(jsonPath("$.message").value("SP-0009: Unsupported corridor: EUR -> INR"));
    }

    @Test
    @SneakyThrows
    void shouldReturn401WhenNoBearer() {
        // given

        // when / then
        mockMvc.perform(get("/api/fx/USD-INR"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SP-0040"));
    }
}
