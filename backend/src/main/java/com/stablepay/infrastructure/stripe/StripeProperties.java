package com.stablepay.infrastructure.stripe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.stripe")
@Validated
@Builder(toBuilder = true)
public record StripeProperties(
    @NotBlank @Pattern(regexp = "^sk_.+", message = "apiKey must start with sk_") String apiKey,
    @NotBlank String webhookSecret,
    boolean testMode,
    boolean autoConfirm,
    String testPaymentMethod,
    @NotBlank String currency
) {}
