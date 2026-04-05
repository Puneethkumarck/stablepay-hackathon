package com.stablepay.infrastructure.solana;

import org.sol4k.Connection;
import org.sol4k.PublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolanaConfig {

    @Bean
    public Connection solanaConnection(
            @Value("${stablepay.solana.rpc-url:https://api.devnet.solana.com}") String rpcUrl) {
        return new Connection(rpcUrl);
    }

    @Bean
    public SolanaProperties solanaProperties(
            @Value("${stablepay.solana.escrow-program-id}") String escrowProgramId,
            @Value("${stablepay.solana.usdc-mint}") String usdcMint,
            @Value("${stablepay.solana.claim-authority-private-key:}") String claimAuthorityPrivateKey) {
        return new SolanaProperties(
                new PublicKey(escrowProgramId),
                new PublicKey(usdcMint),
                claimAuthorityPrivateKey);
    }
}
