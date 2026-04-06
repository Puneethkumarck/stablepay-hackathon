package com.stablepay.infrastructure.solana;

import static com.stablepay.testutil.SolanaFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.SolanaFixtures.SOME_BLOCKHASH;
import static com.stablepay.testutil.SolanaFixtures.SOME_CLAIM_AUTHORITY_PRIVATE_KEY;
import static com.stablepay.testutil.SolanaFixtures.SOME_CLAIM_AUTHORITY_PUBLIC_KEY;
import static com.stablepay.testutil.SolanaFixtures.SOME_DESTINATION_TOKEN_ACCOUNT;
import static com.stablepay.testutil.SolanaFixtures.SOME_EXPIRY_TIMESTAMP;
import static com.stablepay.testutil.SolanaFixtures.SOME_PROGRAM_ID;
import static com.stablepay.testutil.SolanaFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.SolanaFixtures.SOME_SENDER_WALLET;
import static com.stablepay.testutil.SolanaFixtures.SOME_TRANSACTION_SIGNATURE;
import static com.stablepay.testutil.SolanaFixtures.SOME_USDC_MINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sol4k.AccountMeta;
import org.sol4k.Connection;
import org.sol4k.PublicKey;
import org.sol4k.VersionedTransaction;
import org.sol4k.instruction.BaseInstruction;

import com.stablepay.domain.remittance.exception.SolanaTransactionException;

@ExtendWith(MockitoExtension.class)
class SolanaTransactionServiceAdapterTest {

    @Mock
    private Connection solanaConnection;

    @Mock
    private EscrowInstructionBuilder escrowInstructionBuilder;

    private SolanaProperties propertiesWithKey;
    private SolanaProperties propertiesWithoutKey;

    @BeforeEach
    void setUp() {
        propertiesWithKey = new SolanaProperties(
                new PublicKey(SOME_PROGRAM_ID),
                new PublicKey(SOME_USDC_MINT),
                SOME_CLAIM_AUTHORITY_PRIVATE_KEY);
        propertiesWithoutKey = new SolanaProperties(
                new PublicKey(SOME_PROGRAM_ID),
                new PublicKey(SOME_USDC_MINT),
                "");
    }

    @Nested
    class GetTransactionStatus {

        @Test
        void shouldReturnConfirmedForTransactionStatusStub() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey);

            // when
            var result = adapter.getTransactionStatus(SOME_TRANSACTION_SIGNATURE);

            // then
            assertThat(result).isEqualTo("CONFIRMED");
        }
    }

    @Nested
    class ClaimEscrow {

        @Test
        void shouldSubmitClaimTransactionAndReturnSignature() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey);
            var claimAuthorityPubKey = new PublicKey(SOME_CLAIM_AUTHORITY_PUBLIC_KEY);
            var destination = new PublicKey(SOME_DESTINATION_TOKEN_ACCOUNT);
            var instruction = new BaseInstruction(
                    new byte[8], List.of(AccountMeta.signerAndWritable(claimAuthorityPubKey)),
                    new PublicKey(SOME_PROGRAM_ID));

            given(escrowInstructionBuilder.buildClaimInstruction(
                    SOME_REMITTANCE_ID, claimAuthorityPubKey, destination))
                    .willReturn(instruction);
            given(solanaConnection.getLatestBlockhash()).willReturn(SOME_BLOCKHASH);
            given(solanaConnection.sendTransaction(ArgumentMatchers.<VersionedTransaction>notNull()))
                    .willReturn(SOME_TRANSACTION_SIGNATURE);

            // when
            var result = adapter.claimEscrow(SOME_REMITTANCE_ID, SOME_DESTINATION_TOKEN_ACCOUNT);

            // then
            assertThat(result).isEqualTo(SOME_TRANSACTION_SIGNATURE);
            then(solanaConnection).should().sendTransaction(ArgumentMatchers.<VersionedTransaction>notNull());
        }

        @Test
        void shouldThrowWhenClaimAuthorityNotConfigured() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithoutKey);

            // when / then
            assertThatThrownBy(() -> adapter.claimEscrow(
                    SOME_REMITTANCE_ID, SOME_DESTINATION_TOKEN_ACCOUNT))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0014");
        }
    }

    @Nested
    class RefundEscrow {

        @Test
        void shouldSubmitRefundTransactionAndReturnSignature() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey);
            var claimAuthorityPubKey = new PublicKey(SOME_CLAIM_AUTHORITY_PUBLIC_KEY);
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            var instruction = new BaseInstruction(
                    new byte[8], List.of(AccountMeta.signerAndWritable(claimAuthorityPubKey)),
                    new PublicKey(SOME_PROGRAM_ID));

            given(escrowInstructionBuilder.buildRefundInstruction(
                    SOME_REMITTANCE_ID, claimAuthorityPubKey, senderWallet))
                    .willReturn(instruction);
            given(solanaConnection.getLatestBlockhash()).willReturn(SOME_BLOCKHASH);
            given(solanaConnection.sendTransaction(ArgumentMatchers.<VersionedTransaction>notNull()))
                    .willReturn(SOME_TRANSACTION_SIGNATURE);

            // when
            var result = adapter.refundEscrow(SOME_REMITTANCE_ID, SOME_SENDER_WALLET);

            // then
            assertThat(result).isEqualTo(SOME_TRANSACTION_SIGNATURE);
            then(solanaConnection).should().sendTransaction(ArgumentMatchers.<VersionedTransaction>notNull());
        }

        @Test
        void shouldThrowWhenClaimAuthorityNotConfiguredForRefund() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithoutKey);

            // when / then
            assertThatThrownBy(() -> adapter.refundEscrow(SOME_REMITTANCE_ID, SOME_SENDER_WALLET))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0014");
        }
    }

    @Nested
    class DepositEscrow {

        @Test
        void shouldThrowWhenClaimAuthorityNotConfiguredForDeposit() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithoutKey);

            // when / then
            assertThatThrownBy(() -> adapter.depositEscrow(
                    SOME_REMITTANCE_ID, SOME_SENDER_WALLET,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0014");
        }

        @Test
        void shouldThrowSolanaTransactionExceptionWhenDepositBuildFails() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey);
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            var claimAuthorityPubKey = new PublicKey(SOME_CLAIM_AUTHORITY_PUBLIC_KEY);

            given(escrowInstructionBuilder.buildDepositInstruction(
                    SOME_REMITTANCE_ID,
                    senderWallet,
                    claimAuthorityPubKey,
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
    }
}
