package com.stablepay.domain.wallet.exception;

import java.util.UUID;

public class WalletAlreadyExistsException extends RuntimeException {

    public static WalletAlreadyExistsException forUserId(UUID userId) {
        return new WalletAlreadyExistsException(
                "SP-0008: Wallet already exists for userId: " + userId);
    }

    private WalletAlreadyExistsException(String message) {
        super(message);
    }
}
