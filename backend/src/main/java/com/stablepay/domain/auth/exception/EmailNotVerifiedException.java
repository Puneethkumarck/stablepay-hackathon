package com.stablepay.domain.auth.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public static EmailNotVerifiedException forEmail(String email) {
        return new EmailNotVerifiedException("SP-0033: Email not verified: " + email);
    }

    private EmailNotVerifiedException(String message) {
        super(message);
    }
}
