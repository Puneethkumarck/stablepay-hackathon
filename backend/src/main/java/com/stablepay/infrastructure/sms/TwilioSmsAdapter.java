package com.stablepay.infrastructure.sms;

import com.stablepay.domain.common.exception.SmsDeliveryException;
import com.stablepay.domain.common.port.SmsProvider;
import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TwilioSmsAdapter implements SmsProvider {

    private final TwilioRestClient twilioRestClient;
    private final TwilioProperties twilioProperties;

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("Sending SMS to {}", maskPhone(phoneNumber));
        try {
            var messageCreator = createMessage(phoneNumber, message);
            var result = messageCreator.create(twilioRestClient);
            log.info("SMS sent successfully to {} with SID {}", maskPhone(phoneNumber), result.getSid());
        } catch (ApiException ex) {
            log.error("Twilio API error sending SMS to {}: {} (code: {})",
                    maskPhone(phoneNumber), ex.getMessage(), ex.getCode());
            throw SmsDeliveryException.forRecipient(phoneNumber, ex);
        }
    }

    MessageCreator createMessage(String phoneNumber, String message) {
        return Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioProperties.phoneNumber()),
                message);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
