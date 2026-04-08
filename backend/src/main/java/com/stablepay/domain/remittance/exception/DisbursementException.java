package com.stablepay.domain.remittance.exception;

import com.stablepay.domain.common.PiiMasking;

public class DisbursementException extends RuntimeException {

    public static DisbursementException forRecipient(String upiId, Throwable cause) {
        return new DisbursementException(
                "SP-0018: INR disbursement failed for UPI: " + PiiMasking.maskUpi(upiId),
                cause);
    }

    public static DisbursementException forRecipient(String upiId, String reason) {
        return new DisbursementException(
                "SP-0018: INR disbursement failed for UPI: " + PiiMasking.maskUpi(upiId) + " - " + reason);
    }

    private DisbursementException(String message) {
        super(message);
    }

    private DisbursementException(String message, Throwable cause) {
        super(message, cause);
    }
}
