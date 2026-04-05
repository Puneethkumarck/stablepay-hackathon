package com.stablepay.domain.claim.exception;

public class ClaimAlreadyClaimedException extends RuntimeException {

    public static ClaimAlreadyClaimedException forToken(String token) {
        return new ClaimAlreadyClaimedException("SP-0012: Claim already submitted for token: " + token);
    }

    private ClaimAlreadyClaimedException(String message) {
        super(message);
    }
}
