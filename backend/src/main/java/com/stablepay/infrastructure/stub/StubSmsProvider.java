package com.stablepay.infrastructure.stub;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.stablepay.domain.common.port.SmsProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("stub")
public class StubSmsProvider implements SmsProvider {

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("STUB SMS to {}: {}", phoneNumber, message);
    }
}
