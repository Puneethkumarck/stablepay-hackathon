package com.stablepay.domain.auth.exception;

public class InvalidIdTokenException extends RuntimeException {

    public static InvalidIdTokenException of(String detail) {
        return new InvalidIdTokenException("SP-0032: Invalid ID token: " + detail);
    }

    public static InvalidIdTokenException of(String detail, Throwable cause) {
        return new InvalidIdTokenException("SP-0032: Invalid ID token: " + detail, cause);
    }

    private InvalidIdTokenException(String message) {
        super(message);
    }

    private InvalidIdTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
