package com.stablepay.domain.wallet.exception;

public class MpcKeyGenerationException extends RuntimeException {

    public static MpcKeyGenerationException withCeremonyId(String ceremonyId, String reason) {
        return new MpcKeyGenerationException(
                "SP-0010: MPC key generation failed for ceremony " + ceremonyId + ": " + reason);
    }

    public static MpcKeyGenerationException fromCause(String ceremonyId, Throwable cause) {
        return new MpcKeyGenerationException(
                "SP-0010: MPC key generation failed for ceremony " + ceremonyId + ": " + cause.getMessage(),
                cause);
    }

    private MpcKeyGenerationException(String message) {
        super(message);
    }

    private MpcKeyGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
