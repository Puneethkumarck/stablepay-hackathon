package com.stablepay.infrastructure.mpc;

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
