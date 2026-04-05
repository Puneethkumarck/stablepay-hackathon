package com.stablepay.infrastructure.temporal;

import java.math.BigDecimal;

import com.stablepay.domain.remittance.model.RemittanceStatus;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface RemittanceLifecycleActivities {

    String depositEscrow(
            String remittanceId,
            String senderWalletAddress,
            BigDecimal amountUsdc,
            long expiryTimestamp);

    String releaseEscrow(String remittanceId, String destinationTokenAccount);

    String refundEscrow(String remittanceId, String senderWalletAddress);

    void sendClaimSms(String recipientPhone, String claimUrl);

    void simulateInrDisbursement(String upiId, String amountInr);

    void updateRemittanceStatus(String remittanceId, RemittanceStatus status);
}
