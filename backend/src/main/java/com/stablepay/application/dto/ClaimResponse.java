package com.stablepay.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.stablepay.domain.remittance.model.RemittanceStatus;

import lombok.Builder;

@Builder(toBuilder = true)
public record ClaimResponse(
    UUID remittanceId,
    String senderDisplayName,
    BigDecimal amountUsdc,
    BigDecimal amountInr,
    BigDecimal fxRate,
    RemittanceStatus status,
    boolean claimed,
    Instant expiresAt
) {}
