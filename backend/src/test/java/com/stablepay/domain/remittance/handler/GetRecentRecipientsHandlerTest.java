package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.RemittanceFixtures.SOME_CREATED_AT;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_NAME;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.remittance.model.RecentRecipient;
import com.stablepay.domain.remittance.port.RemittanceRepository;

@ExtendWith(MockitoExtension.class)
class GetRecentRecipientsHandlerTest {

    @Mock
    private RemittanceRepository remittanceRepository;

    @InjectMocks
    private GetRecentRecipientsHandler getRecentRecipientsHandler;

    @Test
    void shouldReturnRecentRecipientsForSender() {
        // given
        var recipient = RecentRecipient.builder()
                .name(SOME_RECIPIENT_NAME)
                .phone(SOME_RECIPIENT_PHONE)
                .lastSentAt(SOME_CREATED_AT)
                .build();
        given(remittanceRepository.findRecentRecipients(SOME_SENDER_ID, 10))
                .willReturn(List.of(recipient));

        // when
        var result = getRecentRecipientsHandler.handle(SOME_SENDER_ID, 10);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst())
                .usingRecursiveComparison()
                .isEqualTo(recipient);
    }

    @Test
    void shouldReturnEmptyListWhenNoRecipients() {
        // given
        given(remittanceRepository.findRecentRecipients(SOME_SENDER_ID, 10))
                .willReturn(List.of());

        // when
        var result = getRecentRecipientsHandler.handle(SOME_SENDER_ID, 10);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldClampLimitToMaximumOf50() {
        // given
        given(remittanceRepository.findRecentRecipients(SOME_SENDER_ID, 50))
                .willReturn(List.of());

        // when
        var result = getRecentRecipientsHandler.handle(SOME_SENDER_ID, 100);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldClampLimitToMinimumOf1() {
        // given
        given(remittanceRepository.findRecentRecipients(SOME_SENDER_ID, 1))
                .willReturn(List.of());

        // when
        var result = getRecentRecipientsHandler.handle(SOME_SENDER_ID, 0);

        // then
        assertThat(result).isEmpty();
    }
}
