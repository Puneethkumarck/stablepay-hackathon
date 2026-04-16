package com.stablepay.infrastructure.temporal;

import static com.stablepay.testutil.SolanaFixtures.SOME_CLAIM_AUTHORITY_PRIVATE_KEY;
import static com.stablepay.testutil.WorkflowFixtures.SOME_CLAIM_TOKEN;
import static com.stablepay.testutil.WorkflowFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sol4k.PublicKey;

import com.stablepay.infrastructure.solana.SolanaProperties;

import io.temporal.client.WorkflowClient;

@ExtendWith(MockitoExtension.class)
class TemporalRemittanceClaimSignalerTest {

    @Mock
    private WorkflowClient workflowClient;

    @Mock
    private RemittanceLifecycleWorkflow workflowStub;

    @Captor
    private ArgumentCaptor<ClaimSignal> signalCaptor;

    @Test
    void shouldSignalClaimWithCorrectWorkflowIdAndSignal() {
        // given
        var solanaProperties = new SolanaProperties(
                new PublicKey("EscrowProgram111111111111111111111111111111"),
                new PublicKey("4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"),
                SOME_CLAIM_AUTHORITY_PRIVATE_KEY,
                "http://localhost:8899");
        var signaler = new TemporalRemittanceClaimSignaler(workflowClient, solanaProperties);

        var expectedWorkflowId = RemittanceLifecycleWorkflow.workflowId(SOME_REMITTANCE_ID);
        given(workflowClient.newWorkflowStub(RemittanceLifecycleWorkflow.class, expectedWorkflowId))
                .willReturn(workflowStub);

        // when
        signaler.signalClaim(SOME_REMITTANCE_ID, SOME_CLAIM_TOKEN, SOME_UPI_ID);

        // then
        then(workflowClient).should().newWorkflowStub(RemittanceLifecycleWorkflow.class, expectedWorkflowId);
        then(workflowStub).should().claimSubmitted(signalCaptor.capture());

        var usdcMint = new PublicKey("4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU");
        var claimAuthorityPubkey = org.sol4k.Keypair.fromSecretKey(
                org.sol4k.Base58.decode(SOME_CLAIM_AUTHORITY_PRIVATE_KEY)).getPublicKey();
        var expectedAta = PublicKey.findProgramDerivedAddress(claimAuthorityPubkey, usdcMint)
                .getPublicKey().toBase58();
        var expected = ClaimSignal.builder()
                .claimToken(SOME_CLAIM_TOKEN)
                .upiId(SOME_UPI_ID)
                .destinationAddress(expectedAta)
                .build();
        assertThat(signalCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenClaimAuthorityNotConfigured() {
        // given
        var solanaProperties = new SolanaProperties(
                new PublicKey("EscrowProgram111111111111111111111111111111"),
                new PublicKey("4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"),
                "",
                "http://localhost:8899");
        var signaler = new TemporalRemittanceClaimSignaler(workflowClient, solanaProperties);

        // when / then
        assertThatThrownBy(() -> signaler.signalClaim(SOME_REMITTANCE_ID, SOME_CLAIM_TOKEN, SOME_UPI_ID))
                .isInstanceOf(com.stablepay.domain.remittance.exception.SolanaTransactionException.class)
                .hasMessageContaining("SP-0014");
    }
}
