package com.stablepay.application.config;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Builder;

@ConfigurationProperties(prefix = "stablepay.jwt")
@Validated
@Builder(toBuilder = true)
public record JwtProperties(
    @NotBlank String secret,
    @NotNull Duration accessTtl,
    @NotNull Duration refreshTtl
) {
    public JwtProperties {
        requireNonNull(secret, "secret cannot be null");
        requireNonNull(accessTtl, "accessTtl cannot be null");
        requireNonNull(refreshTtl, "refreshTtl cannot be null");
    }
}
