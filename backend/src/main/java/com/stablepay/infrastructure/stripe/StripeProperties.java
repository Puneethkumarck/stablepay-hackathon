package com.stablepay.infrastructure.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.stripe")
@Validated
@Builder(toBuilder = true)
public record StripeProperties(
    String apiKey,
    String webhookSecret,
    boolean testMode,
    boolean autoConfirm,
    String testPaymentMethod,
    String currency
) {}
