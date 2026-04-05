package com.stablepay.application.config;

import java.time.Duration;
import java.util.List;

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
        boolean enabled,
        @NotBlank String target,
        @NotBlank String namespace,
        @NotBlank String taskQueue,
        @NotNull Duration workflowExecutionTimeout,
        @NotNull Duration workflowRunTimeout,
        List<String> nonRetryableExceptions,
        @NotNull @Valid ActivityOptionsProperties activityOptions
    ) {

        @Builder(toBuilder = true)
        public record ActivityOptionsProperties(
            @NotNull @Valid ActivityConfig defaultOptions,
            @NotNull @Valid ActivityConfig signing,
            @NotNull @Valid ActivityConfig solanaSubmission,
            @NotNull @Valid ActivityConfig smsDelivery
        ) {}

        @Builder(toBuilder = true)
        public record ActivityConfig(
            @NotNull Duration startToCloseTimeout,
            int maxAttempts,
            @NotNull Duration initialInterval,
            double backoffCoefficient
        ) {}
    }
}
