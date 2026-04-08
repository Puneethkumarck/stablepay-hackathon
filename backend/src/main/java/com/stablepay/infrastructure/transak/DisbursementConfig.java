package com.stablepay.infrastructure.transak;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stablepay.domain.remittance.port.FiatDisbursementProvider;

@Configuration
public class DisbursementConfig {

    @Bean
    @ConditionalOnMissingBean(FiatDisbursementProvider.class)
    public FiatDisbursementProvider loggingDisbursementAdapter() {
        return new LoggingDisbursementAdapter();
    }
}
