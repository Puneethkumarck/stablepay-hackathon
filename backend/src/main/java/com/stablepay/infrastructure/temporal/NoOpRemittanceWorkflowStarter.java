package com.stablepay.infrastructure.temporal;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.port.RemittanceWorkflowStarter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "stablepay.temporal", name = "enabled", havingValue = "false")
public class NoOpRemittanceWorkflowStarter implements RemittanceWorkflowStarter {

    @Override
    public void startWorkflow(UUID remittanceId, String senderAddress, String recipientPhone,
            BigDecimal amountUsdc, String claimToken) {
        log.warn("Temporal disabled — skipping workflow for remittanceId={}", remittanceId);
    }
}
