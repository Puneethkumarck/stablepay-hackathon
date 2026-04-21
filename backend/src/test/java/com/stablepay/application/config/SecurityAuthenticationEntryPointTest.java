package com.stablepay.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stablepay.application.dto.ErrorResponse;

import lombok.Builder;
import lombok.SneakyThrows;

class SecurityAuthenticationEntryPointTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-05T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final SecurityAuthenticationEntryPoint entryPoint =
            new SecurityAuthenticationEntryPoint(FIXED_CLOCK);

    @Test
    @SneakyThrows
    void shouldReturn401WithJsonErrorResponse() {
        // given
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/fx/USD-INR");
        var response = new MockHttpServletResponse();
        var authException = new BadCredentialsException("No token provided");

        // when
        entryPoint.commence(request, response, authException);

        // then
        var expected = EntryPointResult.builder()
                .status(401)
                .contentType("application/json;charset=UTF-8")
                .body(ErrorResponse.builder()
                        .errorCode("SP-0040")
                        .message("SP-0040: Authentication required")
                        .timestamp(FIXED_INSTANT)
                        .path("/api/fx/USD-INR")
                        .build())
                .build();

        var actual = EntryPointResult.builder()
                .status(response.getStatus())
                .contentType(response.getContentType())
                .body(objectMapper.readValue(response.getContentAsString(), ErrorResponse.class))
                .build();

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Builder
    private record EntryPointResult(int status, String contentType, ErrorResponse body) {}
}
