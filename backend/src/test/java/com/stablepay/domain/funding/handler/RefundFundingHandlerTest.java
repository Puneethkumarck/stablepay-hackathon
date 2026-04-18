package com.stablepay.domain.funding.handler;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_STRIPE_PAYMENT_INTENT_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.funding.exception.InsufficientBalanceForRefundException;
import com.stablepay.domain.funding.exception.RefundFailedException;
import com.stablepay.domain.funding.exception.RefundNotAllowedException;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.funding.port.PaymentGateway;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.TreasuryService;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class RefundFundingHandlerTest {

    @Mock
    private FundingOrderRepository fundingOrderRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TreasuryService treasuryService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private RefundFundingHandler refundFundingHandler;

    @Captor
    private ArgumentCaptor<FundingOrder> fundingOrderCaptor;

    @Captor
    private ArgumentCaptor<Wallet> walletCaptor;

    @Test
    void shouldRefundFundedOrderAndDecrementWalletBalance() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.FUNDED).build();
        var wallet = walletBuilder()
                .id(order.walletId())
                .availableBalance(new BigDecimal("100.00"))
                .totalBalance(new BigDecimal("100.00"))
                .build();

        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(walletRepository.findById(order.walletId())).willReturn(Optional.of(wallet));
        given(treasuryService.getUsdcBalance(wallet.solanaAddress())).willReturn(new BigDecimal("100.00"));

        var refundInitiated = order.toBuilder().status(FundingStatus.REFUND_INITIATED).build();
        var refunded = order.toBuilder().status(FundingStatus.REFUNDED).build();
        given(fundingOrderRepository.save(refundInitiated)).willReturn(refundInitiated);
        given(fundingOrderRepository.save(refunded)).willReturn(refunded);

        var decrementedWallet = wallet.toBuilder()
                .availableBalance(new BigDecimal("75.00"))
                .totalBalance(new BigDecimal("75.00"))
                .build();
        given(walletRepository.save(decrementedWallet)).willReturn(decrementedWallet);

        // when
        var result = refundFundingHandler.handle(SOME_FUNDING_ID);

        // then
        then(fundingOrderRepository).should(times(2)).save(fundingOrderCaptor.capture());
        then(walletRepository).should().save(walletCaptor.capture());
        then(paymentGateway).should().refund(SOME_STRIPE_PAYMENT_INTENT_ID, SOME_AMOUNT_USDC);

        var saves = fundingOrderCaptor.getAllValues();
        assertThat(saves.get(0)).usingRecursiveComparison().isEqualTo(refundInitiated);
        assertThat(saves.get(1)).usingRecursiveComparison().isEqualTo(refunded);
        assertThat(walletCaptor.getValue()).usingRecursiveComparison().isEqualTo(decrementedWallet);
        assertThat(result).usingRecursiveComparison().isEqualTo(refunded);
    }

    @Test
    void shouldThrowWhenFundingOrderNotFound() {
        // given
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> refundFundingHandler.handle(SOME_FUNDING_ID))
                .isInstanceOf(FundingOrderNotFoundException.class)
                .hasMessageContaining("SP-0020");

        then(walletRepository).shouldHaveNoInteractions();
        then(treasuryService).shouldHaveNoInteractions();
        then(paymentGateway).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @EnumSource(value = FundingStatus.class, names = "FUNDED", mode = Mode.EXCLUDE)
    void shouldThrowWhenOrderStatusIsNotFunded(FundingStatus nonFundedStatus) {
        // given
        var order = fundingOrderBuilder().status(nonFundedStatus).build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when / then
        assertThatThrownBy(() -> refundFundingHandler.handle(SOME_FUNDING_ID))
                .isInstanceOf(RefundNotAllowedException.class)
                .hasMessageContaining("SP-0023")
                .hasMessageContaining(nonFundedStatus.name());

        then(walletRepository).shouldHaveNoInteractions();
        then(treasuryService).shouldHaveNoInteractions();
        then(paymentGateway).shouldHaveNoInteractions();
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldThrowWhenOnChainBalanceIsInsufficient() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.FUNDED).build();
        var wallet = walletBuilder()
                .id(order.walletId())
                .availableBalance(new BigDecimal("100.00"))
                .totalBalance(new BigDecimal("100.00"))
                .build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(walletRepository.findById(order.walletId())).willReturn(Optional.of(wallet));
        given(treasuryService.getUsdcBalance(SOME_SOLANA_ADDRESS)).willReturn(new BigDecimal("10.00"));

        // when / then
        assertThatThrownBy(() -> refundFundingHandler.handle(SOME_FUNDING_ID))
                .isInstanceOf(InsufficientBalanceForRefundException.class)
                .hasMessageContaining("SP-0025")
                .hasMessageContaining("25.00")
                .hasMessageContaining("10.00");

        then(paymentGateway).shouldHaveNoInteractions();
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
        then(walletRepository).should().findById(order.walletId());
        then(walletRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldThrowWhenWalletAvailableBalanceIsInsufficient() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.FUNDED).build();
        var wallet = walletBuilder()
                .id(order.walletId())
                .availableBalance(new BigDecimal("5.00"))
                .totalBalance(new BigDecimal("100.00"))
                .build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(walletRepository.findById(order.walletId())).willReturn(Optional.of(wallet));
        given(treasuryService.getUsdcBalance(SOME_SOLANA_ADDRESS)).willReturn(new BigDecimal("100.00"));

        // when / then
        assertThatThrownBy(() -> refundFundingHandler.handle(SOME_FUNDING_ID))
                .isInstanceOf(InsufficientBalanceForRefundException.class)
                .hasMessageContaining("SP-0025")
                .hasMessageContaining("25.00")
                .hasMessageContaining("5.00");

        then(paymentGateway).shouldHaveNoInteractions();
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
        then(walletRepository).should().findById(order.walletId());
        then(walletRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldMarkRefundFailedAndKeepWalletBalanceWhenStripeFails() {
        // given
        var order = fundingOrderBuilder().status(FundingStatus.FUNDED).build();
        var wallet = walletBuilder()
                .id(order.walletId())
                .availableBalance(new BigDecimal("100.00"))
                .totalBalance(new BigDecimal("100.00"))
                .build();
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));
        given(walletRepository.findById(order.walletId())).willReturn(Optional.of(wallet));
        given(treasuryService.getUsdcBalance(SOME_SOLANA_ADDRESS)).willReturn(new BigDecimal("100.00"));

        var refundInitiated = order.toBuilder().status(FundingStatus.REFUND_INITIATED).build();
        given(fundingOrderRepository.save(refundInitiated)).willReturn(refundInitiated);

        var stripeFailure = FundingFailedException.stripeError("card_declined", new RuntimeException("boom"));
        willThrow(stripeFailure).given(paymentGateway).refund(SOME_STRIPE_PAYMENT_INTENT_ID, SOME_AMOUNT_USDC);

        var refundFailed = order.toBuilder().status(FundingStatus.REFUND_FAILED).build();
        given(fundingOrderRepository.save(refundFailed)).willReturn(refundFailed);

        // when / then
        assertThatThrownBy(() -> refundFundingHandler.handle(SOME_FUNDING_ID))
                .isInstanceOf(RefundFailedException.class)
                .hasMessageContaining("SP-0024")
                .hasMessageContaining(SOME_STRIPE_PAYMENT_INTENT_ID)
                .hasCause(stripeFailure);

        then(fundingOrderRepository).should(times(2)).save(fundingOrderCaptor.capture());
        then(walletRepository).should().findById(order.walletId());
        then(walletRepository).shouldHaveNoMoreInteractions();

        var saves = fundingOrderCaptor.getAllValues();
        assertThat(saves.get(0)).usingRecursiveComparison().isEqualTo(refundInitiated);
        assertThat(saves.get(1)).usingRecursiveComparison().isEqualTo(refundFailed);
    }
}
