package com.stablepay.domain.funding.exception;

public class FundingFailedException extends RuntimeException {

    public static FundingFailedException stripeError(String message, Throwable cause) {
        return new FundingFailedException("SP-0021: Stripe payment failed: " + message, cause);
    }

    public static FundingFailedException invalidRequest(String message) {
        return new FundingFailedException("SP-0022: Invalid funding request: " + message, null);
    }

    private FundingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
