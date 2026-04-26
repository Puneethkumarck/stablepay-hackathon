package com.stablepay.infrastructure.temporal;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;

import com.stablepay.domain.remittance.exception.DisbursementException;
import com.stablepay.domain.remittance.exception.SolanaTransactionException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.TransactionConfirmationStatus;
import com.stablepay.infrastructure.temporal.TaskQueue.Constants;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;

@WorkflowImpl(taskQueues = Constants.TASK_QUEUE_REMITTANCE_LIFECYCLE)
public class RemittanceLifecycleWorkflowImpl implements RemittanceLifecycleWorkflow {

    private static final Logger log = Workflow.getLogger(RemittanceLifecycleWorkflowImpl.class);

    private static final Duration SOLANA_ACTIVITY_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration DISBURSEMENT_ACTIVITY_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration SMS_ACTIVITY_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration STATUS_UPDATE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration TX_CONFIRMATION_ACTIVITY_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration TX_CONFIRMATION_POLL_INTERVAL = Duration.ofSeconds(3);
    private static final Duration TX_CONFIRMATION_TIMEOUT = Duration.ofMinutes(2);
    private static final int SOLANA_MAX_ATTEMPTS = 3;
    private static final int SMS_MAX_ATTEMPTS = 3;
    private static final int DISBURSEMENT_MAX_ATTEMPTS = 3;
    private static final String DISBURSEMENT_NON_RETRIABLE_TYPE =
            DisbursementException.NonRetriable.class.getName();

    private final RemittanceLifecycleActivities solanaActivities =
            Workflow.newActivityStub(RemittanceLifecycleActivities.class, solanaActivityOptions());

    private final RemittanceLifecycleActivities disbursementActivities =
            Workflow.newActivityStub(RemittanceLifecycleActivities.class, disbursementActivityOptions());

    private final RemittanceLifecycleActivities smsActivities =
            Workflow.newActivityStub(RemittanceLifecycleActivities.class, smsActivityOptions());

    private final RemittanceLifecycleActivities statusActivities =
            Workflow.newActivityStub(RemittanceLifecycleActivities.class, statusUpdateActivityOptions());

    private final RemittanceLifecycleActivities confirmationActivities =
            Workflow.newActivityStub(RemittanceLifecycleActivities.class, confirmationActivityOptions());

    private UUID remittanceId;
    private RemittanceStatus currentStatus = RemittanceStatus.INITIATED;
    private String escrowTxSignature;
    private boolean smsNotificationFailed;
    private boolean claimReceived;
    private boolean refundInitiated;
    private ClaimSignal pendingClaim;

    @Override
    public RemittanceWorkflowResult execute(RemittanceWorkflowRequest request) {
        this.remittanceId = request.remittanceId();
        log.info("Starting remittance lifecycle workflow for remittanceId={}", remittanceId);

        var depositSignature = depositEscrow(request);
        this.escrowTxSignature = depositSignature;

        try {
            awaitTransactionConfirmation(depositSignature);
        } catch (SolanaTransactionException e) {
            log.error("Deposit confirmation failed for remittance {}", remittanceId, e);
            statusActivities.updateRemittanceStatus(
                    remittanceId.toString(), RemittanceStatus.DEPOSIT_FAILED);
            currentStatus = RemittanceStatus.DEPOSIT_FAILED;
            return failedResult(RemittanceStatus.DEPOSIT_FAILED, depositSignature);
        }

        var escrowPdaAddress = statusActivities.deriveEscrowPda(remittanceId.toString());
        statusActivities.updateRemittanceStatusWithEscrowPda(
                remittanceId.toString(), RemittanceStatus.ESCROWED, escrowPdaAddress);
        currentStatus = RemittanceStatus.ESCROWED;
        log.info("Remittance {} escrowed with tx={}", remittanceId, depositSignature);

        sendClaimNotification(request);

        var claimed = Workflow.await(request.claimExpiryTimeout(), () -> claimReceived);

        if (claimed && pendingClaim != null) {
            return processClaim(request);
        }

        return processExpiry(request);
    }

