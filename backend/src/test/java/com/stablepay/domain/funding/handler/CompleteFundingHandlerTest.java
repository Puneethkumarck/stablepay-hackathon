package com.stablepay.domain.funding.handler;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.funding.port.FundingWorkflowStarter;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class CompleteFundingHandlerTest {

    @Mock
    private FundingOrderRepository fundingOrderRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private FundingWorkflowStarter fundingWorkflowStarter;

    private CompleteFundingHandler completeFundingHandler;

    @Captor
    private ArgumentCaptor<FundingOrder> fundingOrderCaptor;

    @BeforeEach
    void setUp() {
        completeFundingHandler = new CompleteFundingHandler(
                fundingOrderRepository, walletRepository, Optional.of(fundingWorkflowStarter));
    }

    @Test
    void shouldMarkOrderFundedAndStartWorkflow() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.PAYMENT_CONFIRMED).build();
        var wallet = walletBuilder().id(SOME_WALLET_ID).solanaAddress(SOME_SOLANA_ADDRESS).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(wallet));
        given(fundingOrderRepository.save(fundingOrderCaptor.capture()))
                .willAnswer(invocation -> invocation.<FundingOrder>getArgument(0));

        // when
        completeFundingHandler.handle(SOME_FUNDING_ID);

        // then
        var expectedSaved = order.toBuilder().status(FundingStatus.FUNDED).build();
        assertThat(fundingOrderCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedSaved);
        then(fundingWorkflowStarter).should()
                .startFundingWorkflow(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_SOLANA_ADDRESS, SOME_AMOUNT_USDC);
    }

    @Test
    void shouldNoOpWhenOrderNotFound() {
        // given
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.empty());

        // when
        completeFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
        then(walletRepository).shouldHaveNoInteractions();
        then(fundingWorkflowStarter).shouldHaveNoInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsFunded() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.FUNDED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        completeFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
        then(walletRepository).shouldHaveNoInteractions();
        then(fundingWorkflowStarter).shouldHaveNoInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsFailed() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.FAILED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        completeFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
        then(walletRepository).shouldHaveNoInteractions();
        then(fundingWorkflowStarter).shouldHaveNoInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsRefundInitiated() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.REFUND_INITIATED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        completeFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
        then(walletRepository).shouldHaveNoInteractions();
        then(fundingWorkflowStarter).shouldHaveNoInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsRefunded() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.REFUNDED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        completeFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
        then(walletRepository).shouldHaveNoInteractions();
        then(fundingWorkflowStarter).shouldHaveNoInteractions();
    }

    @Test
    void shouldNoOpWhenStatusIsRefundFailed() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.REFUND_FAILED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        completeFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
        then(walletRepository).shouldHaveNoInteractions();
        then(fundingWorkflowStarter).shouldHaveNoInteractions();
    }

    @Test
    void shouldNoOpWhenWalletNotFound() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.PAYMENT_CONFIRMED).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.empty());

        // when
        completeFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
        then(walletRepository).should().findById(SOME_WALLET_ID);
        then(walletRepository).shouldHaveNoMoreInteractions();
        then(fundingWorkflowStarter).shouldHaveNoInteractions();
    }

    @Test
    void shouldNoOpWhenFundingIdIsNull() {
        // given (nothing stubbed)

        // when
        completeFundingHandler.handle(null);

        // then
        then(fundingOrderRepository).shouldHaveNoInteractions();
        then(walletRepository).shouldHaveNoInteractions();
        then(fundingWorkflowStarter).shouldHaveNoInteractions();
    }

    @Test
    void shouldMarkOrderFundedEvenWhenWorkflowStarterIsAbsent() {
        // given
        var handlerWithoutStarter = new CompleteFundingHandler(
                fundingOrderRepository, walletRepository, Optional.empty());
        var order = fundingOrderBuilder().status(FundingStatus.PAYMENT_CONFIRMED).build();
        var wallet = walletBuilder().id(SOME_WALLET_ID).solanaAddress(SOME_SOLANA_ADDRESS).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(wallet));
        given(fundingOrderRepository.save(fundingOrderCaptor.capture()))
                .willAnswer(invocation -> invocation.<FundingOrder>getArgument(0));

        // when
        handlerWithoutStarter.handle(SOME_FUNDING_ID);

        // then
        var expectedSaved = order.toBuilder().status(FundingStatus.FUNDED).build();
        assertThat(fundingOrderCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedSaved);
        then(fundingWorkflowStarter).shouldHaveNoInteractions();
    }
}
