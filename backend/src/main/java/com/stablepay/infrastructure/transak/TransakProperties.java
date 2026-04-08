package com.stablepay.infrastructure.transak;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.transak")
@Validated
@Builder(toBuilder = true)
public record TransakProperties(
    @NotBlank String apiKey,
    @NotBlank String apiSecret,
    String baseUrl
) {}
