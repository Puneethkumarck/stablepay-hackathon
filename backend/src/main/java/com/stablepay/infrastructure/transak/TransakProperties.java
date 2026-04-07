package com.stablepay.infrastructure.transak;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.transak")
@Builder(toBuilder = true)
public record TransakProperties(
    @NotBlank String apiKey,
    @NotBlank String apiSecret,
    String baseUrl
) {}
