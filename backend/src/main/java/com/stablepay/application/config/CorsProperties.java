package com.stablepay.application.config;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.cors")
@Builder(toBuilder = true)
public record CorsProperties(
    @NotEmpty List<String> allowedOrigins,
    @NotEmpty List<String> allowedMethods,
    @NotEmpty List<String> allowedHeaders,
    long maxAge
) {}
