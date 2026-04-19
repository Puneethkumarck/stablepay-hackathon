package com.stablepay.domain.remittance.exception;

import com.stablepay.domain.common.PiiMasking;

public class DisbursementException extends RuntimeException {

    public static DisbursementException forRecipient(String upiId, Throwable cause) {
        return new DisbursementException(buildMessage(upiId, null), cause);
    }

    public static DisbursementException forRecipient(String upiId, String reason) {
        return new DisbursementException(buildMessage(upiId, reason));
    }

    public static DisbursementException retriable(String upiId, Throwable cause) {
        return new DisbursementException(buildMessage(upiId, null), cause);
    }

    public static NonRetriable nonRetriable(String upiId, String reason) {
        return new NonRetriable(buildMessage(upiId, reason));
    }

    private DisbursementException(String message) {
        super(message);
    }

    private DisbursementException(String message, Throwable cause) {
        super(message, cause);
    }

    private static String buildMessage(String upiId, String reason) {
        var base = "SP-0018: INR disbursement failed for UPI: " + PiiMasking.maskUpi(upiId);
        return reason == null ? base : base + " - " + reason;
    }

    public static class NonRetriable extends DisbursementException {

        private NonRetriable(String message) {
            super(message);
        }
    }
}
