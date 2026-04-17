package com.stablepay.infrastructure.stripe;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.StripeClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableConfigurationProperties(StripeProperties.class)
@RequiredArgsConstructor
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(StripeProperties properties) {
        log.info("Initializing Stripe client (testMode={}, autoConfirm={})",
                properties.testMode(), properties.autoConfirm());
        return new StripeClient(properties.apiKey());
    }
}
