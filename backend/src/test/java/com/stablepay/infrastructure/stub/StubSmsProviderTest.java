package com.stablepay.infrastructure.stub;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class StubSmsProviderTest {

    private final StubSmsProvider stubProvider = new StubSmsProvider();

    @Test
    void shouldSendSmsWithoutThrowing() {
        // given
        var phoneNumber = "+919876543210";
        var message = "Your StablePay claim link: https://claim.stablepay.app/demo-token";

        // when / then
        assertThatCode(() -> stubProvider.sendSms(phoneNumber, message))
                .doesNotThrowAnyException();
    }
}
