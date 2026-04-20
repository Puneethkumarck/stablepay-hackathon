package com.stablepay.domain.auth.model;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record RefreshToken(
    UUID id,
    UUID userId,
    String tokenHash,
    Instant expiresAt,
    Instant revokedAt
) {
    public RefreshToken {
        requireNonNull(id, "id cannot be null");
        requireNonNull(userId, "userId cannot be null");
        requireNonNull(tokenHash, "tokenHash cannot be null");
        requireNonNull(expiresAt, "expiresAt cannot be null");
    }
}
