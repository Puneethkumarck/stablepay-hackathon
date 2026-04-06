package com.stablepay.infrastructure.temporal;

import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record RemittanceWorkflowResult(
    UUID remittanceId,
    String finalStatus,
    String escrowPda,
    String txSignature
) {}
