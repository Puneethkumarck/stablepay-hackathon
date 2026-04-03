package com.stablepay.domain.wallet.exception;

public class WalletNotFoundException extends RuntimeException {

    public static WalletNotFoundException byId(Long id) {
        return new WalletNotFoundException("SP-0006: Wallet not found: " + id);
    }

    private WalletNotFoundException(String message) {
        super(message);
    }
}
