package com.stablepay.infrastructure.temporal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.stablepay.application.config.StablePayProperties;
import com.stablepay.domain.remittance.port.RemittanceWorkflowStarter;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(WorkflowClient.class)
public class TemporalRemittanceWorkflowStarter implements RemittanceWorkflowStarter {

    private final WorkflowFactory workflowFactory;
    private final StablePayProperties properties;

    @Override
    public void startWorkflow(
            UUID remittanceId,
            String senderAddress,
            String recipientPhone,
            BigDecimal amountUsdc,
            String claimToken) {
        var temporal = properties.temporal();
        var workflow = workflowFactory.createRemittanceWorkflow(remittanceId);

        var escrowExpiryTimestamp = Instant.now()
                .plus(temporal.claimExpiryTimeout())
                .getEpochSecond();

        var request = RemittanceWorkflowRequest.builder()
                .remittanceId(remittanceId)
                .senderAddress(senderAddress)
                .recipientPhone(recipientPhone)
                .amountUsdc(amountUsdc)
                .claimToken(claimToken)
                .claimBaseUrl(temporal.claimBaseUrl())
                .claimExpiryTimeout(temporal.claimExpiryTimeout())
                .escrowExpiryTimestamp(escrowExpiryTimestamp)
                .build();

        try {
            WorkflowClient.start(workflow::execute, request);
            log.info("Started Temporal workflow for remittanceId={}", remittanceId);
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.warn(
                    "Workflow already started for remittanceId={}. Skipping.",
                    remittanceId,
                    e);
        }
    }
}
