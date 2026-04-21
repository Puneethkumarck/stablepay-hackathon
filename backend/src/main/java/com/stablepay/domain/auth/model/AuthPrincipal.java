package com.stablepay.domain.auth.model;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record AuthPrincipal(UUID id) {
    public AuthPrincipal {
        requireNonNull(id, "id cannot be null");
    }
}
