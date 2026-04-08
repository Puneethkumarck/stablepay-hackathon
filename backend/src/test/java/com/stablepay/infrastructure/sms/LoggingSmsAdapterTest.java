package com.stablepay.infrastructure.sms;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class LoggingSmsAdapterTest {

    private final LoggingSmsAdapter adapter = new LoggingSmsAdapter();

    @Test
    void shouldSendSmsWithoutError() {
        // given — logging adapter is a no-op

        // when / then
        assertThatCode(() -> adapter.sendSms("+919876543210", "Your claim link: https://claim.stablepay.app/abc"))
                .doesNotThrowAnyException();
    }
}
