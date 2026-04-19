package com.stablepay.domain.remittance.model;

public enum TransactionConfirmationStatus {
    NOT_FOUND,
    PROCESSED,
    CONFIRMED,
    FINALIZED,
    FAILED_ON_CHAIN;

    public boolean isConfirmedOrFinalized() {
        return this == CONFIRMED || this == FINALIZED;
    }

    public boolean hasError() {
        return this == FAILED_ON_CHAIN;
    }
}
