package com.stablepay.infrastructure.mpc;

import java.util.HashMap;
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

    private ManagedChannel channel;

    @Bean
    public ManagedChannel mpcManagedChannel(
            @Value("${stablepay.mpc.sidecar.host:localhost}") String host,
            @Value("${stablepay.mpc.sidecar.port:50051}") int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .build();
        return channel;
    }

    @Bean
    public TssSidecarGrpc.TssSidecarBlockingStub tssSidecarBlockingStub(ManagedChannel mpcManagedChannel) {
        return TssSidecarGrpc.newBlockingStub(mpcManagedChannel);
    }

    @Bean
    public MpcWalletGrpcClient mpcWalletGrpcClient(
            TssSidecarGrpc.TssSidecarBlockingStub tssSidecarBlockingStub,
            @Value("${stablepay.mpc.sidecar.deadline-ms:30000}") long deadlineMs,
            @Value("${stablepay.mpc.party-id:0}") int partyId,
            @Value("${stablepay.mpc.threshold:1}") int threshold,
            @Value("${stablepay.mpc.total-parties:2}") int totalParties,
            @Value("${stablepay.mpc.peer-addresses:}") String peerAddressesStr) {

        var peerAddresses = parsePeerAddresses(peerAddressesStr);
        log.info("MPC config: partyId={}, threshold={}, totalParties={}, peers={}",
                partyId, threshold, totalParties, peerAddresses);
        return new MpcWalletGrpcClient(tssSidecarBlockingStub, deadlineMs,
                partyId, threshold, totalParties, peerAddresses);
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
    public void destroy() throws InterruptedException {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down MPC gRPC channel");
            channel.shutdown();
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("MPC gRPC channel did not terminate gracefully, forcing shutdown");
                channel.shutdownNow();
            }
        }
    }
}
