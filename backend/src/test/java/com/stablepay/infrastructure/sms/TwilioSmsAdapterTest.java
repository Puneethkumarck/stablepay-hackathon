package com.stablepay.infrastructure.sms;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.common.exception.SmsDeliveryException;
import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;

@ExtendWith(MockitoExtension.class)
class TwilioSmsAdapterTest {

    private static final String SOME_PHONE_NUMBER = "+919876543210";
    private static final String SOME_FROM_NUMBER = "+15017250604";
    private static final String SOME_MESSAGE = "You have a StablePay remittance! Claim your funds: https://claim.stablepay.app/abc-123";
    private static final String SOME_MESSAGE_SID = "SM1234567890abcdef1234567890abcdef";

    @Mock
    private TwilioRestClient twilioRestClient;

    @Mock
    private MessageCreator messageCreator;

    @Mock
    private Message message;

    @Test
    void shouldSendSmsViaTwilioApi() {
        // given
        var properties = TwilioProperties.builder()
                .accountSid("AC_test_account_sid")
                .authToken("test_auth_token")
                .phoneNumber(SOME_FROM_NUMBER)
                .build();
        var adapter = new TestableTwilioSmsAdapter(twilioRestClient, properties, messageCreator);

        given(messageCreator.create(twilioRestClient)).willReturn(message);
        given(message.getSid()).willReturn(SOME_MESSAGE_SID);

        // when
        adapter.sendSms(SOME_PHONE_NUMBER, SOME_MESSAGE);

        // then
        then(messageCreator).should().create(twilioRestClient);
    }

    @Test
    void shouldThrowSmsDeliveryExceptionWhenTwilioApiFails() {
        // given
        var properties = TwilioProperties.builder()
                .accountSid("AC_test_account_sid")
                .authToken("test_auth_token")
                .phoneNumber(SOME_FROM_NUMBER)
                .build();
        var adapter = new TestableTwilioSmsAdapter(twilioRestClient, properties, messageCreator);

        given(messageCreator.create(twilioRestClient))
                .willThrow(new ApiException("Unable to create record: The 'To' number is not a valid phone number.", 21211));

        // when / then
        assertThatThrownBy(() -> adapter.sendSms(SOME_PHONE_NUMBER, SOME_MESSAGE))
                .isInstanceOf(SmsDeliveryException.class)
                .hasMessageContaining("SP-0017")
                .hasCauseInstanceOf(ApiException.class);
    }

    private static class TestableTwilioSmsAdapter extends TwilioSmsAdapter {
        private final MessageCreator stubbedCreator;

        TestableTwilioSmsAdapter(
                TwilioRestClient twilioRestClient,
                TwilioProperties twilioProperties,
                MessageCreator stubbedCreator) {
            super(twilioRestClient, twilioProperties);
            this.stubbedCreator = stubbedCreator;
        }

        @Override
        MessageCreator createMessage(String phoneNumber, String message) {
            return stubbedCreator;
        }
    }
}
