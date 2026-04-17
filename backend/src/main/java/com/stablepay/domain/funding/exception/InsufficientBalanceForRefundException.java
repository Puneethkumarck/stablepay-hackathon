package com.stablepay.domain.funding.exception;

import java.math.BigDecimal;

public class InsufficientBalanceForRefundException extends RuntimeException {

    public static InsufficientBalanceForRefundException forAmount(BigDecimal requested, BigDecimal available) {
        return new InsufficientBalanceForRefundException(
                "SP-0025: Insufficient balance for refund. Requested: " + requested + ", Available: " + available);
    }

    private InsufficientBalanceForRefundException(String message) {
        super(message);
    }
}
