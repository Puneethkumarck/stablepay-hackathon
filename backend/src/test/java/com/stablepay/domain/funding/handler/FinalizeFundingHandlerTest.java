package com.stablepay.domain.funding.handler;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_WALLET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.funding.exception.FundingOrderNotFoundException;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.testutil.FundingOrderFixtures;

@ExtendWith(MockitoExtension.class)
class FinalizeFundingHandlerTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private FundingOrderRepository fundingOrderRepository;

    @InjectMocks
    private FinalizeFundingHandler handler;

    @Captor
    private ArgumentCaptor<Wallet> walletCaptor;

    @Captor
    private ArgumentCaptor<FundingOrder> orderCaptor;

    @Test
    void shouldIncrementBalancesAndFlipToFundedAtomically() {
        // given
        var wallet = walletBuilder()
                .id(SOME_WALLET_ID)
                .availableBalance(new BigDecimal("10.000000"))
                .totalBalance(new BigDecimal("10.000000"))
                .build();
        var order = FundingOrderFixtures.fundingOrderBuilder()
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .build();
        given(walletRepository.findByIdForUpdate(SOME_WALLET_ID)).willReturn(Optional.of(wallet));
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        handler.handle(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC);

        // then
        then(walletRepository).should().save(walletCaptor.capture());
        var expectedWallet = wallet.toBuilder()
                .availableBalance(new BigDecimal("35.000000"))
                .totalBalance(new BigDecimal("35.000000"))
                .build();
        assertThat(walletCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedWallet);

        then(fundingOrderRepository).should().save(orderCaptor.capture());
        var expectedOrder = order.toBuilder().status(FundingStatus.FUNDED).build();
        assertThat(orderCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedOrder);
    }

    @Test
    void shouldShortCircuitWhenStatusAlreadyFunded() {
        // given
        var wallet = walletBuilder().id(SOME_WALLET_ID).build();
        var order = FundingOrderFixtures.fundingOrderBuilder()
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(FundingStatus.FUNDED)
                .build();
        given(walletRepository.findByIdForUpdate(SOME_WALLET_ID)).willReturn(Optional.of(wallet));
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.of(order));

        // when
        handler.handle(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC);

        // then
        then(walletRepository).should().findByIdForUpdate(SOME_WALLET_ID);
        then(walletRepository).shouldHaveNoMoreInteractions();
        then(fundingOrderRepository).should().findByFundingId(SOME_FUNDING_ID);
        then(fundingOrderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldBeIdempotentAcrossDoubleInvocationSoBalanceIsCreditedOnlyOnce() {
        // given
        var initialBalance = new BigDecimal("10.000000");
        var initialWallet = walletBuilder()
                .id(SOME_WALLET_ID)
                .availableBalance(initialBalance)
                .totalBalance(initialBalance)
                .build();
        var creditedWallet = initialWallet.toBuilder()
                .availableBalance(initialBalance.add(SOME_AMOUNT_USDC))
                .totalBalance(initialBalance.add(SOME_AMOUNT_USDC))
                .build();
        var pendingOrder = FundingOrderFixtures.fundingOrderBuilder()
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .build();
        var fundedOrder = pendingOrder.toBuilder().status(FundingStatus.FUNDED).build();

        given(walletRepository.findByIdForUpdate(SOME_WALLET_ID))
                .willReturn(Optional.of(initialWallet))
                .willReturn(Optional.of(creditedWallet));
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID))
                .willReturn(Optional.of(pendingOrder))
                .willReturn(Optional.of(fundedOrder));

        // when
        handler.handle(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC);
        handler.handle(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC);

        // then
        then(walletRepository).should().save(walletCaptor.capture());
        assertThat(walletCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(creditedWallet);

        then(fundingOrderRepository).should().save(orderCaptor.capture());
        assertThat(orderCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(fundedOrder);
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        // given
        given(walletRepository.findByIdForUpdate(SOME_WALLET_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> handler.handle(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006");

        then(fundingOrderRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowWhenFundingOrderNotFound() {
        // given
        var wallet = walletBuilder().id(SOME_WALLET_ID).build();
        given(walletRepository.findByIdForUpdate(SOME_WALLET_ID)).willReturn(Optional.of(wallet));
        given(fundingOrderRepository.findByFundingId(SOME_FUNDING_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> handler.handle(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .isInstanceOf(FundingOrderNotFoundException.class)
                .hasMessageContaining("SP-0020");

        then(walletRepository).should().findByIdForUpdate(SOME_WALLET_ID);
        then(walletRepository).shouldHaveNoMoreInteractions();
    }
}
