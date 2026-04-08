package com.stablepay.infrastructure.transak;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "stablepay.transak.enabled", havingValue = "true")
@EnableConfigurationProperties(TransakProperties.class)
public class TransakConfig {

    private static final String DEFAULT_BASE_URL = "https://staging-api.transak.com";

    @Bean
    public RestClient transakRestClient(TransakProperties properties) {
        var baseUrl = properties.baseUrl() != null ? properties.baseUrl() : DEFAULT_BASE_URL;
        log.info("Initializing Transak REST client with base URL {}", baseUrl);

        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Transak-Api-Key", properties.apiKey())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public TransakDisbursementAdapter transakDisbursementAdapter(
            RestClient transakRestClient, TransakProperties properties) {
        return new TransakDisbursementAdapter(transakRestClient, properties);
    }
}
