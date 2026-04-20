package com.stablepay.domain.remittance.port;

import java.math.BigDecimal;
import java.util.UUID;

import com.stablepay.domain.remittance.model.TransactionConfirmationStatus;

public interface SolanaTransactionService {

    String depositEscrow(
            UUID remittanceId,
            String senderWalletAddress,
            BigDecimal amountUsdc,
            long expiryTimestamp);

    String claimEscrow(UUID remittanceId, String destinationTokenAccount, String senderWalletAddress);

    String refundEscrow(UUID remittanceId, String senderWalletAddress);

    TransactionConfirmationStatus getTransactionStatus(String transactionSignature);
}
