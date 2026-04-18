package com.stablepay.domain.funding.handler;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;

@ExtendWith(MockitoExtension.class)
class FailFundingHandlerTest {

    @Mock
    private FundingOrderRepository fundingOrderRepository;

    @InjectMocks
    private FailFundingHandler failFundingHandler;

    @Captor
    private ArgumentCaptor<FundingOrder> fundingOrderCaptor;

    @Test
    void shouldMarkOrderFailed() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.PAYMENT_CONFIRMED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(fundingOrderRepository.save(fundingOrderCaptor.capture()))
                .willAnswer(invocation -> invocation.<FundingOrder>getArgument(0));

        // when
        failFundingHandler.handle(SOME_FUNDING_ID);

        // then
        var expectedSaved = order.toBuilder().status(FundingStatus.FAILED).build();
        assertThat(fundingOrderCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedSaved);
    }

    @Test
    void shouldNoOpWhenOrderNotFound() {
        // given
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.empty());

        // when
        failFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsFailed() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.FAILED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        failFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsFunded() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.FUNDED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        failFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsRefundInitiated() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.REFUND_INITIATED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        failFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsRefunded() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.REFUNDED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        failFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsRefundFailed() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.REFUND_FAILED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        failFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
    }
}
