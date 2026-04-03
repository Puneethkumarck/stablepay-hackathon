package com.stablepay.domain.wallet.exception;

public class WalletAlreadyExistsException extends RuntimeException {

    public static WalletAlreadyExistsException forUserId(String userId) {
        return new WalletAlreadyExistsException(
                "SP-0008: Wallet already exists for userId: " + userId);
    }

    private WalletAlreadyExistsException(String message) {
        super(message);
    }
}
