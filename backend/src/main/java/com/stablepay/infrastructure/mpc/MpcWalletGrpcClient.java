package com.stablepay.infrastructure.mpc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

    private final TssSidecarGrpc.TssSidecarBlockingStub primaryStub;
    private final List<PeerSidecar> peerSidecars;
    private final long deadlineMs;
    private final Supplier<String> ceremonyIdGenerator;
    private final int partyId;
    private final int threshold;
    private final int totalParties;
    private final Map<Integer, String> peerAddresses;

    public MpcWalletGrpcClient(
            TssSidecarGrpc.TssSidecarBlockingStub primaryStub,
            List<PeerSidecar> peerSidecars,
            long deadlineMs,
            int partyId,
            int threshold,
            int totalParties,
            Map<Integer, String> peerAddresses) {
        this(primaryStub, peerSidecars, deadlineMs, () -> UUID.randomUUID().toString(),
                partyId, threshold, totalParties, peerAddresses);
    }

    MpcWalletGrpcClient(
            TssSidecarGrpc.TssSidecarBlockingStub primaryStub,
            long deadlineMs,
            Supplier<String> ceremonyIdGenerator) {
        this(primaryStub, List.of(), deadlineMs, ceremonyIdGenerator, 0, 2, 2, Map.of());
    }

    MpcWalletGrpcClient(
            TssSidecarGrpc.TssSidecarBlockingStub primaryStub,
            List<PeerSidecar> peerSidecars,
            long deadlineMs,
            Supplier<String> ceremonyIdGenerator,
            int partyId,
            int threshold,
            int totalParties,
            Map<Integer, String> peerAddresses) {
        this.primaryStub = primaryStub;
        this.peerSidecars = peerSidecars;
        this.deadlineMs = deadlineMs;
        this.ceremonyIdGenerator = ceremonyIdGenerator;
        this.partyId = partyId;
        this.threshold = threshold;
        this.totalParties = totalParties;
        this.peerAddresses = peerAddresses;
    }

    @Override
    public GeneratedKey generateKey() {
        var ceremonyId = ceremonyIdGenerator.get();
        log.info("Starting MPC key generation ceremony: {} (parties: {})", ceremonyId, totalParties);

        // Trigger all peer sidecars concurrently (fire-and-forget — they participate via P2P)
        var peerFutures = peerSidecars.stream()
                .map(peer -> triggerPeerKeygen(ceremonyId, peer))
                .toList();

        // Call our primary sidecar (blocking — this is the response we keep)
        var primaryRequest = GenerateKeyRequest.newBuilder()
                .setCeremonyId(ceremonyId)
                .setPartyId(partyId)
                .setThreshold(threshold)
                .setTotalParties(totalParties)
                .putAllPeerAddresses(peerAddresses)
                .build();

        try {
            var response = stubWithDeadline(primaryStub).generateKey(primaryRequest);
            validateGenerateKeyResponse(ceremonyId, response);

            // Wait for peers to complete (best-effort — primary result is what matters)
            peerFutures.forEach(f -> f.exceptionally(ex -> {
                log.warn("Peer sidecar keygen completed with error for ceremony {}: {}",
                        ceremonyId, ex.getMessage());
                return null;
            }));

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

        // Trigger peer sidecars for signing (they need their own key share data — not available here)
        // For signing, the peer sidecar must be triggered separately with its own key share.
        // In a 2-of-2 setup, both parties must participate. For the hackathon, we only sign
        // from the primary sidecar using the stored key share (1-of-1 signing with threshold=2).

        var request = SignRequest.newBuilder()
                .setCeremonyId(ceremonyId)
                .setPartyId(partyId)
                .setThreshold(threshold)
                .addSigningPartyIds(partyId)
                .setKeyShareData(ByteString.copyFrom(keyShareData))
                .setMessage(ByteString.copyFrom(transactionBytes))
                .putAllPeerAddresses(peerAddresses)
                .build();

        try {
            var response = stubWithDeadline(primaryStub).sign(request);
            validateSignResponse(ceremonyId, response);

            log.info("MPC signing completed for ceremony {}", ceremonyId);
            return response.getSignature().toByteArray();

        } catch (StatusRuntimeException ex) {
            log.error("gRPC call failed for signing ceremony {}: {}", ceremonyId, ex.getStatus());
            throw MpcSigningException.fromCause(ceremonyId, ex);
        }
    }

    private CompletableFuture<GenerateKeyResponse> triggerPeerKeygen(String ceremonyId, PeerSidecar peer) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Triggering peer sidecar (party {}) for ceremony {}", peer.partyId(), ceremonyId);
            var peerRequest = GenerateKeyRequest.newBuilder()
                    .setCeremonyId(ceremonyId)
                    .setPartyId(peer.partyId())
                    .setThreshold(threshold)
                    .setTotalParties(totalParties)
                    .putAllPeerAddresses(peer.peerAddresses())
                    .build();
            return stubWithDeadline(peer.stub()).generateKey(peerRequest);
        });
    }

    private TssSidecarGrpc.TssSidecarBlockingStub stubWithDeadline(
            TssSidecarGrpc.TssSidecarBlockingStub stub) {
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    void validateGenerateKeyResponse(String ceremonyId, GenerateKeyResponse response) {
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

    void validateSignResponse(String ceremonyId, SignResponse response) {
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

    public record PeerSidecar(
        int partyId,
        TssSidecarGrpc.TssSidecarBlockingStub stub,
        Map<Integer, String> peerAddresses
    ) {}
}
