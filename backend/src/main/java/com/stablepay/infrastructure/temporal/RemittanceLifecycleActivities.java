package com.stablepay.infrastructure.temporal;

import java.math.BigDecimal;

import com.stablepay.domain.remittance.model.DisbursementResult;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.TransactionConfirmationStatus;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface RemittanceLifecycleActivities {

    TransactionConfirmationStatus checkTransactionStatus(String transactionSignature);

    String depositEscrow(
            String remittanceId,
            String senderWalletAddress,
            BigDecimal amountUsdc,
            long expiryTimestamp);

    String releaseEscrow(String remittanceId, String destinationTokenAccount, String senderWalletAddress);

    String refundEscrow(String remittanceId, String senderWalletAddress);

    void sendClaimSms(String recipientPhone, String claimUrl);

    DisbursementResult disburseInr(
            String upiId,
            BigDecimal amountUsdc,
            BigDecimal amountInr,
            String remittanceId);

    void updateRemittanceStatus(String remittanceId, RemittanceStatus status);
}
