package com.stablepay.infrastructure.stripe;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_STRIPE_PAYMENT_INTENT_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_WALLET_ID;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_API_KEY;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_CLIENT_SECRET;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_CURRENCY;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_PAYMENT_INTENT_STATUS;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_TEST_PAYMENT_METHOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.model.PaymentRequest;
import com.stablepay.domain.funding.model.PaymentResult;
import com.stripe.StripeClient;
import com.stripe.exception.ApiException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.service.PaymentIntentService;
import com.stripe.service.RefundService;

@ExtendWith(MockitoExtension.class)
class StripePaymentAdapterTest {

    @Mock
    private StripeClient stripeClient;

    @Mock
    private PaymentIntentService paymentIntentService;

    @Mock
    private RefundService refundService;

    @Mock
    private PaymentIntent paymentIntent;

    @Mock
    private Refund refund;

    @Captor
    private ArgumentCaptor<PaymentIntentCreateParams> paymentIntentParamsCaptor;

    @Captor
    private ArgumentCaptor<RefundCreateParams> refundParamsCaptor;

    private StripeProperties testModeProperties;
    private StripeProperties autoConfirmDisabledProperties;
    private StripePaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        testModeProperties = StripeProperties.builder()
                .apiKey(SOME_STRIPE_API_KEY)
                .webhookSecret("")
                .testMode(true)
                .autoConfirm(true)
                .testPaymentMethod(SOME_STRIPE_TEST_PAYMENT_METHOD)
                .currency(SOME_STRIPE_CURRENCY)
                .build();
        autoConfirmDisabledProperties = testModeProperties.toBuilder()
                .autoConfirm(false)
                .build();
        adapter = new StripePaymentAdapter(stripeClient, testModeProperties);
    }

    @Test
    void shouldCreatePaymentIntentWithConfirmAndPaymentMethodWhenTestModeAndAutoConfirm() throws StripeException {
        // given
        var request = paymentRequest();
        given(stripeClient.paymentIntents()).willReturn(paymentIntentService);
        given(paymentIntentService.create(paymentIntentParamsCaptor.capture())).willReturn(paymentIntent);
        stubPaymentIntentGetters();

        // when
        adapter.initiatePayment(request);

        // then
        var expected = PaymentIntentCreateParams.builder()
                .setAmount(2500L)
                .setCurrency(SOME_STRIPE_CURRENCY)
                .putMetadata("funding_id", SOME_FUNDING_ID.toString())
                .putMetadata("wallet_id", SOME_WALLET_ID.toString())
                .setConfirm(true)
                .setPaymentMethod(SOME_STRIPE_TEST_PAYMENT_METHOD)
                .build();
        assertThat(paymentIntentParamsCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldCreatePaymentIntentWithoutConfirmWhenAutoConfirmDisabled() throws StripeException {
        // given
        adapter = new StripePaymentAdapter(stripeClient, autoConfirmDisabledProperties);
        var request = paymentRequest();
        given(stripeClient.paymentIntents()).willReturn(paymentIntentService);
        given(paymentIntentService.create(paymentIntentParamsCaptor.capture())).willReturn(paymentIntent);
        stubPaymentIntentGetters();

        // when
        adapter.initiatePayment(request);

        // then
        var expected = PaymentIntentCreateParams.builder()
                .setAmount(2500L)
                .setCurrency(SOME_STRIPE_CURRENCY)
                .putMetadata("funding_id", SOME_FUNDING_ID.toString())
                .putMetadata("wallet_id", SOME_WALLET_ID.toString())
                .build();
        assertThat(paymentIntentParamsCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldIncludeFundingIdAndWalletIdInMetadata() throws StripeException {
        // given
        var request = paymentRequest();
        given(stripeClient.paymentIntents()).willReturn(paymentIntentService);
        given(paymentIntentService.create(paymentIntentParamsCaptor.capture())).willReturn(paymentIntent);
        stubPaymentIntentGetters();

        // when
        adapter.initiatePayment(request);

        // then
        var expectedMetadata = Map.of(
                "funding_id", SOME_FUNDING_ID.toString(),
                "wallet_id", SOME_WALLET_ID.toString());
        assertThat(paymentIntentParamsCaptor.getValue().getMetadata())
                .usingRecursiveComparison()
                .isEqualTo(expectedMetadata);
    }

    @Test
    void shouldReturnPaymentResultFromPaymentIntent() throws StripeException {
        // given
        var request = paymentRequest();
        given(stripeClient.paymentIntents()).willReturn(paymentIntentService);
        given(paymentIntentService.create(paymentIntentParamsCaptor.capture())).willReturn(paymentIntent);
        stubPaymentIntentGetters();

        // when
        var result = adapter.initiatePayment(request);

        // then
        var expected = PaymentResult.builder()
                .pspReference(SOME_STRIPE_PAYMENT_INTENT_ID)
                .clientSecret(SOME_STRIPE_CLIENT_SECRET)
                .status(SOME_STRIPE_PAYMENT_INTENT_STATUS)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldWrapStripeExceptionInFundingFailedException() throws StripeException {
        // given
        var request = paymentRequest();
        given(stripeClient.paymentIntents()).willReturn(paymentIntentService);
        var stripeException = new ApiException(
                "card_declined", "req_abc", "card_error", 402, null);
        willThrow(stripeException).given(paymentIntentService).create(paymentIntentParamsCaptor.capture());

        // when / then
        assertThatThrownBy(() -> adapter.initiatePayment(request))
                .isInstanceOf(FundingFailedException.class)
                .hasMessageContaining("SP-0021")
                .hasCause(stripeException);
    }

    @Test
    void shouldWrapArithmeticExceptionInFundingFailedException() {
        // given
        var overflowingAmount = new BigDecimal("92233720368547758.08");
        var request = PaymentRequest.builder()
                .fundingId(SOME_FUNDING_ID)
                .walletId(SOME_WALLET_ID)
                .amountUsdc(overflowingAmount)
                .currency(SOME_STRIPE_CURRENCY)
                .build();

        // when / then
        assertThatThrownBy(() -> adapter.initiatePayment(request))
                .isInstanceOf(FundingFailedException.class)
                .hasMessageContaining("SP-0021")
                .hasMessageContaining("Amount precision")
                .hasCauseInstanceOf(ArithmeticException.class);
    }

    @Test
    void shouldUseCurrencyFromRequestWhenProvided() throws StripeException {
        // given
        var request = paymentRequest().toBuilder().currency("eur").build();
        given(stripeClient.paymentIntents()).willReturn(paymentIntentService);
        given(paymentIntentService.create(paymentIntentParamsCaptor.capture())).willReturn(paymentIntent);
        stubPaymentIntentGetters();

        // when
        adapter.initiatePayment(request);

        // then
        var expected = PaymentIntentCreateParams.builder()
                .setAmount(2500L)
                .setCurrency("eur")
                .putMetadata("funding_id", SOME_FUNDING_ID.toString())
                .putMetadata("wallet_id", SOME_WALLET_ID.toString())
                .setConfirm(true)
                .setPaymentMethod(SOME_STRIPE_TEST_PAYMENT_METHOD)
                .build();
        assertThat(paymentIntentParamsCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldFallBackToConfigCurrencyWhenRequestCurrencyNull() throws StripeException {
        // given
        var request = PaymentRequest.builder()
                .fundingId(SOME_FUNDING_ID)
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .build();
        given(stripeClient.paymentIntents()).willReturn(paymentIntentService);
        given(paymentIntentService.create(paymentIntentParamsCaptor.capture())).willReturn(paymentIntent);
        stubPaymentIntentGetters();

        // when
        adapter.initiatePayment(request);

        // then
        assertThat(paymentIntentParamsCaptor.getValue().getCurrency()).isEqualTo(SOME_STRIPE_CURRENCY);
    }

    @Test
    void shouldRejectNullAmountOnRefund() {
        // when / then
        assertThatThrownBy(() -> adapter.refund(SOME_STRIPE_PAYMENT_INTENT_ID, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("amount");
    }

    @Test
    void shouldRejectNullPaymentIntentIdOnRefund() {
        // when / then
        assertThatThrownBy(() -> adapter.refund(null, SOME_AMOUNT_USDC))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("paymentIntentId");
    }

    @Test
    void shouldRefundViaStripe() throws StripeException {
        // given
        given(stripeClient.refunds()).willReturn(refundService);
        given(refundService.create(refundParamsCaptor.capture())).willReturn(refund);

        // when
        adapter.refund(SOME_STRIPE_PAYMENT_INTENT_ID, SOME_AMOUNT_USDC);

        // then
        var expected = RefundCreateParams.builder()
                .setPaymentIntent(SOME_STRIPE_PAYMENT_INTENT_ID)
                .setAmount(2500L)
                .build();
        assertThat(refundParamsCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldWrapStripeExceptionOnRefund() throws StripeException {
        // given
        given(stripeClient.refunds()).willReturn(refundService);
        var stripeException = new ApiException(
                "charge_already_refunded", "req_xyz", "invalid_request_error", 400, null);
        willThrow(stripeException).given(refundService).create(refundParamsCaptor.capture());

        // when / then
        assertThatThrownBy(() -> adapter.refund(SOME_STRIPE_PAYMENT_INTENT_ID, SOME_AMOUNT_USDC))
                .isInstanceOf(FundingFailedException.class)
                .hasMessageContaining("SP-0021")
                .hasCause(stripeException);
    }

    private PaymentRequest paymentRequest() {
        return PaymentRequest.builder()
                .fundingId(SOME_FUNDING_ID)
                .walletId(SOME_WALLET_ID)
                .amountUsdc(SOME_AMOUNT_USDC)
                .currency(SOME_STRIPE_CURRENCY)
                .build();
    }

    private void stubPaymentIntentGetters() {
        given(paymentIntent.getId()).willReturn(SOME_STRIPE_PAYMENT_INTENT_ID);
        given(paymentIntent.getClientSecret()).willReturn(SOME_STRIPE_CLIENT_SECRET);
        given(paymentIntent.getStatus()).willReturn(SOME_STRIPE_PAYMENT_INTENT_STATUS);
    }
}
