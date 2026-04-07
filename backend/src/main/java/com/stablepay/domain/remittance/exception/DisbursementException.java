package com.stablepay.domain.remittance.exception;

public class DisbursementException extends RuntimeException {

    public static DisbursementException forRecipient(String upiId, Throwable cause) {
        return new DisbursementException(
                "SP-0018: INR disbursement failed for UPI: " + maskUpi(upiId),
                cause);
    }

    public static DisbursementException forRecipient(String upiId, String reason) {
        return new DisbursementException(
                "SP-0018: INR disbursement failed for UPI: " + maskUpi(upiId) + " - " + reason);
    }

    private DisbursementException(String message) {
        super(message);
    }

    private DisbursementException(String message, Throwable cause) {
        super(message, cause);
    }

    private static String maskUpi(String upiId) {
        if (upiId == null || upiId.length() <= 4) {
            return "****";
        }
        return upiId.substring(0, 3) + "****";
    }
}
