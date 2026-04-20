package com.stablepay.domain.auth.model;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record AppUser(
    UUID id,
    String email,
    Instant createdAt
) {
    public AppUser {
        requireNonNull(id, "id cannot be null");
        requireNonNull(email, "email cannot be null");
    }
}
