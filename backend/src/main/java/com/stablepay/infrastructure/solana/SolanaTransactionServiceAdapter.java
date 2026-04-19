package com.stablepay.infrastructure.solana;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

import org.sol4k.Base58;
import org.sol4k.Connection;
import org.sol4k.Keypair;
import org.sol4k.PublicKey;
import org.sol4k.TransactionMessage;
import org.sol4k.VersionedTransaction;
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction;
import org.sol4k.instruction.Instruction;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.domain.remittance.exception.SolanaTransactionException;
import com.stablepay.domain.remittance.model.TransactionConfirmationStatus;
import com.stablepay.domain.remittance.port.SolanaTransactionService;
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.domain.wallet.port.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolanaTransactionServiceAdapter implements SolanaTransactionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Connection solanaConnection;
    private final EscrowInstructionBuilder escrowInstructionBuilder;
    private final SolanaProperties solanaProperties;
    private final MpcWalletClient mpcWalletClient;
    private final WalletRepository walletRepository;

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
    public String claimEscrow(UUID remittanceId, String destinationTokenAccount, String senderWalletAddress) {
        log.info("Submitting escrow claim for remittance {}", remittanceId);

        try {
            var claimAuthorityKeypair = resolveClaimAuthorityKeypair();
            var destination = new PublicKey(destinationTokenAccount);
            var senderWallet = new PublicKey(senderWalletAddress);

            var instructions = new ArrayList<Instruction>();

            // Create the recipient ATA if it doesn't exist on-chain
            if (!accountExists(destination)) {
                var claimAuthorityPubkey = claimAuthorityKeypair.getPublicKey();
                log.info("Recipient ATA {} does not exist, adding CreateAssociatedTokenAccount instruction",
                        destination.toBase58());
                instructions.add(new CreateAssociatedTokenAccountInstruction(
                        claimAuthorityPubkey, destination, claimAuthorityPubkey,
                        solanaProperties.usdcMint()));
            }

            instructions.add(escrowInstructionBuilder.buildClaimInstruction(
                    remittanceId, claimAuthorityKeypair.getPublicKey(), destination, senderWallet));

            var blockhash = solanaConnection.getLatestBlockhash();
            var message = TransactionMessage.newMessage(
                    claimAuthorityKeypair.getPublicKey(), blockhash, instructions);
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

    private boolean accountExists(PublicKey address) {
        try {
            var accountInfo = solanaConnection.getAccountInfo(address);
            return accountInfo != null;
        } catch (Exception e) {
            log.debug("Account {} does not exist or query failed: {}", address.toBase58(), e.getMessage());
            return false;
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
    public TransactionConfirmationStatus getTransactionStatus(String transactionSignature) {
        log.debug("Checking status for transaction {}", transactionSignature);

        HttpURLConnection conn = null;
        try {
            var url = new URI(solanaProperties.rpcUrl()).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            var body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getSignatureStatuses\","
                    + "\"params\":[[\"" + transactionSignature
                    + "\"],{\"searchTransactionHistory\":true}]}";

            try (var os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            try (var is = conn.getInputStream()) {
                var response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (response.contains("\"error\"")) {
                    log.warn("RPC error checking tx status for {}: {}", transactionSignature, response);
                    throw new RuntimeException("RPC error: " + response);
                }
                var status = parseSignatureStatusResponse(response);
                log.info("Transaction {} status: {}", transactionSignature, status);
                return status;
            }
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.submissionFailed(
                    "getSignatureStatuses:" + transactionSignature, readRpcErrorDetails(conn, e));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    static TransactionConfirmationStatus parseSignatureStatusResponse(String json) {
        try {
            var root = MAPPER.readTree(json);
            var value = root.path("result").path("value");
            if (!value.isArray() || value.isEmpty() || value.get(0).isNull()) {
                return TransactionConfirmationStatus.NOT_FOUND;
            }

            var entry = value.get(0);
            var err = entry.get("err");
            if (err != null && !err.isNull()) {
                return TransactionConfirmationStatus.FAILED_ON_CHAIN;
            }

            var confirmationStatus = entry.path("confirmationStatus").asText("");
            return switch (confirmationStatus) {
                case "finalized" -> TransactionConfirmationStatus.FINALIZED;
                case "confirmed" -> TransactionConfirmationStatus.CONFIRMED;
                case "processed" -> TransactionConfirmationStatus.PROCESSED;
                default -> TransactionConfirmationStatus.NOT_FOUND;
            };
        } catch (JsonProcessingException e) {
            throw SolanaTransactionException.submissionFailed("getSignatureStatuses:parse", e);
        }
    }

    private String sendWithSkipPreflight(byte[] txBytes) {
        try {
            return solanaConnection.sendTransaction(txBytes);
        } catch (Exception preflightEx) {
            log.warn("Preflight failed, retrying with skipPreflight: {}", preflightEx.getMessage());
            var txBase64 = java.util.Base64.getEncoder().encodeToString(txBytes);
            var conn = (java.net.HttpURLConnection) null;
            try {
                var url = new java.net.URI(solanaProperties.rpcUrl()).toURL();
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(30_000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                var body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"sendTransaction\",\"params\":[\""
                        + txBase64 + "\",{\"skipPreflight\":true,\"encoding\":\"base64\"}]}";

                try (var os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                try (var is = conn.getInputStream()) {
                    var response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    if (response.contains("\"error\"")) {
                        throw new RuntimeException("RPC error: " + response);
                    }
                    var resultStart = response.indexOf("\"result\":\"");
                    if (resultStart < 0) {
                        throw new RuntimeException("Unexpected RPC response: " + response);
                    }
                    var sigStart = resultStart + "\"result\":\"".length();
                    var sigEnd = response.indexOf("\"", sigStart);
                    return response.substring(sigStart, sigEnd);
                }
            } catch (SolanaTransactionException e) {
                throw e;
            } catch (Exception e) {
                throw SolanaTransactionException.submissionFailed("sendWithSkipPreflight",
                        readRpcErrorDetails(conn, e));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private Exception readRpcErrorDetails(java.net.HttpURLConnection conn, Exception original) {
        if (conn == null) {
            return original;
        }
        try (var errorStream = conn.getErrorStream()) {
            if (errorStream != null) {
                var errorBody = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                log.error("Solana RPC error response: {}", errorBody);
                return new RuntimeException("RPC error: " + errorBody, original);
            }
        } catch (Exception ignored) {
            log.warn("Failed to read RPC error stream: {}", ignored.getMessage());
        }
        return original;
    }

    private Keypair resolveClaimAuthorityKeypair() {
        var privateKeyStr = solanaProperties.claimAuthorityPrivateKey();
        if (privateKeyStr == null || privateKeyStr.isBlank()) {
            throw SolanaTransactionException.claimAuthorityNotConfigured();
        }
        return Keypair.fromSecretKey(Base58.decode(privateKeyStr));
    }
}
