package com.stablepay.infrastructure.temporal;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.common.PiiMasking;
import com.stablepay.domain.common.port.SmsProvider;
import com.stablepay.domain.remittance.exception.DisbursementException;
import com.stablepay.domain.remittance.handler.RemittancePayoutWriter;
import com.stablepay.domain.remittance.handler.UpdateRemittanceStatusHandler;
import com.stablepay.domain.remittance.model.DisbursementResult;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.TransactionConfirmationStatus;
import com.stablepay.domain.remittance.port.FiatDisbursementProvider;
import com.stablepay.domain.remittance.port.SolanaTransactionService;
import com.stablepay.infrastructure.solana.EscrowInstructionBuilder;

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
    private final RemittancePayoutWriter remittancePayoutWriter;
    private final EscrowInstructionBuilder escrowInstructionBuilder;

    @Override
    public TransactionConfirmationStatus checkTransactionStatus(String transactionSignature) {
        requireNonNull(transactionSignature, "transactionSignature must not be null");
        log.info("Checking transaction status for signature {}", transactionSignature);
        var status = solanaTransactionService.getTransactionStatus(transactionSignature);
        log.info("Transaction {} status: {}", transactionSignature, status);
        return status;
    }

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
    public String releaseEscrow(String remittanceId, String destinationTokenAccount, String senderWalletAddress) {
        requireNonNull(remittanceId, "remittanceId must not be null");
        requireNonNull(destinationTokenAccount, "destinationTokenAccount must not be null");
        requireNonNull(senderWalletAddress, "senderWalletAddress must not be null");
        log.info("Releasing escrow for remittance {}", remittanceId);
        var uuid = UUID.fromString(remittanceId);
        var signature = solanaTransactionService.claimEscrow(uuid, destinationTokenAccount, senderWalletAddress);
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
    public DisbursementResult disburseInr(
            String upiId,
            BigDecimal amountUsdc,
            BigDecimal amountInr,
            String remittanceId) {
        requireNonNull(upiId, "upiId must not be null");
        requireNonNull(amountUsdc, "amountUsdc must not be null");
        requireNonNull(amountInr, "amountInr must not be null");
        requireNonNull(remittanceId, "remittanceId must not be null");
        var remittanceUuid = UUID.fromString(remittanceId);

        var existing = remittancePayoutWriter.findExistingPayout(remittanceUuid);
        if (existing.isPresent()) {
            log.info("Payout already persisted for remittance {}, returning cached result", remittanceId);
            return existing.get();
        }

        log.info(
                "Disbursing {} USDC ({} INR) as INR to UPI {} for remittance {}",
                amountUsdc,
                amountInr,
                PiiMasking.maskUpi(upiId),
                remittanceId);
        try {
            var result = fiatDisbursementProvider.disburse(upiId, amountUsdc, amountInr, remittanceId);
            if (result == null) {
                throw DisbursementException.nonRetriable(
                        upiId, "fiatDisbursementProvider returned null DisbursementResult");
            }
            try {
                remittancePayoutWriter.writePayoutId(
                        remittanceUuid, result.providerId(), result.providerStatus());
            } catch (RuntimeException writeFailure) {
                log.error(
                        "SP-0030: CRITICAL — manual reconciliation required for remittance={} providerId={} — payout succeeded but payout_id persistence failed",
                        remittanceId,
                        result.providerId(),
                        writeFailure);
                throw writeFailure;
            }
            log.info(
                    "INR disbursement completed for remittance {} providerId={} providerStatus={}",
                    remittanceId,
                    result.providerId(),
                    result.providerStatus());
            return result;
        } catch (DisbursementException e) {
            try {
                remittancePayoutWriter.writeFailureReason(remittanceUuid, e.getMessage());
            } catch (RuntimeException writeFailure) {
                log.error(
                        "SP-0029: failed to persist payout_failure_reason for remittance {} — original disbursement exception preserved",
                        remittanceId,
                        writeFailure);
            }
            throw e;
        }
    }

    @Override
    public void updateRemittanceStatus(String remittanceId, RemittanceStatus status) {
        requireNonNull(remittanceId, "remittanceId must not be null");
        requireNonNull(status, "status must not be null");
        log.info("Updating remittance {} status to {}", remittanceId, status);
        var uuid = UUID.fromString(remittanceId);
        updateRemittanceStatusHandler.handle(uuid, status);
    }

    @Override
    public String deriveEscrowPda(String remittanceId) {
        requireNonNull(remittanceId, "remittanceId must not be null");
        var uuid = UUID.fromString(remittanceId);
        var pda = escrowInstructionBuilder.deriveEscrowPda(uuid);
        log.info("Derived escrow PDA {} for remittance {}", pda.toBase58(), remittanceId);
        return pda.toBase58();
    }

    @Override
    public void updateRemittanceStatusWithEscrowPda(String remittanceId, RemittanceStatus status, String escrowPda) {
        requireNonNull(remittanceId, "remittanceId must not be null");
        requireNonNull(status, "status must not be null");
        requireNonNull(escrowPda, "escrowPda must not be null");
        var uuid = UUID.fromString(remittanceId);
        updateRemittanceStatusHandler.handle(uuid, status, escrowPda);
    }

    private String maskPhone(String phone) {
        if (phone.length() <= 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
