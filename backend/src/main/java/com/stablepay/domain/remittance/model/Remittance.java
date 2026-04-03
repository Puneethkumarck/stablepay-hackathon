package com.stablepay.domain.remittance.model;

import static java.util.Objects.requireNonNull;

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
) {
    public Remittance {
        requireNonNull(remittanceId, "remittanceId cannot be null");
        requireNonNull(senderId, "senderId cannot be null");
        requireNonNull(recipientPhone, "recipientPhone cannot be null");
        requireNonNull(amountUsdc, "amountUsdc cannot be null");
        requireNonNull(status, "status cannot be null");
    }
}
