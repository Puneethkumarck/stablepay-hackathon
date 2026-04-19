package com.stablepay.domain.remittance.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface RemittanceWorkflowStarter {
    void startWorkflow(UUID remittanceId, String senderAddress, String recipientPhone,
            BigDecimal amountUsdc, BigDecimal amountInr, String claimToken);
}
