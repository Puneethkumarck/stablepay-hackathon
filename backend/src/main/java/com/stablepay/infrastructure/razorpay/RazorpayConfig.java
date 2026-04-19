package com.stablepay.infrastructure.razorpay;

import java.net.http.HttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "stablepay.disbursement.provider", havingValue = "razorpay")
@EnableConfigurationProperties(RazorpayDisbursementProperties.class)
public class RazorpayConfig {

    @Bean
    public RestClient razorpayRestClient(RazorpayDisbursementProperties properties) {
        log.info("Initializing RazorpayX client for account {} baseUrl={}",
                maskAccountNumber(properties.accountNumber()), properties.baseUrl());

        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.requestTimeout())
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.requestTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeaders(h -> h.setBasicAuth(properties.apiKeyId(), properties.apiKeySecret()))
                .build();
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
