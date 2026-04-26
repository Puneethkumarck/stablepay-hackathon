package com.stablepay.infrastructure.temporal;

import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record RemittanceWorkflowStatus(
    UUID remittanceId,
    String currentStatus,
    String depositTxSignature,
    boolean smsNotificationFailed
) {}
