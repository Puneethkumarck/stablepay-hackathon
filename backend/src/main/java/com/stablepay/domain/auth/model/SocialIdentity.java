package com.stablepay.domain.auth.model;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record SocialIdentity(
    UUID userId,
    String provider,
    String subject,
    String email,
    boolean emailVerified,
    String name
) {
    public SocialIdentity {
        requireNonNull(provider, "provider cannot be null");
        requireNonNull(subject, "subject cannot be null");
        requireNonNull(email, "email cannot be null");
    }
}
