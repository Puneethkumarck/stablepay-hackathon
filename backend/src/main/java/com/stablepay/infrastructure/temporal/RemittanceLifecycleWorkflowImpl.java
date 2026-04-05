package com.stablepay.infrastructure.temporal;

import java.time.Duration;

import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemittanceLifecycleWorkflowImpl implements RemittanceLifecycleWorkflow {

    private final RemittanceLifecycleActivities activities =
            Workflow.newActivityStub(RemittanceLifecycleActivities.class);

    private String currentStatus = "INITIATED";
    private String escrowPda;
    private boolean smsNotificationFailed;
    private boolean claimReceived;
    private ClaimSignal pendingClaim;

    @Override
    public RemittanceWorkflowResult execute(RemittanceWorkflowRequest request) {
        log.info("Starting remittance workflow for remittanceId={}", request.remittanceId());

        // Phase 1: Escrow deposit (sign + submit)
        // Activities will be wired in STA-31
        activities.updateRemittanceStatus(request.remittanceId().toString(), "ESCROWED");
        currentStatus = "ESCROWED";

        // Phase 2: Send claim SMS
        try {
            var claimUrl = "https://claim.stablepay.app/" + request.claimToken();
            activities.sendClaimSms(request.recipientPhone(), claimUrl);
        } catch (Exception e) {
            smsNotificationFailed = true;
            log.warn("SMS notification failed for remittanceId={}", request.remittanceId(), e);
        }

        // Phase 3: Wait for claim signal or 48h expiry
        var claimed = Workflow.await(Duration.ofHours(48), () -> claimReceived);

        if (claimed && pendingClaim != null) {
            // Phase 4: Process claim (sign release + submit + disburse)
            activities.updateRemittanceStatus(request.remittanceId().toString(), "CLAIMED");
            currentStatus = "CLAIMED";

            activities.simulateInrDisbursement(pendingClaim.upiId(), "0");
            activities.updateRemittanceStatus(request.remittanceId().toString(), "DELIVERED");
            currentStatus = "DELIVERED";

            return RemittanceWorkflowResult.builder()
                    .remittanceId(request.remittanceId())
                    .finalStatus("DELIVERED")
                    .escrowPda(escrowPda)
                    .build();
        } else {
            // Timeout: refund
            activities.updateRemittanceStatus(request.remittanceId().toString(), "REFUNDED");
            currentStatus = "REFUNDED";

            return RemittanceWorkflowResult.builder()
                    .remittanceId(request.remittanceId())
                    .finalStatus("REFUNDED")
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
                .currentStatus(currentStatus)
                .escrowPda(escrowPda)
                .smsNotificationFailed(smsNotificationFailed)
                .build();
    }
}
