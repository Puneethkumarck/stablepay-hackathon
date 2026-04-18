package com.stablepay.domain.wallet.exception;

public sealed class MpcKeyGenerationException extends RuntimeException
        permits MpcKeyGenerationException.Transient, MpcKeyGenerationException.Permanent {

    private static final String ERROR_PREFIX = "SP-0010: MPC key generation failed for ceremony ";

    public static MpcKeyGenerationException peerShareMissing(String ceremonyId) {
        return new Transient(ERROR_PREFIX + ceremonyId
                + ": peer key share missing after DKG (peer sidecar timed out or returned empty share)");
    }

    public static MpcKeyGenerationException transientFailure(
            String ceremonyId, String reason, Throwable cause) {
        return new Transient(ERROR_PREFIX + ceremonyId + ": " + reason, cause);
    }

    public static MpcKeyGenerationException transientFailure(String ceremonyId, String reason) {
        return new Transient(ERROR_PREFIX + ceremonyId + ": " + reason);
    }

    public static MpcKeyGenerationException permanentFailure(
            String ceremonyId, String reason, Throwable cause) {
        return new Permanent(ERROR_PREFIX + ceremonyId + ": " + reason, cause);
    }

    public static MpcKeyGenerationException permanentFailure(String ceremonyId, String reason) {
        return new Permanent(ERROR_PREFIX + ceremonyId + ": " + reason);
    }

    private MpcKeyGenerationException(String message) {
        super(message);
    }

    private MpcKeyGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static final class Transient extends MpcKeyGenerationException {
        private Transient(String message) {
            super(message);
        }

        private Transient(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class Permanent extends MpcKeyGenerationException {
        private Permanent(String message) {
            super(message);
        }

        private Permanent(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
