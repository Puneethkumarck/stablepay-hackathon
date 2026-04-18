package com.stablepay.domain.funding.exception;

public class InvalidWebhookSignatureException extends RuntimeException {

    public static InvalidWebhookSignatureException withReason(String reason) {
        return new InvalidWebhookSignatureException(
                "SP-0026: Invalid webhook signature: " + reason, null);
    }

    public static InvalidWebhookSignatureException withReason(String reason, Throwable cause) {
        return new InvalidWebhookSignatureException(
                "SP-0026: Invalid webhook signature: " + reason, cause);
    }

    private InvalidWebhookSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
