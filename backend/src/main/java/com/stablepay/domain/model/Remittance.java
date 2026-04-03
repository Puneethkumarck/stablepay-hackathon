package com.stablepay.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record Remittance(
    Long id,
    UUID remittanceId,
    String senderId,
    String recipientPhone,
    BigDecimal amountUsdc,
    BigDecimal amountInr,
    BigDecimal fxRate,
    RemittanceStatus status,
    String escrowPda,
    String claimTokenId,
    boolean smsNotificationFailed,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt
) {}
