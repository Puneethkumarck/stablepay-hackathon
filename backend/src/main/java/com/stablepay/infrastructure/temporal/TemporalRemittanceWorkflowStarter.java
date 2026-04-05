package com.stablepay.infrastructure.temporal;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.stablepay.application.config.StablePayProperties;
import com.stablepay.domain.remittance.port.RemittanceWorkflowStarter;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "stablepay.temporal",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class TemporalRemittanceWorkflowStarter implements RemittanceWorkflowStarter {

    private final WorkflowClient workflowClient;
    private final StablePayProperties properties;

    @Override
    public void startWorkflow(UUID remittanceId, String senderAddress, String recipientPhone,
            BigDecimal amountUsdc, String claimToken) {
        var options = WorkflowOptions.newBuilder()
                .setWorkflowId(RemittanceLifecycleWorkflow.workflowId(remittanceId))
                .setTaskQueue(properties.temporal().taskQueue())
                .build();

        var workflow = workflowClient.newWorkflowStub(RemittanceLifecycleWorkflow.class, options);

        var request = RemittanceWorkflowRequest.builder()
                .remittanceId(remittanceId)
                .senderAddress(senderAddress)
                .recipientPhone(recipientPhone)
                .amountUsdc(amountUsdc)
                .claimToken(claimToken)
                .build();

        WorkflowClient.start(workflow::execute, request);
        log.info("Started Temporal workflow for remittanceId={}", remittanceId);
    }
}
