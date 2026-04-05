package com.stablepay.infrastructure.mpc;

import static com.stablepay.testutil.MpcFixtures.SOME_CEREMONY_ID;
import static com.stablepay.testutil.MpcFixtures.SOME_DEADLINE_MS;
import static com.stablepay.testutil.MpcFixtures.SOME_KEY_SHARE_DATA;
import static com.stablepay.testutil.MpcFixtures.SOME_SIGNATURE;
import static com.stablepay.testutil.MpcFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.MpcFixtures.SOME_TRANSACTION_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.ByteString;
import com.stablepay.domain.wallet.exception.MpcKeyGenerationException;
import com.stablepay.domain.wallet.exception.MpcSigningException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import sidecar.v1.Sidecar.GenerateKeyRequest;
import sidecar.v1.Sidecar.GenerateKeyResponse;
import sidecar.v1.Sidecar.SignRequest;
import sidecar.v1.Sidecar.SignResponse;
import sidecar.v1.TssSidecarGrpc;

@ExtendWith(MockitoExtension.class)
class MpcWalletGrpcClientTest {

    @Mock
    private TssSidecarGrpc.TssSidecarBlockingStub blockingStub;

    @Mock
    private TssSidecarGrpc.TssSidecarBlockingStub deadlineStub;

    private MpcWalletGrpcClient client;

    private static final GenerateKeyRequest EXPECTED_KEYGEN_REQUEST = GenerateKeyRequest.newBuilder()
            .setCeremonyId(SOME_CEREMONY_ID)
            .setPartyId(1)
            .setThreshold(1)
            .setTotalParties(1)
            .putAllPeerAddresses(Map.of())
            .build();

    private static final SignRequest EXPECTED_SIGN_REQUEST = SignRequest.newBuilder()
            .setCeremonyId(SOME_CEREMONY_ID)
            .setPartyId(1)
            .setThreshold(1)
            .addSigningPartyIds(1)
            .setKeyShareData(ByteString.copyFrom(SOME_KEY_SHARE_DATA))
            .setMessage(ByteString.copyFrom(SOME_TRANSACTION_BYTES))
            .putAllPeerAddresses(Map.of())
            .build();

    @BeforeEach
    void setUp() {
        client = new MpcWalletGrpcClient(blockingStub, SOME_DEADLINE_MS, () -> SOME_CEREMONY_ID);
    }

    @Test
    void shouldGenerateKeyAndReturnSolanaAddress() {
        // given
        var response = GenerateKeyResponse.newBuilder()
                .setSolanaAddress(SOME_SOLANA_ADDRESS)
                .setStatus(sidecar.v1.Sidecar.Status.STATUS_COMPLETED)
                .build();

        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.generateKey(EXPECTED_KEYGEN_REQUEST)).willReturn(response);

        // when
        var result = client.generateKey();

