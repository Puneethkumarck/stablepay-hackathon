package com.stablepay.application.controller.fx;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.stablepay.application.controller.fx.mapper.FxRateApiMapper;
import com.stablepay.application.dto.FxRateResponse;
import com.stablepay.domain.fx.exception.UnsupportedCorridorException;
import com.stablepay.domain.fx.handler.GetFxRateQueryHandler;
import com.stablepay.domain.fx.model.FxQuote;

@WebMvcTest(FxRateController.class)
class FxRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetFxRateQueryHandler getFxRateQueryHandler;

    @MockitoBean
    private FxRateApiMapper fxRateApiMapper;

    @Test
    void shouldReturnLiveFxRate() throws Exception {
        // given
        var now = Instant.parse("2026-04-03T10:00:00Z");
        var expiresAt = now.plusSeconds(60);
        var quote = FxQuote.builder()
                .rate(BigDecimal.valueOf(83.25))
                .source("open.er-api.com")
                .timestamp(now)
                .expiresAt(expiresAt)
                .build();
        var response = FxRateResponse.builder()
                .rate(BigDecimal.valueOf(83.25))
                .source("open.er-api.com")
                .timestamp(now)
                .expiresAt(expiresAt)
                .build();

        given(getFxRateQueryHandler.handle("USD", "INR")).willReturn(quote);
        given(fxRateApiMapper.toResponse(quote)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/fx/USD-INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(83.25))
                .andExpect(jsonPath("$.source").value("open.er-api.com"));
    }

    @Test
    void shouldReturnFallbackFxRate() throws Exception {
        // given
        var now = Instant.parse("2026-04-03T10:00:00Z");
        var expiresAt = now.plusSeconds(60);
        var quote = FxQuote.builder()
                .rate(BigDecimal.valueOf(84.50))
                .source("fallback")
                .timestamp(now)
                .expiresAt(expiresAt)
                .build();
        var response = FxRateResponse.builder()
                .rate(BigDecimal.valueOf(84.50))
                .source("fallback")
                .timestamp(now)
                .expiresAt(expiresAt)
                .build();

        given(getFxRateQueryHandler.handle("USD", "INR")).willReturn(quote);
        given(fxRateApiMapper.toResponse(quote)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/fx/usd-inr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(84.50))
                .andExpect(jsonPath("$.source").value("fallback"));
    }

    @Test
    void shouldReturn400ForUnsupportedCorridor() throws Exception {
        // given
        given(getFxRateQueryHandler.handle("EUR", "INR"))
                .willThrow(UnsupportedCorridorException.forPair("EUR", "INR"));

        // when / then
        mockMvc.perform(get("/api/fx/EUR-INR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0009"))
                .andExpect(jsonPath("$.message").value("SP-0009: Unsupported corridor: EUR -> INR"));
    }
}
