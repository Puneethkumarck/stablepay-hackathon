package com.stablepay.domain.auth.exception;

public class UnsupportedAuthProviderException extends RuntimeException {

    public static UnsupportedAuthProviderException forProvider(String provider) {
        return new UnsupportedAuthProviderException("SP-0034: Unsupported auth provider: " + provider);
    }

    private UnsupportedAuthProviderException(String message) {
        super(message);
    }
}
