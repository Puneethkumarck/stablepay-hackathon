package com.stablepay.domain.wallet.exception;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public sealed class MpcKeyGenerationException extends RuntimeException
        permits MpcKeyGenerationException.Transient, MpcKeyGenerationException.Permanent {

    private static final String ERROR_PREFIX = "SP-0010: MPC key generation failed for ceremony ";

    public static MpcKeyGenerationException peerShareMissing(String ceremonyId) {
        return new Transient(ERROR_PREFIX + ceremonyId
                + ": peer key share missing after DKG (peer sidecar timed out or returned empty share)");
    }

    public static MpcKeyGenerationException fromCause(String ceremonyId, Throwable cause) {
        var message = ERROR_PREFIX + ceremonyId + ": " + cause.getMessage();
        return isTransient(cause)
                ? new Transient(message, cause)
                : new Permanent(message, cause);
    }

    public static MpcKeyGenerationException withCeremonyId(String ceremonyId, String reason) {
        return new Permanent(ERROR_PREFIX + ceremonyId + ": " + reason);
    }

    private static boolean isTransient(Throwable cause) {
        if (cause instanceof StatusRuntimeException grpc) {
            var code = grpc.getStatus().getCode();
            return code == Status.Code.DEADLINE_EXCEEDED
                    || code == Status.Code.UNAVAILABLE
                    || code == Status.Code.RESOURCE_EXHAUSTED
                    || code == Status.Code.ABORTED;
        }
        return false;
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
