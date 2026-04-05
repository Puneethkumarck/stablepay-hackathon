package com.stablepay.infrastructure.temporal;

import com.stablepay.domain.remittance.model.RemittanceStatus;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface RemittanceLifecycleActivities {

    byte[] signEscrowDeposit(byte[] unsignedTxBytes);

    byte[] signEscrowRelease(byte[] unsignedTxBytes);

    String submitToSolana(byte[] signedTxBytes);

    void sendClaimSms(String recipientPhone, String claimUrl);

    void simulateInrDisbursement(String upiId, String amountInr);

    void updateRemittanceStatus(String remittanceId, RemittanceStatus status);
}
