package com.stablepay.domain.funding.exception;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_STRIPE_PAYMENT_INTENT_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_WALLET_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.stablepay.domain.funding.model.FundingStatus;

class FundingExceptionTest {

    @Test
    void shouldFormatFundingOrderNotFoundMessage() {
        // given
        var exception = FundingOrderNotFoundException.byFundingId(SOME_FUNDING_ID);

        // when
        var message = exception.getMessage();

        // then
        assertThat(message).contains("SP-0020").contains(SOME_FUNDING_ID.toString());
    }

    @Test
    void shouldFormatFundingFailedMessageAndAttachCause() {
        // given
        var cause = new RuntimeException("boom");
        var exception = FundingFailedException.stripeError("card declined", cause);

        // when
        var message = exception.getMessage();

        // then
        assertThat(message).contains("SP-0021").contains("card declined");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldFormatFundingAlreadyInProgressMessage() {
        // given
        var exception = FundingAlreadyInProgressException.forWallet(SOME_WALLET_ID);

        // when
        var message = exception.getMessage();

        // then
        assertThat(message).contains("SP-0022").contains(SOME_WALLET_ID.toString());
    }

    @Test
    void shouldFormatRefundNotAllowedMessage() {
        // given
        var exception = RefundNotAllowedException.forStatus(FundingStatus.FUNDED);

        // when
        var message = exception.getMessage();

        // then
        assertThat(message).contains("SP-0023").contains(FundingStatus.FUNDED.name());
    }

    @Test
    void shouldFormatRefundFailedMessageAndAttachCause() {
        // given
        var cause = new RuntimeException("api down");
        var exception = RefundFailedException.stripeRefundFailed(SOME_STRIPE_PAYMENT_INTENT_ID, cause);

        // when
        var message = exception.getMessage();

        // then
        assertThat(message).contains("SP-0024").contains(SOME_STRIPE_PAYMENT_INTENT_ID);
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void shouldFormatInsufficientBalanceForRefundMessage() {
        // given
        var requested = new BigDecimal("100.00");
        var available = new BigDecimal("25.00");
        var exception = InsufficientBalanceForRefundException.forAmount(requested, available);

        // when
        var message = exception.getMessage();

        // then
        assertThat(message)
                .contains("SP-0025")
                .contains(requested.toString())
                .contains(available.toString());
    }
}
