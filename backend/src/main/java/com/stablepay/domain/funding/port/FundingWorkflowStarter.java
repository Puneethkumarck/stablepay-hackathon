package com.stablepay.domain.funding.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface FundingWorkflowStarter {
    void startFundingWorkflow(UUID fundingId, Long walletId, String senderSolanaAddress, BigDecimal amountUsdc);
}
