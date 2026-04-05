package com.stablepay.domain.claim.exception;

public class ClaimTokenNotFoundException extends RuntimeException {

    public static ClaimTokenNotFoundException byToken(String token) {
        return new ClaimTokenNotFoundException("SP-0011: Claim token not found: " + token);
    }

    private ClaimTokenNotFoundException(String message) {
        super(message);
    }
}