        // then
        assertThat(result).isEqualTo(SOME_SOLANA_ADDRESS);
    }

    @Test
    void shouldThrowMpcKeyGenerationExceptionWhenStatusFailed() {
        // given
        var response = GenerateKeyResponse.newBuilder()
                .setStatus(sidecar.v1.Sidecar.Status.STATUS_FAILED)
                .setErrorMessage("keygen protocol error")
                .build();

        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.generateKey(EXPECTED_KEYGEN_REQUEST)).willReturn(response);

        // when / then
        assertThatThrownBy(() -> client.generateKey())
                .isInstanceOf(MpcKeyGenerationException.class)
                .hasMessageContaining("keygen protocol error");
    }

    @Test
    void shouldThrowMpcKeyGenerationExceptionWhenStatusTimedOut() {
        // given
        var response = GenerateKeyResponse.newBuilder()
                .setStatus(sidecar.v1.Sidecar.Status.STATUS_TIMED_OUT)
                .build();

        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.generateKey(EXPECTED_KEYGEN_REQUEST)).willReturn(response);

        // when / then
        assertThatThrownBy(() -> client.generateKey())
                .isInstanceOf(MpcKeyGenerationException.class)
                .hasMessageContaining("ceremony timed out");
    }

    @Test
    void shouldThrowMpcKeyGenerationExceptionWhenEmptyAddress() {
        // given
        var response = GenerateKeyResponse.newBuilder()
                .setStatus(sidecar.v1.Sidecar.Status.STATUS_COMPLETED)
                .setSolanaAddress("")
                .build();

        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.generateKey(EXPECTED_KEYGEN_REQUEST)).willReturn(response);

        // when / then
        assertThatThrownBy(() -> client.generateKey())
                .isInstanceOf(MpcKeyGenerationException.class)
                .hasMessageContaining("empty Solana address");
    }

    @Test
    void shouldThrowMpcKeyGenerationExceptionOnGrpcError() {
        // given
        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.generateKey(EXPECTED_KEYGEN_REQUEST))
                .willThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        // when / then
        assertThatThrownBy(() -> client.generateKey())
                .isInstanceOf(MpcKeyGenerationException.class)
                .hasCauseInstanceOf(StatusRuntimeException.class);
    }

    @Test
    void shouldSignTransactionAndReturnSignature() {
        // given
        var response = SignResponse.newBuilder()
                .setSignature(ByteString.copyFrom(SOME_SIGNATURE))
                .setStatus(sidecar.v1.Sidecar.Status.STATUS_COMPLETED)
                .build();

        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.sign(EXPECTED_SIGN_REQUEST)).willReturn(response);

        // when
        var result = client.signTransaction(SOME_TRANSACTION_BYTES, SOME_KEY_SHARE_DATA);

        // then
        assertThat(result).isEqualTo(SOME_SIGNATURE);
    }

    @Test
    void shouldThrowMpcSigningExceptionWhenStatusFailed() {
        // given
        var response = SignResponse.newBuilder()
                .setStatus(sidecar.v1.Sidecar.Status.STATUS_FAILED)
                .setErrorMessage("signing protocol error")
                .build();

        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.sign(EXPECTED_SIGN_REQUEST)).willReturn(response);

        // when / then
        assertThatThrownBy(() -> client.signTransaction(SOME_TRANSACTION_BYTES, SOME_KEY_SHARE_DATA))
                .isInstanceOf(MpcSigningException.class)
                .hasMessageContaining("signing protocol error");
    }

    @Test
    void shouldThrowMpcSigningExceptionWhenStatusTimedOut() {
        // given
        var response = SignResponse.newBuilder()
                .setStatus(sidecar.v1.Sidecar.Status.STATUS_TIMED_OUT)
                .build();

        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.sign(EXPECTED_SIGN_REQUEST)).willReturn(response);

        // when / then
        assertThatThrownBy(() -> client.signTransaction(SOME_TRANSACTION_BYTES, SOME_KEY_SHARE_DATA))
                .isInstanceOf(MpcSigningException.class)
                .hasMessageContaining("ceremony timed out");
    }

    @Test
    void shouldThrowMpcSigningExceptionWhenEmptySignature() {
        // given
        var response = SignResponse.newBuilder()
                .setStatus(sidecar.v1.Sidecar.Status.STATUS_COMPLETED)
                .setSignature(ByteString.EMPTY)
                .build();

        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.sign(EXPECTED_SIGN_REQUEST)).willReturn(response);

        // when / then
        assertThatThrownBy(() -> client.signTransaction(SOME_TRANSACTION_BYTES, SOME_KEY_SHARE_DATA))
                .isInstanceOf(MpcSigningException.class)
                .hasMessageContaining("empty signature");
    }

    @Test
    void shouldThrowMpcSigningExceptionOnGrpcError() {
        // given
        given(blockingStub.withDeadlineAfter(SOME_DEADLINE_MS, TimeUnit.MILLISECONDS))
                .willReturn(deadlineStub);
        given(deadlineStub.sign(EXPECTED_SIGN_REQUEST))
                .willThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        // when / then
        assertThatThrownBy(() -> client.signTransaction(SOME_TRANSACTION_BYTES, SOME_KEY_SHARE_DATA))
                .isInstanceOf(MpcSigningException.class)
                .hasCauseInstanceOf(StatusRuntimeException.class);
    }
}
