package com.stablepay.infrastructure.solana;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.sol4k.Base58;
import org.sol4k.Connection;
import org.sol4k.Keypair;
import org.sol4k.PublicKey;
import org.sol4k.TransactionMessage;
import org.sol4k.VersionedTransaction;
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction;
import org.sol4k.instruction.SplTransferInstruction;
import org.sol4k.instruction.TransferInstruction;
import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.exception.SolanaTransactionException;
import com.stablepay.domain.wallet.port.TreasuryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TreasuryServiceAdapter implements TreasuryService {

    private static final int USDC_DECIMALS = 6;
    private static final BigDecimal USDC_SCALE = BigDecimal.TEN.pow(USDC_DECIMALS);
    private static final BigDecimal LAMPORTS_PER_SOL = BigDecimal.valueOf(1_000_000_000L);

    private final Connection solanaConnection;
    private final SolanaProperties solanaProperties;
    private final TreasuryProperties treasuryProperties;

    @Override
    public String transferSol(String destinationAddress, long lamports) {
        log.info("Treasury transferring {} lamports to {}", lamports, destinationAddress);

        try {
            var treasuryKeypair = resolveTreasuryKeypair();
            var destination = new PublicKey(destinationAddress);
            var instruction = new TransferInstruction(
                    treasuryKeypair.getPublicKey(), destination, lamports);

            var blockhash = solanaConnection.getLatestBlockhash();
            var message = TransactionMessage.newMessage(
                    treasuryKeypair.getPublicKey(), blockhash, instruction);
            var transaction = new VersionedTransaction(message);
            transaction.sign(treasuryKeypair);

            var opContext = "treasury-sol:" + destinationAddress;
            var signature = sendWithSkipPreflight(transaction.serialize(), opContext);
            log.info("Treasury SOL transfer submitted with signature {}", signature);
            return signature;
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.submissionFailed(
                    "treasury-sol:" + destinationAddress, e);
        }
    }

    @Override
    public String transferUsdc(String destinationAddress, BigDecimal amountUsdc) {
        log.info("Treasury transferring {} USDC to {}", amountUsdc, destinationAddress);

        try {
            var treasuryKeypair = resolveTreasuryKeypair();
            var treasuryPubkey = treasuryKeypair.getPublicKey();
            var destinationOwner = new PublicKey(destinationAddress);
            var usdcMint = solanaProperties.usdcMint();
            var treasuryAta = deriveAta(treasuryPubkey, usdcMint);
            var destinationAta = deriveAta(destinationOwner, usdcMint);
            var amountBaseUnits = amountUsdc.setScale(USDC_DECIMALS, RoundingMode.HALF_UP)
                    .multiply(USDC_SCALE)
                    .longValueExact();

            var instruction = new SplTransferInstruction(
                    treasuryAta, destinationAta, usdcMint, treasuryPubkey,
                    amountBaseUnits, USDC_DECIMALS);

            var blockhash = solanaConnection.getLatestBlockhash();
            var message = TransactionMessage.newMessage(treasuryPubkey, blockhash, instruction);
            var transaction = new VersionedTransaction(message);
            transaction.sign(treasuryKeypair);

            var opContext = "treasury-usdc:" + destinationAddress;
            var signature = sendWithSkipPreflight(transaction.serialize(), opContext);
            log.info("Treasury USDC transfer submitted with signature {}", signature);
            return signature;
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.submissionFailed(
                    "treasury-usdc:" + destinationAddress, e);
        }
    }

    @Override
    public BigDecimal getSolBalance(String address) {
        try {
            var lamports = solanaConnection.getBalance(new PublicKey(address));
            return new BigDecimal(lamports).divide(LAMPORTS_PER_SOL, 9, RoundingMode.DOWN);
        } catch (Exception e) {
            throw SolanaTransactionException.submissionFailed(
                    "treasury-sol-balance:" + address, e);
        }
    }

    // Strict balance query for authorization checks: propagates RPC failures as
    // SolanaTransactionException so callers can distinguish "treasury depleted"
    // from "RPC transiently unreachable". Does NOT use the fail-closed ZERO
    // sentinel that getUsdcBalance uses for arbitrary addresses.
    @Override
    public BigDecimal getTreasuryUsdcBalance() {
        var treasuryKeypair = resolveTreasuryKeypair();
        var treasuryPubkey = treasuryKeypair.getPublicKey();
        var ata = deriveAta(treasuryPubkey, solanaProperties.usdcMint());
        try {
            var balance = solanaConnection.getTokenAccountBalance(ata);
            return new BigDecimal(balance.getAmount())
                    .divide(USDC_SCALE, USDC_DECIMALS, RoundingMode.DOWN);
        } catch (Exception e) {
            throw SolanaTransactionException.submissionFailed(
                    "treasury-usdc-balance:" + treasuryPubkey.toBase58(), e);
        }
    }

    @Override
    public BigDecimal getUsdcBalance(String address) {
        PublicKey ata;
        try {
            ata = deriveAta(new PublicKey(address), solanaProperties.usdcMint());
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.pdaDerivationFailed(
                    "ata:" + address, e);
        }

        try {
            var balance = solanaConnection.getTokenAccountBalance(ata);
            return new BigDecimal(balance.getAmount())
                    .divide(USDC_SCALE, USDC_DECIMALS, RoundingMode.DOWN);
        } catch (Exception e) {
            log.warn("USDC balance query for {} (ata {}) failed: {} — treating as zero (fail-closed)",
                    address, ata.toBase58(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void createAtaIfNeeded(String ownerAddress) {
        try {
            var treasuryKeypair = resolveTreasuryKeypair();
            var owner = new PublicKey(ownerAddress);
            var usdcMint = solanaProperties.usdcMint();
            var ata = deriveAta(owner, usdcMint);

            if (accountExists(ata)) {
                log.debug("ATA {} for owner {} already exists — no-op", ata.toBase58(), ownerAddress);
                return;
            }

            log.info("Creating ATA {} for owner {}", ata.toBase58(), ownerAddress);
            var instruction = new CreateAssociatedTokenAccountInstruction(
                    treasuryKeypair.getPublicKey(), ata, owner, usdcMint);

            var blockhash = solanaConnection.getLatestBlockhash();
            var message = TransactionMessage.newMessage(
                    treasuryKeypair.getPublicKey(), blockhash, instruction);
            var transaction = new VersionedTransaction(message);
            transaction.sign(treasuryKeypair);

            var opContext = "treasury-ata:" + ownerAddress;
            var signature = sendWithSkipPreflight(transaction.serialize(), opContext);
            log.info("ATA creation for owner {} submitted with signature {}", ownerAddress, signature);
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.submissionFailed(
                    "treasury-ata:" + ownerAddress, e);
        }
    }

    private PublicKey deriveAta(PublicKey owner, PublicKey mint) {
        try {
            return PublicKey.findProgramDerivedAddress(owner, mint).getPublicKey();
        } catch (Exception e) {
            throw SolanaTransactionException.pdaDerivationFailed(
                    "ata:" + owner.toBase58() + ":" + mint.toBase58(), e);
        }
    }

    private boolean accountExists(PublicKey address) {
        return solanaConnection.getAccountInfo(address) != null;
    }

    private Keypair resolveTreasuryKeypair() {
        var privateKeyStr = treasuryProperties.privateKey();
        if (privateKeyStr == null || privateKeyStr.isBlank()) {
            throw SolanaTransactionException.treasuryNotConfigured();
        }
        return Keypair.fromSecretKey(Base58.decode(privateKeyStr));
    }

    private String sendWithSkipPreflight(byte[] txBytes, String opContext) {
        try {
            return solanaConnection.sendTransaction(txBytes);
        } catch (Exception preflightEx) {
            log.warn("Preflight failed for {}, retrying with skipPreflight: {}",
                    opContext, preflightEx.getMessage());
            var txBase64 = Base64.getEncoder().encodeToString(txBytes);
            HttpURLConnection conn = null;
            try {
                var url = new URI(solanaProperties.rpcUrl()).toURL();
                conn = (HttpURLConnection) url.openConnection();
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
            } catch (Exception e) {
                throw SolanaTransactionException.submissionFailed(
                        opContext + " (skipPreflight)", readRpcErrorDetails(conn, e));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private Exception readRpcErrorDetails(HttpURLConnection conn, Exception original) {
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
}
