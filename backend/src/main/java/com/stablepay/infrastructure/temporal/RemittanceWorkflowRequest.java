package com.stablepay.infrastructure.temporal;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record RemittanceWorkflowRequest(
    UUID remittanceId,
    String senderAddress,
    String recipientPhone,
    BigDecimal amountUsdc,
    BigDecimal amountInr,
    String claimToken,
    String claimBaseUrl,
    Duration claimExpiryTimeout,
    long escrowExpiryTimestamp
) {
    public RemittanceWorkflowRequest {
        requireNonNull(amountInr, "amountInr must not be null");
    }
}