    @Override
    public void claimSubmitted(ClaimSignal claimSignal) {
        log.info("Claim signal received for remittanceId={}, claimToken={}",
                remittanceId, claimSignal.claimToken());
        this.pendingClaim = claimSignal;
        this.claimReceived = true;
    }

    @Override
    public RemittanceWorkflowStatus getStatus() {
        return RemittanceWorkflowStatus.builder()
                .remittanceId(remittanceId)
                .currentStatus(currentStatus != null ? currentStatus.name() : null)
                .depositTxSignature(escrowTxSignature)
                .smsNotificationFailed(smsNotificationFailed)
                .build();
    }

    private String depositEscrow(RemittanceWorkflowRequest request) {
        log.info("Depositing escrow for remittanceId={}", remittanceId);
        return solanaActivities.depositEscrow(
                remittanceId.toString(),
                request.senderAddress(),
                request.amountUsdc(),
                request.escrowExpiryTimestamp());
    }

    private void sendClaimNotification(RemittanceWorkflowRequest request) {
        try {
            var claimUrl = request.claimBaseUrl() + request.claimToken();
            smsActivities.sendClaimSms(request.recipientPhone(), claimUrl);
            log.info("Claim SMS sent for remittanceId={}", remittanceId);
        } catch (Exception e) {
            smsNotificationFailed = true;
            log.warn("SMS notification failed for remittanceId={}, continuing workflow", remittanceId, e);
        }
    }

    private RemittanceWorkflowResult processClaim(RemittanceWorkflowRequest request) {
        log.info("Processing claim for remittanceId={}", remittanceId);

        var releaseSignature = solanaActivities.releaseEscrow(
                remittanceId.toString(),
                pendingClaim.destinationAddress(),
                request.senderAddress());

        try {
            awaitTransactionConfirmation(releaseSignature);
        } catch (SolanaTransactionException e) {
            log.error("SP-0031: CRITICAL — release confirmation failed for remittance {},"
                    + " escrow may be released on-chain", remittanceId, e);
            statusActivities.updateRemittanceStatus(
                    remittanceId.toString(), RemittanceStatus.CLAIM_FAILED);
            currentStatus = RemittanceStatus.CLAIM_FAILED;
            return failedResult(RemittanceStatus.CLAIM_FAILED, releaseSignature);
        }

        statusActivities.updateRemittanceStatus(remittanceId.toString(), RemittanceStatus.CLAIMED);
        currentStatus = RemittanceStatus.CLAIMED;
        log.info("Escrow released for remittanceId={} with tx={}", remittanceId, releaseSignature);

        try {
            disbursementActivities.disburseInr(
                    pendingClaim.upiId(),
                    request.amountUsdc(),
                    request.amountInr(),
                    remittanceId.toString());
        } catch (Exception e) {
            log.error("INR disbursement failed for remittanceId={}, escrow already released", remittanceId, e);
            statusActivities.updateRemittanceStatus(
                    remittanceId.toString(), RemittanceStatus.DISBURSEMENT_FAILED);
            currentStatus = RemittanceStatus.DISBURSEMENT_FAILED;
            return RemittanceWorkflowResult.builder()
                    .remittanceId(remittanceId)
                    .finalStatus(RemittanceStatus.DISBURSEMENT_FAILED.name())
                    .depositTxSignature(escrowTxSignature)
                    .txSignature(releaseSignature)
                    .build();
        }

        statusActivities.updateRemittanceStatus(remittanceId.toString(), RemittanceStatus.DELIVERED);
        currentStatus = RemittanceStatus.DELIVERED;
        log.info("Remittance {} delivered successfully", remittanceId);

        return RemittanceWorkflowResult.builder()
                .remittanceId(remittanceId)
                .finalStatus(RemittanceStatus.DELIVERED.name())
                .depositTxSignature(escrowTxSignature)
                .txSignature(releaseSignature)
                .build();
    }

