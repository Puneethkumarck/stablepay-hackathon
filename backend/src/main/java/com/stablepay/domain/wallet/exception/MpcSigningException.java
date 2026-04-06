package com.stablepay.domain.wallet.exception;

public class MpcSigningException extends RuntimeException {

    public static MpcSigningException withCeremonyId(String ceremonyId, String reason) {
        return new MpcSigningException(
                "SP-0011: MPC signing failed for ceremony " + ceremonyId + ": " + reason);
    }

    public static MpcSigningException fromCause(String ceremonyId, Throwable cause) {
        return new MpcSigningException(
                "SP-0011: MPC signing failed for ceremony " + ceremonyId + ": " + cause.getMessage(),
                cause);
    }

    private MpcSigningException(String message) {
        super(message);
    }

    private MpcSigningException(String message, Throwable cause) {
        super(message, cause);
    }
}
