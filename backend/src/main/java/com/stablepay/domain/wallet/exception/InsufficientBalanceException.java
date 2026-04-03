package com.stablepay.domain.wallet.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    public static InsufficientBalanceException forAmount(BigDecimal requested, BigDecimal available) {
        return new InsufficientBalanceException(
                "SP-0002: Insufficient balance. Requested: " + requested + ", Available: " + available);
    }

    private InsufficientBalanceException(String message) {
        super(message);
    }
}
