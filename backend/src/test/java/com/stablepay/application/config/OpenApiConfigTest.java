package com.stablepay.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;

class OpenApiConfigTest {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void shouldConfigureApiTitle() {
        // given
        var openApi = openApiConfig.stablePayOpenApi();

        // when
        var title = openApi.getInfo().getTitle();

        // then
        assertThat(title).isEqualTo("StablePay API");
    }

    @Test
    void shouldConfigureApiVersion() {
        // given
        var openApi = openApiConfig.stablePayOpenApi();

        // when
        var version = openApi.getInfo().getVersion();

        // then
        assertThat(version).isEqualTo("0.1.0");
    }

    @Test
    void shouldConfigureApiDescription() {
        // given
        var openApi = openApiConfig.stablePayOpenApi();

        // when
        var description = openApi.getInfo().getDescription();

        // then
        assertThat(description).contains("Cross-border stablecoin remittance");
    }

    @Test
    void shouldConfigureContact() {
        // given
        var openApi = openApiConfig.stablePayOpenApi();

        // when
        var contact = openApi.getInfo().getContact();

        // then
        assertThat(contact.getName()).isEqualTo("StablePay Team");
        assertThat(contact.getUrl()).isEqualTo("https://github.com/stablepay");
    }

    @Test
    void shouldConfigureLicense() {
        // given
        var openApi = openApiConfig.stablePayOpenApi();

        // when
        var license = openApi.getInfo().getLicense();

        // then
        assertThat(license.getName()).isEqualTo("MIT");
    }

    @Test
    void shouldReturnFullyConfiguredOpenApiBean() {
        // given — expected OpenAPI configuration
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
