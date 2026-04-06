package com.stablepay.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stablePayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("StablePay API")
                        .description("Cross-border stablecoin remittance API on Solana. "
                                + "Enables instant, low-cost USD to INR remittances via USDC "
                                + "with MPC wallet abstraction and guaranteed delivery.")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("StablePay Team")
                                .url("https://github.com/stablepay"))
                        .license(new License()
                                .name("MIT")));
    }
}