    private RemittanceWorkflowResult processExpiry(RemittanceWorkflowRequest request) {
        log.info("Claim expiry timeout reached for remittanceId={}, checking claim flag", remittanceId);

        if (claimReceived && pendingClaim != null) {
            log.info("Claim received during timeout check for remittanceId={}, processing claim", remittanceId);
            return processClaim(request);
        }

        refundInitiated = true;
        log.info("No claim received for remittanceId={}, initiating refund", remittanceId);

        var refundSignature = solanaActivities.refundEscrow(
                remittanceId.toString(),
                request.senderAddress());

        try {
            awaitTransactionConfirmation(refundSignature);
        } catch (SolanaTransactionException e) {
            log.error("Refund confirmation failed for remittance {}", remittanceId, e);
            statusActivities.updateRemittanceStatus(
                    remittanceId.toString(), RemittanceStatus.REFUND_FAILED);
            currentStatus = RemittanceStatus.REFUND_FAILED;
            return failedResult(RemittanceStatus.REFUND_FAILED, refundSignature);
        }

        statusActivities.updateRemittanceStatus(remittanceId.toString(), RemittanceStatus.REFUNDED);
        currentStatus = RemittanceStatus.REFUNDED;
        log.info("Remittance {} refunded with tx={}", remittanceId, refundSignature);

        return RemittanceWorkflowResult.builder()
                .remittanceId(remittanceId)
                .finalStatus(RemittanceStatus.REFUNDED.name())
                .depositTxSignature(escrowTxSignature)
                .txSignature(refundSignature)
                .build();
    }

    private static ActivityOptions solanaActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(SOLANA_ACTIVITY_TIMEOUT)
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(SOLANA_MAX_ATTEMPTS)
                        .setInitialInterval(Duration.ofSeconds(2))
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();
    }

    static ActivityOptions disbursementActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(DISBURSEMENT_ACTIVITY_TIMEOUT)
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(DISBURSEMENT_MAX_ATTEMPTS)
                        .setInitialInterval(Duration.ofSeconds(2))
                        .setBackoffCoefficient(2.0)
                        .setDoNotRetry(DISBURSEMENT_NON_RETRIABLE_TYPE)
                        .build())
                .build();
    }

    private static ActivityOptions smsActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(SMS_ACTIVITY_TIMEOUT)
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(SMS_MAX_ATTEMPTS)
                        .setInitialInterval(Duration.ofSeconds(5))
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();
    }

    private static ActivityOptions statusUpdateActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(STATUS_UPDATE_TIMEOUT)
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(SOLANA_MAX_ATTEMPTS)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();
    }

    private static ActivityOptions confirmationActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(TX_CONFIRMATION_ACTIVITY_TIMEOUT)
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(SOLANA_MAX_ATTEMPTS)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setBackoffCoefficient(1.5)
                        .setDoNotRetry(IllegalArgumentException.class.getName())
                        .build())
                .build();
    }

    private void awaitTransactionConfirmation(String signature) {
        var deadline = Workflow.currentTimeMillis() + TX_CONFIRMATION_TIMEOUT.toMillis();
        var status = TransactionConfirmationStatus.NOT_FOUND;

        while (!status.isTerminal()) {
            if (Workflow.currentTimeMillis() >= deadline) {
                throw SolanaTransactionException.confirmationTimeout(signature);
            }

            status = confirmationActivities.checkTransactionStatus(signature);

            switch (status) {
                case CONFIRMED, FINALIZED ->
                    log.info("Transaction {} confirmed (status={})", signature, status);
                case FAILED_ON_CHAIN ->
                    throw SolanaTransactionException.transactionFailed(signature);
                default -> {
                    log.info("Transaction {} pending (status={})", signature, status);
                    Workflow.sleep(TX_CONFIRMATION_POLL_INTERVAL);
                }
            }
        }
    }

    private RemittanceWorkflowResult failedResult(RemittanceStatus status, String txSignature) {
        return RemittanceWorkflowResult.builder()
                .remittanceId(remittanceId)
                .finalStatus(status.name())
                .depositTxSignature(escrowTxSignature)
                .txSignature(txSignature)
                .build();
    }
}
