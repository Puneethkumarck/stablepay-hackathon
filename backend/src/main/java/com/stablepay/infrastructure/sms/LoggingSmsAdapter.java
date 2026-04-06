package com.stablepay.infrastructure.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.stablepay.domain.common.port.SmsProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnMissingBean(SmsProvider.class)
public class LoggingSmsAdapter implements SmsProvider {

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("[SMS] To: {} | Message: {}", phoneNumber, message);
    }
}
