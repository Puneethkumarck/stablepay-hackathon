package com.stablepay.domain.wallet.exception;

public class WalletNotFoundException extends RuntimeException {

    public static WalletNotFoundException byId(Long id) {
        return new WalletNotFoundException("SP-0006: Wallet not found: " + id);
    }

    public static WalletNotFoundException byUserId(String userId) {
        return new WalletNotFoundException("SP-0006: Wallet not found for user: " + userId);
    }

    private WalletNotFoundException(String message) {
        super(message);
    }
}
