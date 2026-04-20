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
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sol4k.AccountMeta;
import org.sol4k.Connection;
import org.sol4k.PublicKey;
import org.sol4k.instruction.BaseInstruction;

import com.stablepay.domain.remittance.exception.SolanaTransactionException;
import com.stablepay.domain.remittance.model.TransactionConfirmationStatus;
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class SolanaTransactionServiceAdapterTest {

    @Mock
    private Connection solanaConnection;

    @Mock
    private EscrowInstructionBuilder escrowInstructionBuilder;

    @Mock
    private MpcWalletClient mpcWalletClient;

    @Mock
    private WalletRepository walletRepository;

    @Captor
    private ArgumentCaptor<byte[]> txBytesCaptor;

    @Captor
    private ArgumentCaptor<byte[]> messageBytesCaptor;

    @Captor
    private ArgumentCaptor<byte[]> keyShareCaptor;

    @Captor
    private ArgumentCaptor<byte[]> peerKeyShareCaptor;

    private SolanaProperties propertiesWithKey;
    private SolanaProperties propertiesWithoutKey;

    @BeforeEach
    void setUp() {
        propertiesWithKey = new SolanaProperties(
                new PublicKey(SOME_PROGRAM_ID),
                new PublicKey(SOME_USDC_MINT),
                SOME_CLAIM_AUTHORITY_PRIVATE_KEY,
                "http://localhost:8899");
        propertiesWithoutKey = new SolanaProperties(
                new PublicKey(SOME_PROGRAM_ID),
                new PublicKey(SOME_USDC_MINT),
                "",
                "http://localhost:8899");
    }

    @Nested
    class ParseSignatureStatusResponse {

        @Test
        void shouldReturnFinalizedForFinalizedResponse() {
            // given
            var json = """
                    {
                      "jsonrpc": "2.0",
                      "result": {
                        "context": {"slot": 123},
                        "value": [{
                          "slot": 123,
                          "confirmations": null,
                          "err": null,
                          "confirmationStatus": "finalized"
                        }]
                      },
                      "id": 1
                    }
                    """;

            // when
            var result = SolanaTransactionServiceAdapter.parseSignatureStatusResponse(json);

            // then
            assertThat(result).isEqualTo(TransactionConfirmationStatus.FINALIZED);
        }

        @Test
        void shouldReturnConfirmedForConfirmedResponse() {
            // given
            var json = """
                    {
                      "jsonrpc": "2.0",
                      "result": {
                        "context": {"slot": 123},
                        "value": [{
                          "slot": 123,
                          "confirmations": 5,
                          "err": null,
                          "confirmationStatus": "confirmed"
                        }]
                      },
                      "id": 1
                    }
                    """;

            // when
            var result = SolanaTransactionServiceAdapter.parseSignatureStatusResponse(json);

            // then
            assertThat(result).isEqualTo(TransactionConfirmationStatus.CONFIRMED);
        }

        @Test
        void shouldReturnProcessedForProcessedResponse() {
            // given
            var json = """
                    {
                      "jsonrpc": "2.0",
                      "result": {
                        "context": {"slot": 123},
                        "value": [{
                          "slot": 123,
                          "confirmations": 0,
                          "err": null,
                          "confirmationStatus": "processed"
                        }]
                      },
                      "id": 1
                    }
                    """;

            // when
            var result = SolanaTransactionServiceAdapter.parseSignatureStatusResponse(json);

            // then
            assertThat(result).isEqualTo(TransactionConfirmationStatus.PROCESSED);
        }

        @Test
        void shouldReturnNotFoundWhenValueIsNull() {
            // given
            var json = """
                    {
                      "jsonrpc": "2.0",
                      "result": {
                        "context": {"slot": 123},
                        "value": [null]
                      },
                      "id": 1
                    }
                    """;

            // when
            var result = SolanaTransactionServiceAdapter.parseSignatureStatusResponse(json);

            // then
            assertThat(result).isEqualTo(TransactionConfirmationStatus.NOT_FOUND);
        }

        @Test
        void shouldReturnFailedOnChainWhenErrIsPresent() {
            // given
            var json = """
                    {
                      "jsonrpc": "2.0",
                      "result": {
                        "context": {"slot": 123},
                        "value": [{
                          "slot": 123,
                          "confirmations": null,
                          "err": {"InstructionError": [0, "Custom"]},
                          "confirmationStatus": "finalized"
                        }]
                      },
                      "id": 1
                    }
                    """;

            // when
            var result = SolanaTransactionServiceAdapter.parseSignatureStatusResponse(json);

            // then
            assertThat(result).isEqualTo(TransactionConfirmationStatus.FAILED_ON_CHAIN);
        }

        @Test
        void shouldReturnFinalizedWhenErrIsNullWithWhitespace() {
            // given
            var json = """
                    {
                      "jsonrpc": "2.0",
                      "result": {
                        "context": {"slot": 123},
                        "value": [{
                          "slot": 123,
                          "confirmations": null,
                          "err":   null,
                          "confirmationStatus": "finalized"
                        }]
                      },
                      "id": 1
                    }
                    """;

            // when
            var result = SolanaTransactionServiceAdapter.parseSignatureStatusResponse(json);

            // then
            assertThat(result).isEqualTo(TransactionConfirmationStatus.FINALIZED);
        }

        @Test
        void shouldReturnNotFoundWhenResponseHasNoValueField() {
            // given
            var json = """
                    {
                      "jsonrpc": "2.0",
                      "result": {
                        "context": {"slot": 123}
                      },
                      "id": 1
                    }
                    """;

            // when
            var result = SolanaTransactionServiceAdapter.parseSignatureStatusResponse(json);

            // then
            assertThat(result).isEqualTo(TransactionConfirmationStatus.NOT_FOUND);
        }
    }

    @Nested
    class ClaimEscrow {

        @Test
        void shouldSubmitClaimTransactionAndReturnSignature() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey,
                    mpcWalletClient, walletRepository);
            var claimAuthorityPubKey = new PublicKey(SOME_CLAIM_AUTHORITY_PUBLIC_KEY);
            var destination = new PublicKey(SOME_DESTINATION_TOKEN_ACCOUNT);
            var instruction = new BaseInstruction(
                    new byte[8], List.of(AccountMeta.signerAndWritable(claimAuthorityPubKey)),
                    new PublicKey(SOME_PROGRAM_ID));

            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            given(escrowInstructionBuilder.buildClaimInstruction(
                    SOME_REMITTANCE_ID, claimAuthorityPubKey, destination, senderWallet))
                    .willReturn(instruction);
            given(solanaConnection.getLatestBlockhash()).willReturn(SOME_BLOCKHASH);
            given(solanaConnection.sendTransaction(txBytesCaptor.capture()))
                    .willReturn(SOME_TRANSACTION_SIGNATURE);

            // when
            var result = adapter.claimEscrow(SOME_REMITTANCE_ID, SOME_DESTINATION_TOKEN_ACCOUNT, SOME_SENDER_WALLET);

            // then
            assertThat(result).isEqualTo(SOME_TRANSACTION_SIGNATURE);
            assertThat(txBytesCaptor.getValue()).isNotEmpty();
        }

        @Test
        void shouldThrowWhenClaimAuthorityNotConfigured() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithoutKey,
                    mpcWalletClient, walletRepository);

            // when / then
            assertThatThrownBy(() -> adapter.claimEscrow(
                    SOME_REMITTANCE_ID, SOME_DESTINATION_TOKEN_ACCOUNT, SOME_SENDER_WALLET))
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
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey,
                    mpcWalletClient, walletRepository);
            var claimAuthorityPubKey = new PublicKey(SOME_CLAIM_AUTHORITY_PUBLIC_KEY);
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            var instruction = new BaseInstruction(
                    new byte[8], List.of(AccountMeta.signerAndWritable(claimAuthorityPubKey)),
                    new PublicKey(SOME_PROGRAM_ID));

            given(escrowInstructionBuilder.buildRefundInstruction(
                    SOME_REMITTANCE_ID, claimAuthorityPubKey, senderWallet))
                    .willReturn(instruction);
            given(solanaConnection.getLatestBlockhash()).willReturn(SOME_BLOCKHASH);
            given(solanaConnection.sendTransaction(txBytesCaptor.capture()))
                    .willReturn(SOME_TRANSACTION_SIGNATURE);

            // when
            var result = adapter.refundEscrow(SOME_REMITTANCE_ID, SOME_SENDER_WALLET);

            // then
            assertThat(result).isEqualTo(SOME_TRANSACTION_SIGNATURE);
            assertThat(txBytesCaptor.getValue()).isNotEmpty();
        }

        @Test
        void shouldThrowWhenClaimAuthorityNotConfiguredForRefund() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithoutKey,
                    mpcWalletClient, walletRepository);

            // when / then
            assertThatThrownBy(() -> adapter.refundEscrow(SOME_REMITTANCE_ID, SOME_SENDER_WALLET))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0014");
        }
    }

    @Nested
    class DepositEscrow {

        @Test
        void shouldSignDepositViaMpcAndReturnSignature() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey,
                    mpcWalletClient, walletRepository);
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            var claimAuthorityPubKey = new PublicKey(SOME_CLAIM_AUTHORITY_PUBLIC_KEY);
            var wallet = walletBuilder()
                    .solanaAddress(SOME_SENDER_WALLET)
                    .keyShareData(new byte[]{1, 2, 3})
                    .build();
            var instruction = new BaseInstruction(
                    new byte[8], List.of(AccountMeta.signerAndWritable(senderWallet)),
                    new PublicKey(SOME_PROGRAM_ID));

            given(escrowInstructionBuilder.buildDepositInstruction(
                    SOME_REMITTANCE_ID, senderWallet, claimAuthorityPubKey,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP))
                    .willReturn(instruction);
            given(walletRepository.findBySolanaAddress(SOME_SENDER_WALLET))
                    .willReturn(Optional.of(wallet));
            given(solanaConnection.getLatestBlockhash()).willReturn(SOME_BLOCKHASH);
            given(mpcWalletClient.signTransaction(
                    messageBytesCaptor.capture(),
                    keyShareCaptor.capture(),
                    peerKeyShareCaptor.capture()))
                    .willReturn(new byte[64]);
            given(solanaConnection.sendTransaction(txBytesCaptor.capture()))
                    .willReturn(SOME_TRANSACTION_SIGNATURE);

            // when
            var result = adapter.depositEscrow(
                    SOME_REMITTANCE_ID, SOME_SENDER_WALLET,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP);

            // then
            assertThat(result).isEqualTo(SOME_TRANSACTION_SIGNATURE);
            assertThat(messageBytesCaptor.getValue()).isNotEmpty();
            assertThat(keyShareCaptor.getValue()).isEqualTo(new byte[]{1, 2, 3});
            assertThat(txBytesCaptor.getValue()).isNotEmpty();
        }

        @Test
        void shouldThrowWhenWalletNotFoundForDeposit() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey,
                    mpcWalletClient, walletRepository);
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            var claimAuthorityPubKey = new PublicKey(SOME_CLAIM_AUTHORITY_PUBLIC_KEY);
            var instruction = new BaseInstruction(
                    new byte[8], List.of(AccountMeta.signerAndWritable(senderWallet)),
                    new PublicKey(SOME_PROGRAM_ID));

            given(escrowInstructionBuilder.buildDepositInstruction(
                    SOME_REMITTANCE_ID, senderWallet, claimAuthorityPubKey,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP))
                    .willReturn(instruction);
            given(walletRepository.findBySolanaAddress(SOME_SENDER_WALLET))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> adapter.depositEscrow(
                    SOME_REMITTANCE_ID, SOME_SENDER_WALLET,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0010");
        }

        @Test
        void shouldThrowWhenMpcSigningFails() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey,
                    mpcWalletClient, walletRepository);
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            var claimAuthorityPubKey = new PublicKey(SOME_CLAIM_AUTHORITY_PUBLIC_KEY);
            var wallet = walletBuilder()
                    .solanaAddress(SOME_SENDER_WALLET)
                    .keyShareData(new byte[]{1, 2, 3})
                    .build();
            var instruction = new BaseInstruction(
                    new byte[8], List.of(AccountMeta.signerAndWritable(senderWallet)),
                    new PublicKey(SOME_PROGRAM_ID));

            given(escrowInstructionBuilder.buildDepositInstruction(
                    SOME_REMITTANCE_ID, senderWallet, claimAuthorityPubKey,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP))
                    .willReturn(instruction);
            given(walletRepository.findBySolanaAddress(SOME_SENDER_WALLET))
                    .willReturn(Optional.of(wallet));
            given(solanaConnection.getLatestBlockhash()).willReturn(SOME_BLOCKHASH);
            given(mpcWalletClient.signTransaction(
                    messageBytesCaptor.capture(),
                    keyShareCaptor.capture(),
                    peerKeyShareCaptor.capture()))
                    .willThrow(new RuntimeException("MPC signing failed"));

            // when / then
            assertThatThrownBy(() -> adapter.depositEscrow(
                    SOME_REMITTANCE_ID, SOME_SENDER_WALLET,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0010");
        }

        @Test
        void shouldThrowWhenClaimAuthorityNotConfiguredForDeposit() {
            // given
            var adapter = new SolanaTransactionServiceAdapter(
                    solanaConnection, escrowInstructionBuilder, propertiesWithoutKey,
                    mpcWalletClient, walletRepository);

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
                    solanaConnection, escrowInstructionBuilder, propertiesWithKey,
                    mpcWalletClient, walletRepository);
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
