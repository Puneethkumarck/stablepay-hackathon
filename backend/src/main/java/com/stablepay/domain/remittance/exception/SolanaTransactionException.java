package com.stablepay.domain.remittance.exception;

public class SolanaTransactionException extends RuntimeException {

    private SolanaTransactionException(String message) {
        super(message);
    }

    private SolanaTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static SolanaTransactionException submissionFailed(String signature, Throwable cause) {
        return new SolanaTransactionException(
                "SP-0010: Solana transaction submission failed for signature: " + signature, cause);
    }

    public static SolanaTransactionException instructionBuildFailed(String instruction, Throwable cause) {
        return new SolanaTransactionException(
                "SP-0011: Failed to build Solana instruction: " + instruction, cause);
    }

    public static SolanaTransactionException confirmationTimeout(String signature) {
        return new SolanaTransactionException(
                "SP-0012: Solana transaction confirmation timeout for signature: " + signature);
    }

    public static SolanaTransactionException pdaDerivationFailed(String seeds, Throwable cause) {
        return new SolanaTransactionException(
                "SP-0013: Failed to derive PDA with seeds: " + seeds, cause);
    }
}
