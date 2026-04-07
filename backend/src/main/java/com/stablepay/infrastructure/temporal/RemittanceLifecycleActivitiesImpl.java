package com.stablepay.infrastructure.temporal;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.common.port.SmsProvider;
import com.stablepay.domain.remittance.handler.UpdateRemittanceStatusHandler;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.FiatDisbursementProvider;
import com.stablepay.domain.remittance.port.SolanaTransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemittanceLifecycleActivitiesImpl implements RemittanceLifecycleActivities {

    private final SolanaTransactionService solanaTransactionService;
    private final SmsProvider smsProvider;
    private final FiatDisbursementProvider fiatDisbursementProvider;
    private final UpdateRemittanceStatusHandler updateRemittanceStatusHandler;

    @Override
    public String depositEscrow(
            String remittanceId,
            String senderWalletAddress,
            BigDecimal amountUsdc,
            long expiryTimestamp) {
        requireNonNull(remittanceId, "remittanceId must not be null");
        requireNonNull(senderWalletAddress, "senderWalletAddress must not be null");
        requireNonNull(amountUsdc, "amountUsdc must not be null");
        log.info("Depositing escrow for remittance {} amount {} USDC", remittanceId, amountUsdc);
        var uuid = UUID.fromString(remittanceId);
        var signature = solanaTransactionService.depositEscrow(
                uuid, senderWalletAddress, amountUsdc, expiryTimestamp);
        log.info("Escrow deposit completed for remittance {} with signature {}", remittanceId, signature);
        return signature;
    }

    @Override
    public String releaseEscrow(String remittanceId, String destinationTokenAccount) {
        requireNonNull(remittanceId, "remittanceId must not be null");
        requireNonNull(destinationTokenAccount, "destinationTokenAccount must not be null");
        log.info("Releasing escrow for remittance {}", remittanceId);
        var uuid = UUID.fromString(remittanceId);
        var signature = solanaTransactionService.claimEscrow(uuid, destinationTokenAccount);
        log.info("Escrow released for remittance {} with signature {}", remittanceId, signature);
        return signature;
    }

    @Override
    public String refundEscrow(String remittanceId, String senderWalletAddress) {
        requireNonNull(remittanceId, "remittanceId must not be null");
        requireNonNull(senderWalletAddress, "senderWalletAddress must not be null");
        log.info("Refunding escrow for remittance {}", remittanceId);
        var uuid = UUID.fromString(remittanceId);
        var signature = solanaTransactionService.refundEscrow(uuid, senderWalletAddress);
        log.info("Escrow refunded for remittance {} with signature {}", remittanceId, signature);
        return signature;
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
    public void disburseInr(String upiId, String amountUsdc, String remittanceId) {
        requireNonNull(upiId, "upiId must not be null");
        requireNonNull(amountUsdc, "amountUsdc must not be null");
        requireNonNull(remittanceId, "remittanceId must not be null");
        log.info("Disbursing {} USDC as INR to UPI {} for remittance {}", amountUsdc, maskUpi(upiId), remittanceId);
        fiatDisbursementProvider.disburse(upiId, amountUsdc, remittanceId);
        log.info("INR disbursement completed for remittance {}", remittanceId);
    }

    private static String maskUpi(String upiId) {
        if (upiId == null || upiId.length() <= 4) {
            return "****";
        }
        return upiId.substring(0, 3) + "****";
    }

    @Override
    public void updateRemittanceStatus(String remittanceId, RemittanceStatus status) {
        requireNonNull(remittanceId, "remittanceId must not be null");
        requireNonNull(status, "status must not be null");
        log.info("Updating remittance {} status to {}", remittanceId, status);
        var uuid = UUID.fromString(remittanceId);
        updateRemittanceStatusHandler.handle(uuid, status);
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
