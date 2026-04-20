package com.stablepay.domain.auth.exception;

public class InvalidIdTokenException extends RuntimeException {

    public static InvalidIdTokenException of(String detail) {
        return new InvalidIdTokenException("SP-0032: Invalid ID token: " + detail);
    }

    private InvalidIdTokenException(String message) {
        super(message);
    }
}
