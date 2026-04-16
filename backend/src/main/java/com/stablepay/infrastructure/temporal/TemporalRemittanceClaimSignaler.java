package com.stablepay.infrastructure.temporal;

import java.util.UUID;

import org.sol4k.Base58;
import org.sol4k.Keypair;
import org.sol4k.PublicKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.exception.SolanaTransactionException;
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
        log.info("Signaling claim for workflowId={}", workflowId);

        var workflow = workflowClient.newWorkflowStub(
                RemittanceLifecycleWorkflow.class, workflowId);

        var destinationTokenAccount = resolveClaimDestinationAta();
        var signal = ClaimSignal.builder()
                .claimToken(claimToken)
                .upiId(upiId)
                .destinationAddress(destinationTokenAccount)
                .build();

        workflow.claimSubmitted(signal);
        log.info("Claim signal sent for workflowId={}", workflowId);
    }

    private String resolveClaimDestinationAta() {
        var privateKeyStr = solanaProperties.claimAuthorityPrivateKey();
        if (privateKeyStr == null || privateKeyStr.isBlank()) {
            throw SolanaTransactionException.claimAuthorityNotConfigured();
        }
        var claimAuthorityPubkey = Keypair.fromSecretKey(Base58.decode(privateKeyStr)).getPublicKey();
        var ata = PublicKey.findProgramDerivedAddress(claimAuthorityPubkey, solanaProperties.usdcMint());
        log.info("Resolved claim authority ATA: {} (owner: {})", ata.getPublicKey().toBase58(),
                claimAuthorityPubkey.toBase58());
        return ata.getPublicKey().toBase58();
    }
}
