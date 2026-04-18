package com.stablepay.infrastructure.temporal;

import java.math.BigDecimal;
import java.util.UUID;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface WalletFundingActivities {

    void checkTreasuryBalance(BigDecimal amountUsdc);

    void ensureSolBalance(String senderSolanaAddress);

    void createAtaIfNeeded(String senderSolanaAddress);

    String transferUsdc(String senderSolanaAddress, BigDecimal amountUsdc);

    void finalizeFunding(UUID fundingId, Long walletId, BigDecimal amountUsdc);
}
