package com.stablepay.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void shouldReturnFullyConfiguredOpenApiBean() {
        // given
        var expected = new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("StablePay API")
                        .description("Cross-border stablecoin remittance API on Solana. "
                                + "Enables instant, low-cost USD to INR remittances via USDC "
                                + "with MPC wallet abstraction and guaranteed delivery.")
                        .version("0.1.0")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("StablePay Team")
                                .url("https://github.com/stablepay"))
                        .license(new io.swagger.v3.oas.models.info.License()
                                .name("MIT")));

        // when
        var result = openApiConfig.stablePayOpenApi();

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
