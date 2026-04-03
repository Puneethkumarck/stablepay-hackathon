package com.stablepay.domain.model;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record ClaimToken(
    Long id,
    UUID remittanceId,
    String token,
    boolean claimed,
    Instant createdAt,
    Instant expiresAt
) {
    public ClaimToken {
        requireNonNull(remittanceId, "remittanceId cannot be null");
        requireNonNull(token, "token cannot be null");
    }
}
