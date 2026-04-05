package com.stablepay.infrastructure.temporal;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface RemittanceLifecycleActivities {

    /** Sign escrow deposit transaction via MPC sidecar. */
    byte[] signEscrowDeposit(byte[] unsignedTxBytes);

    /** Sign escrow release transaction via MPC sidecar. */
    byte[] signEscrowRelease(byte[] unsignedTxBytes);

    /** Submit signed transaction to Solana and wait for confirmation. */
    String submitToSolana(byte[] signedTxBytes);

    /** Send claim link SMS to recipient via Twilio. */
    void sendClaimSms(String recipientPhone, String claimUrl);

    /** Simulate INR disbursement (mock for hackathon). */
    void simulateInrDisbursement(String upiId, String amountInr);

    /** Update remittance status in database. */
    void updateRemittanceStatus(String remittanceId, String status);
}
