package com.stablepay.infrastructure.razorpay;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.razorpay")
@Validated
@Builder(toBuilder = true)
public record RazorpayDisbursementProperties(
        @NotBlank String apiKeyId,
        @NotBlank String apiKeySecret,
        @NotBlank String accountNumber,
        @NotBlank String baseUrl,
        @NotNull Duration requestTimeout) {}
