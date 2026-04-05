package com.stablepay.infrastructure.solana;

import static com.stablepay.testutil.SolanaFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.SolanaFixtures.SOME_DESTINATION_TOKEN_ACCOUNT;
import static com.stablepay.testutil.SolanaFixtures.SOME_EXPIRY_TIMESTAMP;
import static com.stablepay.testutil.SolanaFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.SolanaFixtures.SOME_SENDER_WALLET;
import static com.stablepay.testutil.SolanaFixtures.SOME_TRANSACTION_SIGNATURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sol4k.Connection;
import org.sol4k.PublicKey;

import com.stablepay.domain.remittance.exception.SolanaTransactionException;

@ExtendWith(MockitoExtension.class)
class SolanaTransactionServiceAdapterTest {

    @Mock
    private Connection solanaConnection;

    @Mock
    private EscrowInstructionBuilder escrowInstructionBuilder;

    private SolanaProperties solanaProperties;

    private SolanaTransactionServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        solanaProperties = new SolanaProperties(
                new PublicKey("EscrowProgram111111111111111111111111111111"),
                new PublicKey("4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"),
                "");
        adapter = new SolanaTransactionServiceAdapter(
                solanaConnection, escrowInstructionBuilder, solanaProperties);
    }

    @Test
    void shouldReturnConfirmedForTransactionStatusStub() {
        // given
        var signature = SOME_TRANSACTION_SIGNATURE;

        // when
        var result = adapter.getTransactionStatus(signature);

        // then
        assertThat(result).isEqualTo("CONFIRMED");
    }

    @Test
    void shouldThrowSolanaTransactionExceptionWhenDepositBuildFails() {
        // given
        var senderWallet = new PublicKey(SOME_SENDER_WALLET);
        given(escrowInstructionBuilder.buildDepositInstruction(
                SOME_REMITTANCE_ID,
                senderWallet,
                senderWallet,
                SOME_AMOUNT_USDC,
                SOME_EXPIRY_TIMESTAMP))
                .willThrow(new RuntimeException("Build failed"));

        // when / then
        assertThatThrownBy(() -> adapter.depositEscrow(
                SOME_REMITTANCE_ID, SOME_SENDER_WALLET,
                SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP))
                .isInstanceOf(SolanaTransactionException.class)
                .hasMessageContaining("SP-0010");
    }

    @Test
    void shouldThrowSolanaTransactionExceptionWhenClaimBuildFails() {
        // given — adapter has empty claim authority key, generates ephemeral keypair
        // The connection call will fail because no instruction was built

        // when / then
        assertThatThrownBy(() -> adapter.claimEscrow(
                SOME_REMITTANCE_ID, SOME_DESTINATION_TOKEN_ACCOUNT))
                .isInstanceOf(SolanaTransactionException.class);
    }

    @Test
    void shouldThrowSolanaTransactionExceptionWhenRefundBuildFails() {
        // given — adapter has empty claim authority key, generates ephemeral keypair
        // The connection call will fail because no instruction was built

        // when / then
        assertThatThrownBy(() -> adapter.refundEscrow(SOME_REMITTANCE_ID))
                .isInstanceOf(SolanaTransactionException.class);
    }
}
