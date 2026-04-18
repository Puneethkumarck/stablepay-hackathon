package com.stablepay.infrastructure.temporal;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record WalletFundingWorkflowRequest(
    UUID fundingId,
    Long walletId,
    String senderSolanaAddress,
    BigDecimal amountUsdc
) {
    public WalletFundingWorkflowRequest {
        requireNonNull(fundingId, "fundingId cannot be null");
        requireNonNull(walletId, "walletId cannot be null");
        requireNonNull(senderSolanaAddress, "senderSolanaAddress cannot be null");
        requireNonNull(amountUsdc, "amountUsdc cannot be null");
    }
}
