package com.stablepay.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.stablepay.domain.remittance.model.RemittanceStatus;

import lombok.Builder;

@Builder(toBuilder = true)
public record RemittanceResponse(
    Long id,
    UUID remittanceId,
    UUID senderId,
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
