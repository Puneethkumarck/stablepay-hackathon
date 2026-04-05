package com.stablepay.infrastructure.solana;

import java.math.BigDecimal;
import java.util.UUID;

import org.sol4k.Base58;
import org.sol4k.Connection;
import org.sol4k.Keypair;
import org.sol4k.PublicKey;
import org.sol4k.TransactionMessage;
import org.sol4k.VersionedTransaction;
import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.exception.SolanaTransactionException;
import com.stablepay.domain.remittance.port.SolanaTransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolanaTransactionServiceAdapter implements SolanaTransactionService {

    private final Connection solanaConnection;
    private final EscrowInstructionBuilder escrowInstructionBuilder;
    private final SolanaProperties solanaProperties;

    @Override
    public String depositEscrow(
            UUID remittanceId,
            String senderWalletAddress,
            BigDecimal amountUsdc,
            long expiryTimestamp) {

        log.info("Submitting escrow deposit for remittance {} amount {} USDC",
                remittanceId, amountUsdc);

        try {
            var senderWallet = new PublicKey(senderWalletAddress);
            var claimAuthority = deriveClaimAuthorityPublicKey();

            var instruction = escrowInstructionBuilder.buildDepositInstruction(
                    remittanceId, senderWallet, claimAuthority, amountUsdc, expiryTimestamp);

            var blockhash = solanaConnection.getLatestBlockhash();
            var message = TransactionMessage.newMessage(senderWallet, blockhash, instruction);
            var transaction = new VersionedTransaction(message);

            var signature = solanaConnection.sendTransaction(transaction);
            log.info("Escrow deposit submitted for remittance {} with signature {}",
                    remittanceId, signature);

            return signature;
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.submissionFailed(
                    "deposit:" + remittanceId, e);
        }
    }

    @Override
    public String claimEscrow(UUID remittanceId, String destinationTokenAccount) {
        log.info("Submitting escrow claim for remittance {}", remittanceId);

        try {
            var claimAuthorityKeypair = resolveClaimAuthorityKeypair();
            var destination = new PublicKey(destinationTokenAccount);

            var instruction = escrowInstructionBuilder.buildClaimInstruction(
                    remittanceId, claimAuthorityKeypair.getPublicKey(), destination);

            var blockhash = solanaConnection.getLatestBlockhash();
            var message = TransactionMessage.newMessage(
                    claimAuthorityKeypair.getPublicKey(), blockhash, instruction);
            var transaction = new VersionedTransaction(message);
            transaction.sign(claimAuthorityKeypair);

            var signature = solanaConnection.sendTransaction(transaction);
            log.info("Escrow claim submitted for remittance {} with signature {}",
                    remittanceId, signature);

            return signature;
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.submissionFailed(
                    "claim:" + remittanceId, e);
        }
    }

    @Override
    public String refundEscrow(UUID remittanceId) {
        log.info("Submitting escrow refund for remittance {}", remittanceId);

        try {
            var claimAuthorityKeypair = resolveClaimAuthorityKeypair();
            var senderWallet = claimAuthorityKeypair.getPublicKey();

            var instruction = escrowInstructionBuilder.buildRefundInstruction(
                    remittanceId, senderWallet);

            var blockhash = solanaConnection.getLatestBlockhash();
            var message = TransactionMessage.newMessage(
                    claimAuthorityKeypair.getPublicKey(), blockhash, instruction);
            var transaction = new VersionedTransaction(message);
            transaction.sign(claimAuthorityKeypair);

            var signature = solanaConnection.sendTransaction(transaction);
            log.info("Escrow refund submitted for remittance {} with signature {}",
                    remittanceId, signature);

            return signature;
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.submissionFailed(
                    "refund:" + remittanceId, e);
        }
    }

    @Override
    public String getTransactionStatus(String transactionSignature) {
        log.debug("Checking status for transaction {}", transactionSignature);

        // sol4k does not expose getSignatureStatuses RPC.
        // For now, return a stub status. Full confirmation polling
        // will be implemented via raw JSON-RPC in a follow-up.
        log.info("Transaction status check for {} (stub: returning CONFIRMED)", transactionSignature);
        return "CONFIRMED";
    }

    private PublicKey deriveClaimAuthorityPublicKey() {
        var privateKeyStr = solanaProperties.claimAuthorityPrivateKey();
        if (privateKeyStr == null || privateKeyStr.isBlank()) {
            return Keypair.generate().getPublicKey();
        }
        return Keypair.fromSecretKey(Base58.decode(privateKeyStr)).getPublicKey();
    }

    private Keypair resolveClaimAuthorityKeypair() {
        var privateKeyStr = solanaProperties.claimAuthorityPrivateKey();
        if (privateKeyStr == null || privateKeyStr.isBlank()) {
            log.warn("No claim authority private key configured, generating ephemeral keypair");
            return Keypair.generate();
        }
        return Keypair.fromSecretKey(Base58.decode(privateKeyStr));
    }
}
