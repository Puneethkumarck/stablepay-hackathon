package com.stablepay.infrastructure.temporal;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;

import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.infrastructure.temporal.TaskQueue.Constants;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;

@WorkflowImpl(taskQueues = Constants.TASK_QUEUE_REMITTANCE_LIFECYCLE)
public class RemittanceLifecycleWorkflowImpl implements RemittanceLifecycleWorkflow {

    private static final Logger log = Workflow.getLogger(RemittanceLifecycleWorkflowImpl.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final RemittanceLifecycleActivities activities =
            Workflow.newActivityStub(RemittanceLifecycleActivities.class, defaultActivityOptions());

    private UUID remittanceId;
    private RemittanceStatus currentStatus = RemittanceStatus.INITIATED;
    private String escrowPda;
    private boolean smsNotificationFailed;
    private boolean claimReceived;
    private ClaimSignal pendingClaim;

    @Override
    public RemittanceWorkflowResult execute(RemittanceWorkflowRequest request) {
        this.remittanceId = request.remittanceId();
        log.info("Starting remittance workflow for remittanceId={}", remittanceId);

        activities.updateRemittanceStatus(remittanceId.toString(), RemittanceStatus.ESCROWED);
        currentStatus = RemittanceStatus.ESCROWED;

        try {
            var claimUrl = request.claimBaseUrl() + request.claimToken();
            activities.sendClaimSms(request.recipientPhone(), claimUrl);
        } catch (Exception e) {
            smsNotificationFailed = true;
            log.warn("SMS notification failed for remittanceId={}", remittanceId, e);
        }

        var claimed = Workflow.await(request.claimExpiryTimeout(), () -> claimReceived);

        if (claimed && pendingClaim != null) {
            activities.updateRemittanceStatus(remittanceId.toString(), RemittanceStatus.CLAIMED);
            currentStatus = RemittanceStatus.CLAIMED;

            activities.simulateInrDisbursement(pendingClaim.upiId(), request.amountUsdc().toPlainString());
            activities.updateRemittanceStatus(remittanceId.toString(), RemittanceStatus.DELIVERED);
            currentStatus = RemittanceStatus.DELIVERED;

            return RemittanceWorkflowResult.builder()
                    .remittanceId(remittanceId)
                    .finalStatus(RemittanceStatus.DELIVERED.name())
                    .escrowPda(escrowPda)
                    .build();
        } else {
            activities.updateRemittanceStatus(remittanceId.toString(), RemittanceStatus.REFUNDED);
            currentStatus = RemittanceStatus.REFUNDED;

            return RemittanceWorkflowResult.builder()
                    .remittanceId(remittanceId)
                    .finalStatus(RemittanceStatus.REFUNDED.name())
                    .escrowPda(escrowPda)
                    .build();
        }
    }

    @Override
    public void claimSubmitted(ClaimSignal claimSignal) {
        log.info("Claim signal received for claimToken={}", claimSignal.claimToken());
        this.pendingClaim = claimSignal;
        this.claimReceived = true;
    }

    @Override
    public RemittanceWorkflowStatus getStatus() {
        return RemittanceWorkflowStatus.builder()
                .remittanceId(remittanceId)
                .currentStatus(currentStatus != null ? currentStatus.name() : null)
                .escrowPda(escrowPda)
                .smsNotificationFailed(smsNotificationFailed)
                .build();
    }

    private static ActivityOptions defaultActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(DEFAULT_TIMEOUT)
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(DEFAULT_MAX_ATTEMPTS)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();
    }
}
