package com.stablepay.infrastructure.temporal;

import java.time.Duration;

import org.slf4j.Logger;

import com.stablepay.infrastructure.temporal.TaskQueue.Constants;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;

@WorkflowImpl(taskQueues = Constants.TASK_QUEUE_WALLET_FUNDING)
public class WalletFundingWorkflowImpl implements WalletFundingWorkflow {

    private static final Logger log = Workflow.getLogger(WalletFundingWorkflowImpl.class);

    private final WalletFundingActivities treasuryCheckActivity =
            Workflow.newActivityStub(WalletFundingActivities.class, treasuryCheckOptions());
    private final WalletFundingActivities solTopUpActivity =
            Workflow.newActivityStub(WalletFundingActivities.class, solTopUpOptions());
    private final WalletFundingActivities ataActivity =
            Workflow.newActivityStub(WalletFundingActivities.class, ataOptions());
    private final WalletFundingActivities transferActivity =
            Workflow.newActivityStub(WalletFundingActivities.class, transferOptions());
    private final WalletFundingActivities finalizeActivity =
            Workflow.newActivityStub(WalletFundingActivities.class, finalizeOptions());

    @Override
    public void execute(WalletFundingWorkflowRequest request) {
        log.info("Starting wallet funding workflow fundingId={} walletId={}",
                request.fundingId(), request.walletId());

        treasuryCheckActivity.checkTreasuryBalance(request.amountUsdc());
        solTopUpActivity.ensureSolBalance(request.senderSolanaAddress());
        ataActivity.createAtaIfNeeded(request.senderSolanaAddress());
        var signature = transferActivity.transferUsdc(
                request.senderSolanaAddress(), request.amountUsdc());
        log.info("USDC transfer submitted fundingId={} signature={}",
                request.fundingId(), signature);
        finalizeActivity.finalizeFunding(
                request.fundingId(), request.walletId(), request.amountUsdc());

        log.info("Wallet funding workflow completed fundingId={}", request.fundingId());
    }

    // Treasury depletion is a permanent business failure — don't retry. Any
    // other exception (RPC transient) must retry up to 3 times so a flaky
    // Solana RPC call doesn't permanently fail an otherwise-fundable workflow.
    private static ActivityOptions treasuryCheckOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setBackoffCoefficient(2.0)
                        .setDoNotRetry(
                                "com.stablepay.domain.wallet.exception.TreasuryDepletedException")
                        .build())
                .build();
    }

    private static ActivityOptions solTopUpOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(standardRetry())
                .build();
    }

    private static ActivityOptions ataOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(standardRetry())
                .build();
    }

    // transferUsdc has no signature-persistence idempotency, so a timed-out
    // activity that actually succeeded on-chain could be retried and produce a
    // second USDC transfer. Until STA-84 adds signature persistence keyed by
    // fundingId, cap this activity at a single attempt and let the workflow
    // fail (operators re-drive). ensureSolBalance stays retryable because its
    // pre-check naturally short-circuits if the sender already holds SOL, and
    // createAtaIfNeeded stays retryable because its pre-check detects an
    // existing ATA.
    private static ActivityOptions transferOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(1)
                        .build())
                .build();
    }

    private static ActivityOptions finalizeOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(15))
                .setRetryOptions(standardRetry())
                .build();
    }

    private static RetryOptions standardRetry() {
        return RetryOptions.newBuilder()
                .setMaximumAttempts(3)
                .setInitialInterval(Duration.ofSeconds(2))
                .setBackoffCoefficient(2.0)
                .build();
    }
}
