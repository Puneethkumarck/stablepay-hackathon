package com.stablepay.domain.exception;

import java.math.BigDecimal;

public class TreasuryDepletedException extends RuntimeException {

    public static TreasuryDepletedException insufficientTreasury(
            BigDecimal requested, BigDecimal available) {
        return new TreasuryDepletedException(
                "SP-0007: Insufficient treasury balance. Requested: "
                        + requested + ", Available: " + available);
    }

    private TreasuryDepletedException(String message) {
        super(message);
    }
}
