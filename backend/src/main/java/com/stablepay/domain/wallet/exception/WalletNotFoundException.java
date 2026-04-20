package com.stablepay.domain.wallet.exception;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

    public static WalletNotFoundException byId(Long id) {
        return new WalletNotFoundException("SP-0006: Wallet not found: " + id);
    }

    public static WalletNotFoundException byUserId(UUID userId) {
        return new WalletNotFoundException("SP-0006: Wallet not found for user: " + userId);
    }

    private WalletNotFoundException(String message) {
        super(message);
    }
}
