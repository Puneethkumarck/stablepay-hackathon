package com.stablepay.application.controller;

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

import com.stablepay.application.mapper.FxRateApiMapper;
import com.stablepay.domain.exception.UnsupportedCorridorException;
import com.stablepay.domain.handler.GetFxRateQueryHandler;
import com.stablepay.domain.model.FxQuote;

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
        var timestamp = Instant.parse("2026-04-03T10:00:00Z");
        var expiresAt = Instant.parse("2026-04-03T10:01:00Z");
        var rate = new BigDecimal("83.25");

        var quote = FxQuote.builder()
                .rate(rate)
                .source("live")
                .timestamp(timestamp)
                .expiresAt(expiresAt)
                .build();

        var response = com.stablepay.application.dto.FxRateResponse.builder()
                .rate(rate)
                .source("live")
                .timestamp(timestamp)
                .expiresAt(expiresAt)
                .build();

        given(getFxRateQueryHandler.handle("USD", "INR")).willReturn(quote);
        given(fxRateApiMapper.toResponse(quote)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/fx/USD-INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(83.25))
                .andExpect(jsonPath("$.source").value("live"))
                .andExpect(jsonPath("$.timestamp").value("2026-04-03T10:00:00Z"))
                .andExpect(jsonPath("$.expiresAt").value("2026-04-03T10:01:00Z"));
    }

    @Test
    void shouldReturnFallbackFxRate() throws Exception {
        // given
        var timestamp = Instant.parse("2026-04-03T10:00:00Z");
        var expiresAt = Instant.parse("2026-04-03T10:01:00Z");
        var fallbackRate = new BigDecimal("84.50");

        var quote = FxQuote.builder()
                .rate(fallbackRate)
                .source("fallback")
                .timestamp(timestamp)
                .expiresAt(expiresAt)
                .build();

        var response = com.stablepay.application.dto.FxRateResponse.builder()
                .rate(fallbackRate)
                .source("fallback")
                .timestamp(timestamp)
                .expiresAt(expiresAt)
                .build();

        given(getFxRateQueryHandler.handle("USD", "INR")).willReturn(quote);
        given(fxRateApiMapper.toResponse(quote)).willReturn(response);

        // when / then
        mockMvc.perform(get("/api/fx/USD-INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate").value(84.50))
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.timestamp").value("2026-04-03T10:00:00Z"))
                .andExpect(jsonPath("$.expiresAt").value("2026-04-03T10:01:00Z"));
    }

    @Test
    void shouldReturn400ForUnsupportedCorridor() throws Exception {
        // given
        given(getFxRateQueryHandler.handle("EUR", "GBP"))
                .willThrow(UnsupportedCorridorException.forPair("EUR", "GBP"));

        // when / then
        mockMvc.perform(get("/api/fx/EUR-GBP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SP-0009"))
                .andExpect(jsonPath("$.message").value("SP-0009: Unsupported corridor: EUR/GBP"));
    }
}
