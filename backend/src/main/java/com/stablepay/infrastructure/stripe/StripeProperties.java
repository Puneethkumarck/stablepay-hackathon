package com.stablepay.infrastructure.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.stripe")
@Builder(toBuilder = true)
public record StripeProperties(
    String apiKey,
    String webhookSecret,
    boolean testMode,
    boolean autoConfirm,
    String testPaymentMethod,
    String currency
) {}
