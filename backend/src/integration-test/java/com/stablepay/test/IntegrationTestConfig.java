package com.stablepay.test;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.stablepay.domain.auth.port.SocialIdentityVerifier;
import com.stablepay.domain.common.port.SmsProvider;
import com.stablepay.domain.remittance.port.FiatDisbursementProvider;
import com.stablepay.domain.wallet.port.MpcWalletClient;

@TestConfiguration
public class IntegrationTestConfig {

    @Bean
    public SocialIdentityVerifier socialIdentityVerifier() {
        return Mockito.mock(SocialIdentityVerifier.class);
    }

    @Bean
    public MpcWalletClient mpcWalletClient() {
        return Mockito.mock(MpcWalletClient.class);
    }

    @Bean
    public SmsProvider smsProvider() {
        return Mockito.mock(SmsProvider.class);
    }

    @Bean
    public FiatDisbursementProvider fiatDisbursementProvider() {
        return Mockito.mock(FiatDisbursementProvider.class);
    }
}
