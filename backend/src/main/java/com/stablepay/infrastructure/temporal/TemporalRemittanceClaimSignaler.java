package com.stablepay.infrastructure.temporal;

import java.util.UUID;

import org.sol4k.Base58;
import org.sol4k.Keypair;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.port.RemittanceClaimSignaler;
import com.stablepay.infrastructure.solana.SolanaProperties;

import io.temporal.client.WorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(WorkflowClient.class)
public class TemporalRemittanceClaimSignaler implements RemittanceClaimSignaler {

    private final WorkflowClient workflowClient;
    private final SolanaProperties solanaProperties;

    @Override
    public void signalClaim(UUID remittanceId, String claimToken, String upiId) {
        var workflowId = RemittanceLifecycleWorkflow.workflowId(remittanceId);
        log.info("Signaling claim for workflowId={}, claimToken={}", workflowId, claimToken);

        var workflow = workflowClient.newWorkflowStub(
                RemittanceLifecycleWorkflow.class, workflowId);

        var destinationAddress = resolveClaimDestination();
        var signal = ClaimSignal.builder()
                .claimToken(claimToken)
                .upiId(upiId)
                .destinationAddress(destinationAddress)
                .build();

        workflow.claimSubmitted(signal);
        log.info("Claim signal sent for workflowId={}", workflowId);
    }

    private String resolveClaimDestination() {
        var privateKeyStr = solanaProperties.claimAuthorityPrivateKey();
        if (privateKeyStr == null || privateKeyStr.isBlank()) {
            return "";
        }
        return Keypair.fromSecretKey(Base58.decode(privateKeyStr))
                .getPublicKey().toBase58();
    }
}
