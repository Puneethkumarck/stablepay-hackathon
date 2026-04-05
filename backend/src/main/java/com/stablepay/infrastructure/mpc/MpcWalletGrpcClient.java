package com.stablepay.infrastructure.mpc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;

import com.google.protobuf.ByteString;
import com.stablepay.domain.wallet.exception.MpcKeyGenerationException;
import com.stablepay.domain.wallet.exception.MpcSigningException;
import com.stablepay.domain.wallet.model.GeneratedKey;
import com.stablepay.domain.wallet.port.MpcWalletClient;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import sidecar.v1.Sidecar.GenerateKeyRequest;
import sidecar.v1.Sidecar.GenerateKeyResponse;
import sidecar.v1.Sidecar.SignRequest;
import sidecar.v1.Sidecar.SignResponse;
import sidecar.v1.Sidecar.Status;
import sidecar.v1.TssSidecarGrpc;

@Slf4j
public class MpcWalletGrpcClient implements MpcWalletClient {

    private static final int DEFAULT_PARTY_ID = 1;
    private static final int DEFAULT_THRESHOLD = 1;
    private static final int DEFAULT_TOTAL_PARTIES = 1;

    private final TssSidecarGrpc.TssSidecarBlockingStub blockingStub;
    private final long deadlineMs;
    private final Supplier<String> ceremonyIdGenerator;

    public MpcWalletGrpcClient(
            TssSidecarGrpc.TssSidecarBlockingStub blockingStub,
            @Value("${stablepay.mpc.sidecar.deadline-ms:30000}") long deadlineMs) {
        this(blockingStub, deadlineMs, () -> UUID.randomUUID().toString());
    }

    MpcWalletGrpcClient(
            TssSidecarGrpc.TssSidecarBlockingStub blockingStub,
            long deadlineMs,
            Supplier<String> ceremonyIdGenerator) {
        this.blockingStub = blockingStub;
        this.deadlineMs = deadlineMs;
        this.ceremonyIdGenerator = ceremonyIdGenerator;
    }

    @Override
    public GeneratedKey generateKey() {
        var ceremonyId = ceremonyIdGenerator.get();
        log.info("Starting MPC key generation ceremony: {}", ceremonyId);

        var request = GenerateKeyRequest.newBuilder()
                .setCeremonyId(ceremonyId)
                .setPartyId(DEFAULT_PARTY_ID)
                .setThreshold(DEFAULT_THRESHOLD)
                .setTotalParties(DEFAULT_TOTAL_PARTIES)
                .putAllPeerAddresses(Map.of())
                .build();

        try {
            var response = stubWithDeadline().generateKey(request);
            validateGenerateKeyResponse(ceremonyId, response);

            log.info("MPC key generation completed for ceremony {}: address={}",
                    ceremonyId, response.getSolanaAddress());
            return GeneratedKey.builder()
                    .solanaAddress(response.getSolanaAddress())
                    .publicKey(response.getPublicKey().toByteArray())
                    .keyShareData(response.getKeyShareData().toByteArray())
                    .build();

        } catch (StatusRuntimeException ex) {
            log.error("gRPC call failed for key generation ceremony {}: {}", ceremonyId, ex.getStatus());
            throw MpcKeyGenerationException.fromCause(ceremonyId, ex);
        }
    }

    @Override
    public byte[] signTransaction(byte[] transactionBytes, byte[] keyShareData) {
        var ceremonyId = ceremonyIdGenerator.get();
        log.info("Starting MPC signing ceremony: {}", ceremonyId);

        var request = SignRequest.newBuilder()
                .setCeremonyId(ceremonyId)
                .setPartyId(DEFAULT_PARTY_ID)
                .setThreshold(DEFAULT_THRESHOLD)
                .addSigningPartyIds(DEFAULT_PARTY_ID)
                .setKeyShareData(ByteString.copyFrom(keyShareData))
                .setMessage(ByteString.copyFrom(transactionBytes))
                .putAllPeerAddresses(Map.of())
                .build();

        try {
            var response = stubWithDeadline().sign(request);
            validateSignResponse(ceremonyId, response);

            log.info("MPC signing completed for ceremony {}", ceremonyId);
            return response.getSignature().toByteArray();

        } catch (StatusRuntimeException ex) {
            log.error("gRPC call failed for signing ceremony {}: {}", ceremonyId, ex.getStatus());
            throw MpcSigningException.fromCause(ceremonyId, ex);
        }
    }

    private TssSidecarGrpc.TssSidecarBlockingStub stubWithDeadline() {
        return blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    private void validateGenerateKeyResponse(String ceremonyId, GenerateKeyResponse response) {
        if (response.getStatus() != Status.STATUS_COMPLETED) {
            var reason = switch (response.getStatus()) {
                case STATUS_FAILED -> response.getErrorMessage();
                case STATUS_TIMED_OUT -> "ceremony timed out";
                default -> "unexpected status: " + response.getStatus();
            };
            throw MpcKeyGenerationException.withCeremonyId(ceremonyId, reason);
        }
        if (response.getSolanaAddress().isBlank()) {
            throw MpcKeyGenerationException.withCeremonyId(ceremonyId, "empty Solana address in response");
        }
    }

    private void validateSignResponse(String ceremonyId, SignResponse response) {
        if (response.getStatus() != Status.STATUS_COMPLETED) {
            var reason = switch (response.getStatus()) {
                case STATUS_FAILED -> response.getErrorMessage();
                case STATUS_TIMED_OUT -> "ceremony timed out";
                default -> "unexpected status: " + response.getStatus();
            };
            throw MpcSigningException.withCeremonyId(ceremonyId, reason);
        }
        if (response.getSignature().isEmpty()) {
            throw MpcSigningException.withCeremonyId(ceremonyId, "empty signature in response");
        }
    }
}
