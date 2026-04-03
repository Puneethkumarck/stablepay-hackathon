package com.stablepay.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record ClaimToken(
    Long id,
    UUID remittanceId,
    String token,
    boolean claimed,
    Instant createdAt
) {}
