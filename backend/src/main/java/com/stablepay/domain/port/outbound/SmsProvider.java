package com.stablepay.domain.port.outbound;

public interface SmsProvider {
    void sendSms(String phoneNumber, String message);
}
