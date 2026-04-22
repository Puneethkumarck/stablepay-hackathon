package com.stablepay.infrastructure.kms;

public class KeyShareEncryptionException extends RuntimeException {

    public KeyShareEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
