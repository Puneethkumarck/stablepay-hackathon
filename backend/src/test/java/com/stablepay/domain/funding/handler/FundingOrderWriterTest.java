package com.stablepay.domain.funding.handler;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_STRIPE_PAYMENT_INTENT_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.stablepay.domain.funding.exception.FundingAlreadyInProgressException;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;

@ExtendWith(MockitoExtension.class)
class FundingOrderWriterTest {

    @Mock
    private FundingOrderRepository fundingOrderRepository;

    @InjectMocks
    private FundingOrderWriter fundingOrderWriter;

    @Captor
    private ArgumentCaptor<FundingOrder> fundingOrderCaptor;

    @Test
    void shouldSaveNewPaymentConfirmedOrderWithGeneratedFundingId() {
        // given
        given(fundingOrderRepository.save(argThat(o -> o != null && o.fundingId() != null)))
                .willAnswer(invocation -> invocation.<FundingOrder>getArgument(0));

        // when
        var result = fundingOrderWriter.savePending(SOME_WALLET_ID, SOME_AMOUNT_USDC);

        // then
        then(fundingOrderRepository).should().save(fundingOrderCaptor.capture());
        var saved = fundingOrderCaptor.getValue();

        var expected = FundingOrder.builder()
                .fundingId(saved.fundingId())
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .build();

        assertThat(saved).usingRecursiveComparison().isEqualTo(expected);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        assertThat(saved.fundingId()).isNotNull();
    }

    @Test
    void shouldTranslateUniqueConstraintViolationToFundingAlreadyInProgress() {
        // given — partial unique index on (wallet_id) WHERE status='PAYMENT_CONFIRMED'
        // produces DataIntegrityViolationException when a concurrent funding wins.
        willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(fundingOrderRepository).save(argThat(o -> o != null && o.fundingId() != null));

        // when / then
        assertThatThrownBy(() -> fundingOrderWriter.savePending(SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .isInstanceOf(FundingAlreadyInProgressException.class)
                .hasMessageContaining("SP-0022")
                .hasMessageContaining(SOME_WALLET_ID.toString());
    }

    @Test
    void shouldAttachPaymentIntentWithoutMutatingOtherFields() {
        // given
        var pending = fundingOrderBuilder()
                .stripePaymentIntentId(null)
                .build();
        given(fundingOrderRepository.save(argThat(o -> o != null
                && SOME_STRIPE_PAYMENT_INTENT_ID.equals(o.stripePaymentIntentId()))))
                .willAnswer(invocation -> invocation.<FundingOrder>getArgument(0));

        // when
        var result = fundingOrderWriter.attachPaymentIntent(pending, SOME_STRIPE_PAYMENT_INTENT_ID);

        // then
        then(fundingOrderRepository).should().save(fundingOrderCaptor.capture());
        var saved = fundingOrderCaptor.getValue();

        var expected = pending.toBuilder()
                .stripePaymentIntentId(SOME_STRIPE_PAYMENT_INTENT_ID)
                .build();

        assertThat(saved).usingRecursiveComparison().isEqualTo(expected);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMarkOrderAsFailed() {
        // given
        var pending = fundingOrderBuilder()
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .build();
        given(fundingOrderRepository.save(argThat(o -> o != null
                && o.status() == FundingStatus.FAILED)))
                .willAnswer(invocation -> invocation.<FundingOrder>getArgument(0));

        // when
        fundingOrderWriter.markFailed(pending);

        // then
        then(fundingOrderRepository).should().save(fundingOrderCaptor.capture());
        var saved = fundingOrderCaptor.getValue();

        var expected = pending.toBuilder()
                .status(FundingStatus.FAILED)
                .build();

        assertThat(saved).usingRecursiveComparison().isEqualTo(expected);
    }
}
