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
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SolanaTransactionServiceAdapter implements SolanaTransactionService {

    private final Connection solanaConnection;
    private final EscrowInstructionBuilder escrowInstructionBuilder;
    private final SolanaProperties solanaProperties;
    private final MpcWalletClient mpcWalletClient;
    private final WalletRepository walletRepository;
    private final String solanaRpcUrl;

    public SolanaTransactionServiceAdapter(
            Connection solanaConnection,
            EscrowInstructionBuilder escrowInstructionBuilder,
            SolanaProperties solanaProperties,
            MpcWalletClient mpcWalletClient,
            WalletRepository walletRepository,
            @org.springframework.beans.factory.annotation.Value("${stablepay.solana.rpc-url:https://api.devnet.solana.com}") String solanaRpcUrl) {
        this.solanaConnection = solanaConnection;
        this.escrowInstructionBuilder = escrowInstructionBuilder;
        this.solanaProperties = solanaProperties;
        this.mpcWalletClient = mpcWalletClient;
        this.walletRepository = walletRepository;
        this.solanaRpcUrl = solanaRpcUrl;
    }

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
            var claimAuthority = resolveClaimAuthorityKeypair().getPublicKey();

            var instruction = escrowInstructionBuilder.buildDepositInstruction(
                    remittanceId, senderWallet, claimAuthority, amountUsdc, expiryTimestamp);

            var wallet = walletRepository.findBySolanaAddress(senderWalletAddress)
                    .orElseThrow(() -> SolanaTransactionException.submissionFailed(
                            "deposit:" + remittanceId,
                            new IllegalStateException("No wallet found for address: " + senderWalletAddress)));

            var blockhash = solanaConnection.getLatestBlockhash();
            var message = TransactionMessage.newMessage(senderWallet, blockhash, instruction);
            var messageBytes = message.serialize();
            var mpcSignature = mpcWalletClient.signTransaction(
                    messageBytes, wallet.keyShareData(), wallet.peerKeyShareData());
            if (mpcSignature == null || mpcSignature.length != 64) {
                throw SolanaTransactionException.submissionFailed(
                        "deposit:" + remittanceId,
                        new IllegalStateException("Invalid MPC signature: expected 64 bytes, got "
                                + (mpcSignature == null ? "null" : mpcSignature.length)));
            }
            // Build the signed transaction bytes manually:
            // wire format = compact_array(signatures) + serialized_message
            var sigCountByte = new byte[]{1}; // 1 signature (compact-u16)
            var txBytes = new byte[sigCountByte.length + mpcSignature.length + messageBytes.length];
            System.arraycopy(sigCountByte, 0, txBytes, 0, 1);
            System.arraycopy(mpcSignature, 0, txBytes, 1, 64);
            System.arraycopy(messageBytes, 0, txBytes, 65, messageBytes.length);

            log.info("Deposit transaction for remittance {} signed via MPC ({} bytes)",
                    remittanceId, txBytes.length);

            var signature = sendWithSkipPreflight(txBytes);
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

            var signature = sendWithSkipPreflight(transaction.serialize());
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
    public String refundEscrow(UUID remittanceId, String senderWalletAddress) {
        log.info("Submitting escrow refund for remittance {}", remittanceId);

        try {
            var claimAuthorityKeypair = resolveClaimAuthorityKeypair();
            var senderWallet = new PublicKey(senderWalletAddress);

            var instruction = escrowInstructionBuilder.buildRefundInstruction(
                    remittanceId, claimAuthorityKeypair.getPublicKey(), senderWallet);

            var blockhash = solanaConnection.getLatestBlockhash();
            var message = TransactionMessage.newMessage(
                    claimAuthorityKeypair.getPublicKey(), blockhash, instruction);
            var transaction = new VersionedTransaction(message);
            transaction.sign(claimAuthorityKeypair);

            var signature = sendWithSkipPreflight(transaction.serialize());
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

    private String sendWithSkipPreflight(byte[] txBytes) {
        try {
            return solanaConnection.sendTransaction(txBytes);
        } catch (Exception preflightEx) {
            log.warn("Preflight failed, retrying with skipPreflight: {}", preflightEx.getMessage());
            var txBase64 = java.util.Base64.getEncoder().encodeToString(txBytes);
            try {
                var url = new java.net.URI(solanaRpcUrl).toURL();
                var conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                var body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"sendTransaction\",\"params\":[\""
                        + txBase64 + "\",{\"skipPreflight\":true,\"encoding\":\"base64\"}]}";
                conn.getOutputStream().write(body.getBytes());

                var response = new String(conn.getInputStream().readAllBytes());
                var resultStart = response.indexOf("\"result\":\"");
                if (resultStart < 0) {
                    throw new RuntimeException("RPC error: " + response);
                }
                var sigStart = resultStart + "\"result\":\"".length();
                var sigEnd = response.indexOf("\"", sigStart);
                return response.substring(sigStart, sigEnd);
            } catch (Exception e) {
                throw SolanaTransactionException.submissionFailed("sendWithSkipPreflight", e);
            }
        }
    }

    private Keypair resolveClaimAuthorityKeypair() {
        var privateKeyStr = solanaProperties.claimAuthorityPrivateKey();
        if (privateKeyStr == null || privateKeyStr.isBlank()) {
            throw SolanaTransactionException.claimAuthorityNotConfigured();
        }
        return Keypair.fromSecretKey(Base58.decode(privateKeyStr));
    }
}
