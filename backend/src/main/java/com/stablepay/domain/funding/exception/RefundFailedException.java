package com.stablepay.domain.funding.exception;

public class RefundFailedException extends RuntimeException {

    private RefundFailedException(String message) {
        super(message);
    }

    private RefundFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public static RefundFailedException stripeRefundFailed(String paymentIntentId, Throwable cause) {
        return new RefundFailedException(
                "SP-0024: Stripe refund failed for PaymentIntent: " + paymentIntentId, cause);
    }
}
