package com.stablepay.domain.remittance.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record RemittanceStatusEvent(
    Long id,
    UUID remittanceId,
    RemittanceStatus status,
    String message,
    Instant createdAt
) {}
