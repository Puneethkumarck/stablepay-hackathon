package com.stablepay.domain.wallet.exception;

import java.math.BigDecimal;

public class TreasuryDepletedException extends RuntimeException {

    public static TreasuryDepletedException insufficientTreasury(
            BigDecimal requested, BigDecimal available) {
        return new TreasuryDepletedException(
                "SP-0007: Treasury depleted. Requested: " + requested + ", Available: " + available);
    }

    private TreasuryDepletedException(String message) {
        super(message);
    }
}
