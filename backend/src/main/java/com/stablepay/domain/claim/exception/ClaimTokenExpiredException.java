package com.stablepay.domain.claim.exception;

public class ClaimTokenExpiredException extends RuntimeException {

    public static ClaimTokenExpiredException forToken(String token) {
        return new ClaimTokenExpiredException("SP-0013: Claim token expired: " + token);
    }

    private ClaimTokenExpiredException(String message) {
        super(message);
    }
}
