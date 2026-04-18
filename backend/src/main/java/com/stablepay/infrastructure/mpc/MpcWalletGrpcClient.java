package com.stablepay.infrastructure.mpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.protobuf.ByteString;
import com.stablepay.domain.wallet.exception.MpcKeyGenerationException;
import com.stablepay.domain.wallet.exception.MpcSigningException;
import com.stablepay.domain.wallet.model.GeneratedKey;
import com.stablepay.domain.wallet.port.MpcWalletClient;

import io.github.resilience4j.retry.annotation.Retry;
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

    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

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
        this(primaryStub, List.of(), deadlineMs, ceremonyIdGenerator, 0, 1, 1, Map.of());
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
    @Retry(name = "mpcKeygen")
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

            // Collect peer key share data (best-effort — primary result is what matters)
            byte[] peerKeyShareData = null;
            for (var future : peerFutures) {
                try {
                    var peerResponse = future.get(deadlineMs, TimeUnit.MILLISECONDS);
                    if (peerResponse == null) {
                        log.warn("Peer keygen response was null for ceremony {}", ceremonyId);
                    } else {
                        log.info("Peer keygen response for ceremony {}: status={}, keyShareLen={}",
                                ceremonyId, peerResponse.getStatus(),
                                peerResponse.getKeyShareData().size());
                        if (peerResponse.getStatus() == Status.STATUS_COMPLETED
                                && !peerResponse.getKeyShareData().isEmpty()) {
                            peerKeyShareData = peerResponse.getKeyShareData().toByteArray();
                            log.info("Captured peer key share for ceremony {} ({} bytes)",
                                    ceremonyId, peerKeyShareData.length);
                        } else if (peerResponse.getStatus() == Status.STATUS_FAILED) {
                            log.warn("Peer keygen FAILED for ceremony {}: {}",
                                    ceremonyId, peerResponse.getErrorMessage());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Peer sidecar keygen error for ceremony {}: {}",
                            ceremonyId, ex.getMessage());
                }
            }

            if (threshold >= 2 && peerKeyShareData == null) {
                log.error("MPC DKG ceremony {} completed on primary but peer key share is missing",
                        ceremonyId);
                throw MpcKeyGenerationException.peerShareMissing(ceremonyId);
            }

            log.info("MPC key generation completed for ceremony {}: address={}",
                    ceremonyId, response.getSolanaAddress());
            return GeneratedKey.builder()
                    .solanaAddress(response.getSolanaAddress())
                    .publicKey(response.getPublicKey().toByteArray())
                    .keyShareData(response.getKeyShareData().toByteArray())
                    .peerKeyShareData(peerKeyShareData)
                    .build();

        } catch (StatusRuntimeException ex) {
            log.error("gRPC call failed for key generation ceremony {}: {}", ceremonyId, ex.getStatus());
            throw MpcKeyGenerationException.fromCause(ceremonyId, ex);
        }
    }

    @Override
    public byte[] signTransaction(byte[] transactionBytes, byte[] keyShareData, byte[] peerKeyShareData) {
        var ceremonyId = ceremonyIdGenerator.get();
        log.info("Starting MPC signing ceremony: {} (parties: {})", ceremonyId, totalParties);

        var allSigningPartyIds = allPartyIds();

        // Trigger peer sidecars concurrently (same pattern as DKG)
        var peerFutures = Collections.<CompletableFuture<SignResponse>>emptyList();
        if (peerKeyShareData != null) {
            peerFutures = peerSidecars.stream()
                    .map(peer -> triggerPeerSigning(ceremonyId, peer, peerKeyShareData, transactionBytes, allSigningPartyIds))
                    .toList();
        } else {
            log.warn("No peer key share data for ceremony {} — peer signing skipped, "
                    + "this will likely fail in a {}-of-{} setup", ceremonyId, threshold, totalParties);
        }

        var request = SignRequest.newBuilder()
                .setCeremonyId(ceremonyId)
                .setPartyId(partyId)
                .setThreshold(threshold)
                .addAllSigningPartyIds(allSigningPartyIds)
                .setKeyShareData(ByteString.copyFrom(keyShareData))
                .setMessage(ByteString.copyFrom(transactionBytes))
                .putAllPeerAddresses(peerAddresses)
                .build();

        try {
            var response = stubWithDeadline(primaryStub).sign(request);
            validateSignResponse(ceremonyId, response);

            // Collect peer signing results (best-effort logging)
            for (var future : peerFutures) {
                try {
                    var peerResponse = future.get(deadlineMs, TimeUnit.MILLISECONDS);
                    if (peerResponse.getStatus() == Status.STATUS_COMPLETED) {
                        log.info("Peer signing completed for ceremony {}", ceremonyId);
                    } else {
                        log.warn("Peer signing did not complete for ceremony {}: status={}, error={}",
                                ceremonyId, peerResponse.getStatus(), peerResponse.getErrorMessage());
                    }
                } catch (Exception ex) {
                    log.warn("Peer sidecar signing error for ceremony {}: {}", ceremonyId, ex.getMessage());
                }
            }

            log.info("MPC signing completed for ceremony {}", ceremonyId);
            return response.getSignature().toByteArray();

        } catch (StatusRuntimeException ex) {
            log.error("gRPC call failed for signing ceremony {}: {}", ceremonyId, ex.getStatus());
            throw MpcSigningException.fromCause(ceremonyId, ex);
        }
    }

    private CompletableFuture<SignResponse> triggerPeerSigning(
            String ceremonyId, PeerSidecar peer, byte[] peerKeyShareData,
            byte[] message, List<Integer> allSigningPartyIds) {
        return CompletableFuture.supplyAsync(() -> {

            log.info("Triggering peer sidecar (party {}) for signing ceremony {}", peer.partyId(), ceremonyId);
            var peerRequest = SignRequest.newBuilder()
                    .setCeremonyId(ceremonyId)
                    .setPartyId(peer.partyId())
                    .setThreshold(threshold)
                    .addAllSigningPartyIds(allSigningPartyIds)
                    .setKeyShareData(ByteString.copyFrom(peerKeyShareData))
                    .setMessage(ByteString.copyFrom(message))
                    .putAllPeerAddresses(peer.peerAddresses())
                    .build();
            return stubWithDeadline(peer.stub()).sign(peerRequest);
        }, VIRTUAL_EXECUTOR);
    }

    private List<Integer> allPartyIds() {
        var ids = new ArrayList<Integer>();
        ids.add(partyId);
        peerSidecars.forEach(peer -> ids.add(peer.partyId()));
        if (ids.size() < totalParties) {
            for (int i = 0; i < totalParties; i++) {
                if (!ids.contains(i)) {
                    ids.add(i);
                }
            }
        }
        ids.sort(Integer::compareTo);
        return ids;
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
        }, VIRTUAL_EXECUTOR);
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
