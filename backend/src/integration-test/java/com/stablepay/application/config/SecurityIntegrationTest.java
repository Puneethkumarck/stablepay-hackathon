package com.stablepay.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.stablepay.test.PgTest;

import lombok.SneakyThrows;

@PgTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    class PublicEndpoints {

        @Test
        @SneakyThrows
        void shouldAllowUnauthenticatedAccessToClaimEndpoint() {
            // given

            // when
            var result = mockMvc.perform(get("/api/claims/some-token"));

            // then
            result.andExpect(status().isNotFound());
        }

        @Test
        @SneakyThrows
        void shouldAllowUnauthenticatedAccessToHealthEndpoint() {
            // given

            // when
            var result = mockMvc.perform(get("/actuator/health"))
                    .andReturn();

            // then
            assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
        }
    }

    @Nested
    class ProtectedEndpoints {

        @Test
        @SneakyThrows
        void shouldReturn401ForUnauthenticatedFxRequest() {
            // given

            // when
            var result = mockMvc.perform(get("/api/fx/USD-INR"));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("SP-0040"))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.path").value("/api/fx/USD-INR"));
        }

        @Test
        @SneakyThrows
        void shouldAllowAuthenticatedAccessToFxEndpoint() {
            // given

            // when
            var result = mockMvc.perform(get("/api/fx/USD-INR")
                    .with(jwt()));

            // then
            result.andExpect(status().isOk());
        }

        @Test
        @SneakyThrows
        void shouldReturn401WithJsonBodyWhenNoToken() {
            // given

            // when
            var result = mockMvc.perform(get("/api/remittances"));

            // then
            result.andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errorCode").value("SP-0040"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }
}
