package com.stablepay.application.config;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.cors")
@Validated
@Builder(toBuilder = true)
public record CorsProperties(
    @NotEmpty List<String> allowedOrigins,
    @NotEmpty List<String> allowedMethods,
    @NotEmpty List<String> allowedHeaders,
    @Min(1) long maxAge
) {
    public CorsProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
        allowedMethods = List.copyOf(allowedMethods);
        allowedHeaders = List.copyOf(allowedHeaders);
    }
}
