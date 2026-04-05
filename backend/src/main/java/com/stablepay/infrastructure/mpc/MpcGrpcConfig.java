package com.stablepay.infrastructure.mpc;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import sidecar.v1.TssSidecarGrpc;

@Configuration
@ConditionalOnProperty(name = "stablepay.mpc.enabled", havingValue = "true", matchIfMissing = true)
public class MpcGrpcConfig {

    @Bean
    public ManagedChannel mpcManagedChannel(
            @Value("${stablepay.mpc.sidecar.host:localhost}") String host,
            @Value("${stablepay.mpc.sidecar.port:50051}") int port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public TssSidecarGrpc.TssSidecarBlockingStub tssSidecarBlockingStub(ManagedChannel mpcManagedChannel) {
        return TssSidecarGrpc.newBlockingStub(mpcManagedChannel);
    }
}
