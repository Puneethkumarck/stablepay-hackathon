package com.stablepay.infrastructure.temporal;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.common.port.SmsProvider;
import com.stablepay.domain.remittance.handler.UpdateRemittanceStatusHandler;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.wallet.port.MpcWalletClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemittanceLifecycleActivitiesImpl implements RemittanceLifecycleActivities {

    private final MpcWalletClient mpcWalletClient;
    private final SmsProvider smsProvider;
    private final UpdateRemittanceStatusHandler updateRemittanceStatusHandler;

    @Override
    public byte[] signEscrowDeposit(byte[] unsignedTxBytes) {
        requireNonNull(unsignedTxBytes, "unsignedTxBytes must not be null");
        log.info("Signing escrow deposit transaction ({} bytes)", unsignedTxBytes.length);
        var keyShareData = resolveKeyShareData();
        var signature = mpcWalletClient.signTransaction(unsignedTxBytes, keyShareData);
        log.info("Escrow deposit signed successfully ({} byte signature)", signature.length);
        return signature;
    }

    @Override
    public byte[] signEscrowRelease(byte[] unsignedTxBytes) {
        requireNonNull(unsignedTxBytes, "unsignedTxBytes must not be null");
        log.info("Signing escrow release transaction ({} bytes)", unsignedTxBytes.length);
        var keyShareData = resolveKeyShareData();
        var signature = mpcWalletClient.signTransaction(unsignedTxBytes, keyShareData);
        log.info("Escrow release signed successfully ({} byte signature)", signature.length);
        return signature;
    }

    @Override
    public String submitToSolana(byte[] signedTxBytes) {
        requireNonNull(signedTxBytes, "signedTxBytes must not be null");
        log.info("Submitting signed transaction to Solana ({} bytes)", signedTxBytes.length);
        // Simulated for hackathon MVP: log the submission and return a stub signature.
        // Full Solana RPC raw-bytes submission with confirmation polling will be
        // integrated when the escrow program is deployed on devnet.
        var txSignature = "sim_" + UUID.randomUUID().toString().replace("-", "");
        log.info("Transaction submitted to Solana, signature={}", txSignature);
        return txSignature;
    }

    @Override
    public void sendClaimSms(String recipientPhone, String claimUrl) {
        requireNonNull(recipientPhone, "recipientPhone must not be null");
        requireNonNull(claimUrl, "claimUrl must not be null");
        log.info("Sending claim SMS to {}", maskPhone(recipientPhone));
        var message = "You have a StablePay remittance! Claim your funds: " + claimUrl;
        smsProvider.sendSms(recipientPhone, message);
        log.info("Claim SMS sent successfully to {}", maskPhone(recipientPhone));
    }

    @Override
    public void simulateInrDisbursement(String upiId, String amountInr) {
        requireNonNull(upiId, "upiId must not be null");
        requireNonNull(amountInr, "amountInr must not be null");
        log.info("Simulating INR disbursement: {} INR to UPI {}", amountInr, upiId);
        // Hackathon MVP: simulate disbursement by logging.
        // Production would integrate with a payment rail (e.g., RazorpayX, Cashfree).
        log.info("INR disbursement simulated successfully: {} INR to UPI {}", amountInr, upiId);
    }

    @Override
    public void updateRemittanceStatus(String remittanceId, RemittanceStatus status) {
        requireNonNull(remittanceId, "remittanceId must not be null");
        requireNonNull(status, "status must not be null");
        log.info("Updating remittance {} status to {}", remittanceId, status);
        var uuid = UUID.fromString(remittanceId);
        updateRemittanceStatusHandler.handle(uuid, status);
    }

    private byte[] resolveKeyShareData() {
        // Hackathon MVP: single-party MPC setup. Key share resolution will be
        // enhanced to look up per-wallet key shares when multi-party signing is
        // integrated. For now, return a minimal placeholder that the MPC sidecar
        // can accept for its single-party ceremony.
        return new byte[0];
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
