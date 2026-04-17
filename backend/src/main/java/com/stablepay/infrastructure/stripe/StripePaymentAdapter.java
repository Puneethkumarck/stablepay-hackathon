package com.stablepay.infrastructure.stripe;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.stablepay.domain.funding.exception.FundingFailedException;
import com.stablepay.domain.funding.model.PaymentRequest;
import com.stablepay.domain.funding.model.PaymentResult;
import com.stablepay.domain.funding.port.PaymentGateway;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripePaymentAdapter implements PaymentGateway {

    private final StripeClient stripeClient;
    private final StripeProperties stripeProperties;

    @Override
    public PaymentResult initiatePayment(PaymentRequest request) {
        validateAmount(request.amountUsdc(), "amountUsdc");
        log.info("Initiating Stripe PaymentIntent fundingId={} walletId={}",
                request.fundingId(), request.walletId());
        try {
            var params = buildPaymentIntentParams(request);
            var intent = stripeClient.paymentIntents().create(params);
            return PaymentResult.builder()
                    .pspReference(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .status(intent.getStatus())
                    .build();
        } catch (StripeException e) {
            log.warn("Stripe PaymentIntent creation failed fundingId={} code={} status={} requestId={}",
                    request.fundingId(), e.getCode(), e.getStatusCode(), e.getRequestId(), e);
            throw FundingFailedException.stripeError("[" + e.getCode() + "] " + e.getMessage(), e);
        } catch (ArithmeticException e) {
            throw FundingFailedException.stripeError(
                    "Amount precision exceeds Stripe's cent granularity", e);
        }
    }

    @Override
    public void refund(String paymentIntentId, BigDecimal amount) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw FundingFailedException.invalidRequest("paymentIntentId is required for refund");
        }
        validateAmount(amount, "refund amount");
        log.info("Initiating Stripe refund paymentIntentId={}", paymentIntentId);
        try {
            var params = buildRefundParams(paymentIntentId, amount);
            stripeClient.refunds().create(params);
        } catch (StripeException e) {
            log.warn("Stripe refund failed paymentIntentId={} code={} status={} requestId={}",
                    paymentIntentId, e.getCode(), e.getStatusCode(), e.getRequestId(), e);
            throw FundingFailedException.stripeError("[" + e.getCode() + "] " + e.getMessage(), e);
        } catch (ArithmeticException e) {
            throw FundingFailedException.stripeError(
                    "Amount precision exceeds Stripe's cent granularity", e);
        }
    }

    private PaymentIntentCreateParams buildPaymentIntentParams(PaymentRequest request) {
        var cents = request.amountUsdc().movePointRight(2).longValueExact();
        var currency = request.currency() != null ? request.currency() : stripeProperties.currency();
        var builder = PaymentIntentCreateParams.builder()
                .setAmount(cents)
                .setCurrency(currency)
                .putMetadata("funding_id", request.fundingId().toString())
                .putMetadata("wallet_id", request.walletId().toString());
        if (stripeProperties.testMode() && stripeProperties.autoConfirm()) {
            builder.setConfirm(true);
            builder.setPaymentMethod(stripeProperties.testPaymentMethod());
        }
        return builder.build();
    }

    private RefundCreateParams buildRefundParams(String paymentIntentId, BigDecimal amount) {
        var cents = amount.movePointRight(2).longValueExact();
        return RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(cents)
                .build();
    }

    private void validateAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw FundingFailedException.invalidRequest(fieldName + " is required");
        }
        if (amount.signum() <= 0) {
            throw FundingFailedException.invalidRequest(fieldName + " must be positive");
        }
    }
}
