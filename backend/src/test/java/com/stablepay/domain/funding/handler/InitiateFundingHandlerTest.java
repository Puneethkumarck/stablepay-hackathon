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
import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.funding.exception.FundingAlreadyInProgressException;
import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.model.FundingInitiationResult;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.FundingStatus;
import com.stablepay.domain.funding.model.PaymentRequest;
import com.stablepay.domain.funding.model.PaymentResult;
import com.stablepay.domain.funding.port.FundingOrderRepository;
import com.stablepay.domain.funding.port.PaymentGateway;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class InitiateFundingHandlerTest {

    private static final String SOME_CLIENT_SECRET = "pi_3MnTest_secret_abc";

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private FundingOrderRepository fundingOrderRepository;

    @InjectMocks
    private InitiateFundingHandler initiateFundingHandler;

    @Captor
    private ArgumentCaptor<FundingOrder> fundingOrderCaptor;

    @Captor
    private ArgumentCaptor<PaymentRequest> paymentRequestCaptor;

    @Test
    void shouldPersistOrderCallStripeAndReturnClientSecret() {
        // given
        given(walletRepository.existsById(SOME_WALLET_ID)).willReturn(true);
        given(fundingOrderRepository.findByWalletIdAndStatusIn(
                SOME_WALLET_ID, List.of(FundingStatus.PAYMENT_CONFIRMED))).willReturn(List.of());

        given(fundingOrderRepository.save(argThat(o -> o != null && o.fundingId() != null)))
                .willAnswer(invocation -> invocation.<FundingOrder>getArgument(0));

        given(paymentGateway.initiatePayment(argThat(r -> r != null && r.fundingId() != null)))
                .willAnswer(invocation -> PaymentResult.builder()
                        .pspReference(SOME_STRIPE_PAYMENT_INTENT_ID)
                        .clientSecret(SOME_CLIENT_SECRET)
                        .status("requires_payment_method")
                        .build());

        // when
        var result = initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC);

        // then
        then(fundingOrderRepository).should(times(2)).save(fundingOrderCaptor.capture());
        then(paymentGateway).should().initiatePayment(paymentRequestCaptor.capture());

        var allSaved = fundingOrderCaptor.getAllValues();
        var firstSave = allSaved.get(0);
        var secondSave = allSaved.get(1);
        var paymentRequest = paymentRequestCaptor.getValue();

        var expectedFirstSave = FundingOrder.builder()
                .fundingId(firstSave.fundingId())
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .build();

        var expectedSecondSave = expectedFirstSave.toBuilder()
                .stripePaymentIntentId(SOME_STRIPE_PAYMENT_INTENT_ID)
                .build();

        var expectedPaymentRequest = PaymentRequest.builder()
                .fundingId(firstSave.fundingId())
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .build();

        var expectedResult = FundingInitiationResult.builder()
                .order(expectedSecondSave)
                .clientSecret(SOME_CLIENT_SECRET)
                .build();

        assertThat(firstSave).usingRecursiveComparison().isEqualTo(expectedFirstSave);
        assertThat(secondSave).usingRecursiveComparison().isEqualTo(expectedSecondSave);
        assertThat(paymentRequest).usingRecursiveComparison().isEqualTo(expectedPaymentRequest);
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        // given
        given(walletRepository.existsById(SOME_WALLET_ID)).willReturn(false);

        // when / then
        assertThatThrownBy(() -> initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006");

        then(fundingOrderRepository).shouldHaveNoInteractions();
        then(paymentGateway).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowWhenActiveOrderAlreadyExists() {
        // given
        given(walletRepository.existsById(SOME_WALLET_ID)).willReturn(true);

        var existing = fundingOrderBuilder().build();
        given(fundingOrderRepository.findByWalletIdAndStatusIn(
                SOME_WALLET_ID, List.of(FundingStatus.PAYMENT_CONFIRMED)))
                .willReturn(List.of(existing));

        // when / then
        assertThatThrownBy(() -> initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .isInstanceOf(FundingAlreadyInProgressException.class)
                .hasMessageContaining("SP-0022")
                .hasMessageContaining(SOME_WALLET_ID.toString());

        then(paymentGateway).shouldHaveNoInteractions();
    }

    @Test
    void shouldPropagateFundingAlreadyInProgressFromAdapter() {
        // given
        given(walletRepository.existsById(SOME_WALLET_ID)).willReturn(true);
        given(fundingOrderRepository.findByWalletIdAndStatusIn(
                SOME_WALLET_ID, List.of(FundingStatus.PAYMENT_CONFIRMED))).willReturn(List.of());

        willThrow(FundingAlreadyInProgressException.forWallet(SOME_WALLET_ID))
                .given(fundingOrderRepository).save(argThat(o -> o != null && o.walletId().equals(SOME_WALLET_ID)));

        // when / then
        assertThatThrownBy(() -> initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .isInstanceOf(FundingAlreadyInProgressException.class)
                .hasMessageContaining("SP-0022");

        then(paymentGateway).shouldHaveNoInteractions();
    }

    @Test
    void shouldTransitionOrderToFailedWhenStripeFails() {
        // given
        given(walletRepository.existsById(SOME_WALLET_ID)).willReturn(true);
        given(fundingOrderRepository.findByWalletIdAndStatusIn(
                SOME_WALLET_ID, List.of(FundingStatus.PAYMENT_CONFIRMED))).willReturn(List.of());

        given(fundingOrderRepository.save(argThat(o -> o != null && o.fundingId() != null)))
                .willAnswer(invocation -> invocation.<FundingOrder>getArgument(0));

        willThrow(FundingFailedException.stripeError("card_declined", new RuntimeException("boom")))
                .given(paymentGateway).initiatePayment(argThat(r -> r != null && r.fundingId() != null));

        // when / then
        assertThatThrownBy(() -> initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC))
                .isInstanceOf(FundingFailedException.class)
                .hasMessageContaining("SP-0021");

        then(fundingOrderRepository).should(times(2)).save(fundingOrderCaptor.capture());
        var saves = fundingOrderCaptor.getAllValues();

        var expectedFirst = FundingOrder.builder()
                .fundingId(saves.get(0).fundingId())
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .status(FundingStatus.PAYMENT_CONFIRMED)
                .build();

        var expectedSecond = expectedFirst.toBuilder()
                .status(FundingStatus.FAILED)
                .build();

        assertThat(saves.get(0)).usingRecursiveComparison().isEqualTo(expectedFirst);
        assertThat(saves.get(1)).usingRecursiveComparison().isEqualTo(expectedSecond);
    }
}
