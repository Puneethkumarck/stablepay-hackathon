package com.stablepay.domain.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public static InvalidRefreshTokenException of(String detail) {
        return new InvalidRefreshTokenException("SP-0035: Invalid refresh token: " + detail);
    }

    private InvalidRefreshTokenException(String message) {
        super(message);
    }
}
