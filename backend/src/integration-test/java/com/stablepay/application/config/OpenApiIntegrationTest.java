package com.stablepay.application.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.stablepay.test.PgTest;

import lombok.SneakyThrows;

@PgTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SneakyThrows
    void shouldServeOpenApiSpec() {
        // given — springdoc auto-configuration exposes /v3/api-docs

        // when
        var result = mockMvc.perform(get("/v3/api-docs"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("StablePay API"))
                .andExpect(jsonPath("$.info.version").value("0.1.0"))
                .andExpect(jsonPath("$.paths").isNotEmpty());
    }

    @Test
    @SneakyThrows
    void shouldGroupEndpointsByTag() {
        // given — controllers are annotated with @Tag

        // when
        var result = mockMvc.perform(get("/v3/api-docs"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[?(@.name == 'Wallets')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Remittances')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Claims')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'FX Rate')]").exists());
    }

    @Test
    @SneakyThrows
    void shouldDocumentErrorResponseSchema() {
        // given — ErrorResponse is referenced in @ApiResponse annotations

        // when
        var result = mockMvc.perform(get("/v3/api-docs"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.errorCode").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.message").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.timestamp").exists())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.path").exists());
    }
}
