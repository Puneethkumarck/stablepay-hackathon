package com.stablepay.infrastructure.sms;

import com.stablepay.domain.common.port.SmsProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingSmsAdapter implements SmsProvider {

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("[SMS] To: {} | Message: {}", phoneNumber, message);
    }
}
