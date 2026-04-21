package com.stablepay.domain.funding.handler;

import static com.stablepay.testutil.AuthFixtures.SOME_OTHER_USER_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_STRIPE_PAYMENT_INTENT_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.FundingOrderFixtures.fundingOrderBuilder;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.funding.exception.FundingAlreadyInProgressException;
import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.model.FundingInitiationResult;
import com.stablepay.domain.funding.model.FundingOrder;
import com.stablepay.domain.funding.model.PaymentRequest;
import com.stablepay.domain.funding.model.PaymentResult;
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
    private FundingOrderWriter fundingOrderWriter;

    @InjectMocks
    private InitiateFundingHandler initiateFundingHandler;

    @Captor
    private ArgumentCaptor<PaymentRequest> paymentRequestCaptor;

    @Captor
    private ArgumentCaptor<FundingOrder> attachOrderCaptor;

    @Test
    void shouldCommitPendingOrderBeforeStripeCallAndAttachPaymentIntentAfter() {
        // given
        var wallet = walletBuilder().id(SOME_WALLET_ID).build();
        var pending = fundingOrderBuilder()
                .stripePaymentIntentId(null)
                .build();
        var withIntent = pending.toBuilder()
                .stripePaymentIntentId(SOME_STRIPE_PAYMENT_INTENT_ID)
                .build();

        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(wallet));
        given(fundingOrderWriter.savePending(SOME_WALLET_ID, SOME_AMOUNT_USDC)).willReturn(pending);
        given(paymentGateway.initiatePayment(paymentRequestCaptor.capture()))
                .willReturn(PaymentResult.builder()
                        .pspReference(SOME_STRIPE_PAYMENT_INTENT_ID)
                        .clientSecret(SOME_CLIENT_SECRET)
                        .status("requires_payment_method")
                        .build());
        given(fundingOrderWriter.attachPaymentIntent(pending, SOME_STRIPE_PAYMENT_INTENT_ID))
                .willReturn(withIntent);

        // when
        var result = initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC, SOME_USER_ID);

        // then
        InOrder inOrder = Mockito.inOrder(fundingOrderWriter, paymentGateway);
        inOrder.verify(fundingOrderWriter).savePending(SOME_WALLET_ID, SOME_AMOUNT_USDC);
        inOrder.verify(paymentGateway).initiatePayment(Mockito.any(PaymentRequest.class));
        inOrder.verify(fundingOrderWriter).attachPaymentIntent(pending, SOME_STRIPE_PAYMENT_INTENT_ID);
        then(fundingOrderWriter).should(never()).markFailed(Mockito.any());

        var expectedPaymentRequest = PaymentRequest.builder()
                .fundingId(pending.fundingId())
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .build();

        var expectedResult = FundingInitiationResult.builder()
                .order(withIntent)
                .clientSecret(SOME_CLIENT_SECRET)
                .build();

        assertThat(paymentRequestCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedPaymentRequest);
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        // given
        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC, SOME_USER_ID))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006");

        then(fundingOrderWriter).shouldHaveNoInteractions();
        then(paymentGateway).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowWhenWalletBelongsToDifferentUser() {
        // given
        var wallet = walletBuilder().id(SOME_WALLET_ID).build();
        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(wallet));

        // when / then
        assertThatThrownBy(() -> initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC, SOME_OTHER_USER_ID))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006");

        then(fundingOrderWriter).shouldHaveNoInteractions();
        then(paymentGateway).shouldHaveNoInteractions();
    }

    @Test
    void shouldNotCallStripeWhenWriterRejectsConcurrentFunding() {
        // given
        var wallet = walletBuilder().id(SOME_WALLET_ID).build();
        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(wallet));
        willThrow(FundingAlreadyInProgressException.forWallet(SOME_WALLET_ID))
                .given(fundingOrderWriter).savePending(SOME_WALLET_ID, SOME_AMOUNT_USDC);

        // when / then
        assertThatThrownBy(() -> initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC, SOME_USER_ID))
                .isInstanceOf(FundingAlreadyInProgressException.class)
                .hasMessageContaining("SP-0022");

        then(paymentGateway).shouldHaveNoInteractions();
        then(fundingOrderWriter).should(never()).attachPaymentIntent(Mockito.any(), Mockito.anyString());
        then(fundingOrderWriter).should(never()).markFailed(Mockito.any());
    }

    @Test
    void shouldMarkOrderFailedAndPropagateWhenStripeRejects() {
        // given
        var wallet = walletBuilder().id(SOME_WALLET_ID).build();
        var pending = fundingOrderBuilder()
                .stripePaymentIntentId(null)
                .build();

        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(wallet));
        given(fundingOrderWriter.savePending(SOME_WALLET_ID, SOME_AMOUNT_USDC)).willReturn(pending);
        willThrow(FundingFailedException.stripeError("card_declined", new RuntimeException("boom")))
                .given(paymentGateway).initiatePayment(paymentRequestCaptor.capture());

        // when / then
        assertThatThrownBy(() -> initiateFundingHandler.handle(SOME_WALLET_ID, SOME_AMOUNT_USDC, SOME_USER_ID))
                .isInstanceOf(FundingFailedException.class)
                .hasMessageContaining("SP-0021");

        then(fundingOrderWriter).should().markFailed(attachOrderCaptor.capture());
        then(fundingOrderWriter).should(never()).attachPaymentIntent(Mockito.any(), Mockito.anyString());

        assertThat(attachOrderCaptor.getValue()).usingRecursiveComparison().isEqualTo(pending);
    }
}
