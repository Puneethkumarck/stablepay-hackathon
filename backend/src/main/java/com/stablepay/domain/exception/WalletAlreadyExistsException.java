package com.stablepay.domain.exception;

public class WalletAlreadyExistsException extends RuntimeException {

    public static WalletAlreadyExistsException forUserId(String userId) {
        return new WalletAlreadyExistsException(
                "SP-0008: Wallet already exists for user: " + userId);
    }

    private WalletAlreadyExistsException(String message) {
        super(message);
    }
}
