package com.stablepay.domain.auth.model;

import static java.util.Objects.requireNonNull;

import java.time.Duration;

import lombok.Builder;

@Builder(toBuilder = true)
public record AuthTokenConfig(
    Duration accessTtl,
    Duration refreshTtl
) {
    public AuthTokenConfig {
        requireNonNull(accessTtl, "accessTtl cannot be null");
        requireNonNull(refreshTtl, "refreshTtl cannot be null");
    }
}
