package com.stablepay.infrastructure.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stablepay.domain.common.port.SmsProvider;

@Configuration
public class SmsConfig {

    @Bean
    @ConditionalOnProperty(name = "stablepay.twilio.enabled", havingValue = "false", matchIfMissing = true)
    public SmsProvider loggingSmsAdapter() {
        return new LoggingSmsAdapter();
    }
}
