package com.stablepay.application.config;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay")
@Builder(toBuilder = true)
public record StablePayProperties(
    @Valid FxProperties fx,
    @Valid TemporalProperties temporal
) {

    @Builder(toBuilder = true)
    public record FxProperties(
        @Valid ApiProperties api
    ) {

        @Builder(toBuilder = true)
        public record ApiProperties(
            @NotBlank String baseUrl
        ) {}
    }

    @Builder(toBuilder = true)
    public record TemporalProperties(
        @NotNull Duration workflowExecutionTimeout,
        @NotNull Duration workflowRunTimeout,
        @NotNull Duration claimExpiryTimeout,
        @NotBlank String claimBaseUrl
    ) {}
}
