package com.stablepay.domain.remittance.exception;

import com.stablepay.domain.remittance.model.RemittanceStatus;

public class InvalidRemittanceStateException extends RuntimeException {

    public static InvalidRemittanceStateException forClaim(RemittanceStatus currentStatus) {
        return new InvalidRemittanceStateException(
                "SP-0014: Cannot claim remittance in status: " + currentStatus);
    }

    private InvalidRemittanceStateException(String message) {
        super(message);
    }
}
