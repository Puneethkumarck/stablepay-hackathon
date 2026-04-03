package com.stablepay.infrastructure.fx;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableCaching
public class FxRateConfig {

    @Bean
    RestClient exchangeRateRestClient() {
        return RestClient.builder()
                .baseUrl("https://open.er-api.com")
                .build();
    }
}
