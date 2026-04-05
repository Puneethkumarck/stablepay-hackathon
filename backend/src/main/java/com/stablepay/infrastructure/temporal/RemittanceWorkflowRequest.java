package com.stablepay.infrastructure.temporal;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record RemittanceWorkflowRequest(
    UUID remittanceId,
    String senderAddress,
    String recipientPhone,
    BigDecimal amountUsdc,
    String claimToken
) {}
