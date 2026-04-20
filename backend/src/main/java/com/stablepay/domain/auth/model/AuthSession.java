package com.stablepay.domain.auth.model;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record AuthSession(
    String accessToken,
    String refreshToken,
    Instant accessExpiresAt
) {
    public AuthSession {
        requireNonNull(accessToken, "accessToken cannot be null");
        requireNonNull(refreshToken, "refreshToken cannot be null");
        requireNonNull(accessExpiresAt, "accessExpiresAt cannot be null");
    }
}
