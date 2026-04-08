package com.stablepay.infrastructure.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stablepay.domain.common.port.SmsProvider;

@Configuration
public class SmsConfig {

    @Bean
    @ConditionalOnMissingBean(SmsProvider.class)
    public SmsProvider loggingSmsAdapter() {
        return new LoggingSmsAdapter();
    }
}
