package com.stablepay.infrastructure.mpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import sidecar.v1.TssSidecarGrpc;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "stablepay.mpc.enabled", havingValue = "true", matchIfMissing = true)
public class MpcGrpcConfig implements DisposableBean {

    private final List<ManagedChannel> channels = new ArrayList<>();

    @Bean
    public MpcWalletGrpcClient mpcWalletGrpcClient(
            @Value("${stablepay.mpc.sidecar.host:localhost}") String primaryHost,
            @Value("${stablepay.mpc.sidecar.port:50051}") int primaryPort,
            @Value("${stablepay.mpc.sidecar.deadline-ms:30000}") long deadlineMs,
            @Value("${stablepay.mpc.party-id:0}") int partyId,
            @Value("${stablepay.mpc.threshold:2}") int threshold,
            @Value("${stablepay.mpc.total-parties:2}") int totalParties,
            @Value("${stablepay.mpc.peer-addresses:}") String peerAddressesStr,
            @Value("${stablepay.mpc.peer-sidecars:}") String peerSidecarsStr) {

        var primaryPeerAddresses = parsePeerAddresses(peerAddressesStr);

        // Create primary channel (sidecar-0)
        var primaryChannel = createChannel(primaryHost, primaryPort);
        var primaryStub = TssSidecarGrpc.newBlockingStub(primaryChannel);

        // Create peer sidecar channels (sidecar-1, sidecar-2, etc.)
        var peerSidecars = buildPeerSidecars(peerSidecarsStr, partyId, primaryHost, primaryPort);

        log.info("MPC config: partyId={}, threshold={}, totalParties={}, peers={}, peerSidecars={}",
                partyId, threshold, totalParties, primaryPeerAddresses, peerSidecars.size());

        return new MpcWalletGrpcClient(primaryStub, peerSidecars, deadlineMs,
                partyId, threshold, totalParties, primaryPeerAddresses);
    }

    private List<MpcWalletGrpcClient.PeerSidecar> buildPeerSidecars(
            String peerSidecarsStr, int primaryPartyId, String primaryHost, int primaryPort) {
        var peers = new ArrayList<MpcWalletGrpcClient.PeerSidecar>();
        if (peerSidecarsStr == null || peerSidecarsStr.isBlank()) {
            return peers;
        }
        // Format: "partyId=grpcHost:grpcPort:p2pAddress,..."
        // Example: "1=mpc-sidecar-1:50051:mpc-sidecar-0:7000"
        // p2pAddress is the P2P address of the PRIMARY sidecar as seen from this peer
        for (var entry : peerSidecarsStr.split(",")) {
            var parts = entry.trim().split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            var peerPartyId = Integer.parseInt(parts[0].trim());
            var addrParts = parts[1].trim().split(":");
            if (addrParts.length < 3) {
                log.warn("Invalid peer sidecar format: {}", entry);
                continue;
            }
            var grpcHost = addrParts[0];
            var grpcPort = Integer.parseInt(addrParts[1]);
            var p2pAddr = addrParts[2] + ":" + addrParts[3];

            var channel = createChannel(grpcHost, grpcPort);
            var stub = TssSidecarGrpc.newBlockingStub(channel);

            // This peer's peerAddresses: point back to the primary sidecar's P2P port
            var peerPeerAddresses = Map.of(primaryPartyId, p2pAddr);

            peers.add(new MpcWalletGrpcClient.PeerSidecar(peerPartyId, stub, peerPeerAddresses));
            log.info("Registered peer sidecar: partyId={}, grpc={}:{}, peerAddresses={}",
                    peerPartyId, grpcHost, grpcPort, peerPeerAddresses);
        }
        return peers;
    }

    private ManagedChannel createChannel(String host, int port) {
        var channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .build();
        channels.add(channel);
        return channel;
    }

    private Map<Integer, String> parsePeerAddresses(String raw) {
        var result = new HashMap<Integer, String>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (var entry : raw.split(",")) {
            var parts = entry.trim().split("=", 2);
            if (parts.length == 2) {
                result.put(Integer.parseInt(parts[0].trim()), parts[1].trim());
            }
        }
        return result;
    }

    @Override
    public void destroy() {
        for (var channel : channels) {
            if (!channel.isShutdown()) {
                log.info("Shutting down MPC gRPC channel");
                channel.shutdown();
                try {
                    if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.warn("MPC gRPC channel did not terminate gracefully, forcing shutdown");
                        channel.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    channel.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
