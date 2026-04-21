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
        if (accessTtl.isNegative() || accessTtl.isZero()) {
            throw new IllegalArgumentException("accessTtl must be positive");
        }
        if (refreshTtl.isNegative() || refreshTtl.isZero()) {
            throw new IllegalArgumentException("refreshTtl must be positive");
        }
    }
}
