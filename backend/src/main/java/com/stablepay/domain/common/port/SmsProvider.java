package com.stablepay.domain.common.port;

public interface SmsProvider {
    void sendSms(String phoneNumber, String message);
}
