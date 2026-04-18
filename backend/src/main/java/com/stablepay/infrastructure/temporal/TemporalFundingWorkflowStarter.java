package com.stablepay.infrastructure.temporal;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.stablepay.domain.funding.port.FundingWorkflowStarter;

import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(WorkflowClient.class)
public class TemporalFundingWorkflowStarter implements FundingWorkflowStarter {

    private final WorkflowClient workflowClient;

    // Deferring the RPC until afterCommit prevents a dual-write hazard: if the caller's
    // transaction rolls back after this method returned, the workflow would otherwise
    // execute against funding-order state that was never persisted. Outside a transaction
    // (startup hooks, tests) we fire immediately.
    @Override
    public void startFundingWorkflow(
            UUID fundingId, Long walletId, String senderSolanaAddress, BigDecimal amountUsdc) {
        Runnable startCall = () -> start(fundingId, walletId, senderSolanaAddress, amountUsdc);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    startCall.run();
                }
            });
        } else {
            startCall.run();
        }
    }

    private void start(
            UUID fundingId, Long walletId, String senderSolanaAddress, BigDecimal amountUsdc) {
        var workflow = workflowClient.newWorkflowStub(
                WalletFundingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TaskQueue.WALLET_FUNDING.getName())
                        .setWorkflowId(WalletFundingWorkflow.workflowId(fundingId))
                        .setWorkflowIdReusePolicy(
                                WorkflowIdReusePolicy
                                        .WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                        .build());

        var request = WalletFundingWorkflowRequest.builder()
                .fundingId(fundingId)
                .walletId(walletId)
                .senderSolanaAddress(senderSolanaAddress)
                .amountUsdc(amountUsdc)
                .build();

        try {
            WorkflowClient.start(workflow::execute, request);
            log.info("Started wallet funding workflow fundingId={} walletId={}", fundingId, walletId);
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.warn(
                    "Wallet funding workflow already started for fundingId={}. Skipping.",
                    fundingId,
                    e);
        } catch (RuntimeException e) {
            // FundingOrder stays in PAYMENT_CONFIRMED (the FUNDED flip now happens inside
            // the workflow's finalizeFunding activity, per spec appendix #25). A Stripe
            // retry will re-enter CompleteFundingHandler and retry the start. Log loudly
            // so operators can investigate if retries don't arrive.
            // Follow-up: STA-84 adds an outbox + reconciler to recover automatically.
            log.error(
                    "wallet funding workflow start FAILED fundingId={} walletId={} — "
                            + "relying on Stripe webhook retry",
                    fundingId, walletId, e);
        }
    }
}
