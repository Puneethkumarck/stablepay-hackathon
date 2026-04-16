package com.stablepay.infrastructure.solana;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.sol4k.AccountMeta;
import org.sol4k.PublicKey;
import org.sol4k.instruction.BaseInstruction;
import org.sol4k.instruction.Instruction;
import org.springframework.stereotype.Component;

import com.stablepay.domain.remittance.exception.SolanaTransactionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EscrowInstructionBuilder {

    private static final PublicKey TOKEN_PROGRAM_ID =
            new PublicKey("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA");
    private static final PublicKey SYSTEM_PROGRAM_ID =
            new PublicKey("11111111111111111111111111111111");
    private static final byte[] PDA_MARKER = "ProgramDerivedAddress".getBytes();

    private static final int USDC_DECIMALS = 6;
    private static final byte[] ESCROW_SEED_PREFIX = "escrow".getBytes();

    private final SolanaProperties solanaProperties;

    public Instruction buildDepositInstruction(
            UUID remittanceId,
            PublicKey senderWallet,
            PublicKey claimAuthority,
            BigDecimal amountUsdc,
            long expiryTimestamp) {

        var remittanceIdPubkey = uuidToPublicKey(remittanceId);
        var escrowPda = deriveEscrowPda(remittanceIdPubkey.bytes());
        var senderAta = deriveAssociatedTokenAddress(senderWallet, solanaProperties.usdcMint());
        var vaultPda = deriveVaultPda(escrowPda);
        var lamports = usdcToLamports(amountUsdc);

        var data = buildDepositData(lamports, expiryTimestamp);

        // Account order must match Anchor Deposit struct exactly
        var keys = List.of(
                AccountMeta.signerAndWritable(senderWallet),       // sender
                AccountMeta.writable(escrowPda),                   // escrow (init)
                AccountMeta.writable(vaultPda),                    // vault (init)
                AccountMeta.writable(senderAta),                   // sender_token
                new AccountMeta(solanaProperties.usdcMint(), false, false),  // usdc_mint
                new AccountMeta(claimAuthority, false, false),     // claim_authority
                new AccountMeta(remittanceIdPubkey, false, false), // remittance_id
                new AccountMeta(SYSTEM_PROGRAM_ID, false, false),  // system_program
                new AccountMeta(TOKEN_PROGRAM_ID, false, false));  // token_program

        log.debug("Built deposit instruction for remittance {} with escrow PDA {}, remittanceIdPubkey {}",
                remittanceId, escrowPda.toBase58(), remittanceIdPubkey.toBase58());

        return new BaseInstruction(data, keys, solanaProperties.escrowProgramId());
    }

    public Instruction buildClaimInstruction(
            UUID remittanceId,
            PublicKey claimAuthority,
            PublicKey destinationTokenAccount,
            PublicKey senderWallet) {

        var remittanceIdPubkey = uuidToPublicKey(remittanceId);
        var escrowPda = deriveEscrowPda(remittanceIdPubkey.bytes());
        var vaultPda = deriveVaultPda(escrowPda);

        var data = buildClaimData();

        // Account order must match Anchor Claim struct exactly
        var keys = List.of(
                new AccountMeta(claimAuthority, true, false),     // claim_authority (signer)
                AccountMeta.writable(escrowPda),                  // escrow (mut, close)
                AccountMeta.writable(vaultPda),                   // vault (mut)
                AccountMeta.writable(destinationTokenAccount),    // recipient_token (mut)
                AccountMeta.writable(senderWallet),               // sender (mut, receives rent)
                new AccountMeta(TOKEN_PROGRAM_ID, false, false)); // token_program

        log.debug("Built claim instruction for remittance {} with destination {}",
                remittanceId, destinationTokenAccount.toBase58());

        return new BaseInstruction(data, keys, solanaProperties.escrowProgramId());
    }

    public Instruction buildRefundInstruction(
            UUID remittanceId, PublicKey claimAuthority, PublicKey senderWallet) {

        var remittanceIdPubkey = uuidToPublicKey(remittanceId);
        var escrowPda = deriveEscrowPda(remittanceIdPubkey.bytes());
        var vaultPda = deriveVaultPda(escrowPda);
        var senderAta = deriveAssociatedTokenAddress(senderWallet, solanaProperties.usdcMint());

        var data = buildRefundData();

        // Account order must match Anchor Refund struct exactly
        var keys = List.of(
                AccountMeta.signerAndWritable(claimAuthority),    // payer (mut, signer)
                AccountMeta.writable(escrowPda),                  // escrow (mut, close)
                AccountMeta.writable(vaultPda),                   // vault (mut)
                AccountMeta.writable(senderWallet),               // sender (mut, receives rent)
                AccountMeta.writable(senderAta),                  // sender_token (mut)
                new AccountMeta(TOKEN_PROGRAM_ID, false, false)); // token_program

        log.debug("Built refund instruction for remittance {} returning to {}",
                remittanceId, senderWallet.toBase58());

        return new BaseInstruction(data, keys, solanaProperties.escrowProgramId());
    }

    public PublicKey deriveEscrowPda(byte[] remittanceIdPubkeyBytes) {
        try {
            return findProgramDerivedAddress(
                    List.of(ESCROW_SEED_PREFIX, remittanceIdPubkeyBytes),
                    solanaProperties.escrowProgramId());
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.pdaDerivationFailed("escrow", e);
        }
    }

    public PublicKey deriveAssociatedTokenAddress(PublicKey owner, PublicKey mint) {
        try {
            return PublicKey.findProgramDerivedAddress(owner, mint)
                    .getPublicKey();
        } catch (Exception e) {
            throw SolanaTransactionException.pdaDerivationFailed(
                    "ata:" + owner.toBase58() + ":" + mint.toBase58(), e);
        }
    }

    byte[] buildDepositData(long lamports, long expiryTimestamp) {
        var discriminator = anchorDiscriminator("global:deposit");
        var buffer = ByteBuffer.allocate(8 + 8 + 8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(discriminator)
                .putLong(lamports)
                .putLong(expiryTimestamp);
        return buffer.array();
    }

    PublicKey uuidToPublicKey(UUID uuid) {
        var bytes = new byte[32];
        var uuidBytes = uuidToBytes(uuid);
        System.arraycopy(uuidBytes, 0, bytes, 0, 16);
        return new PublicKey(bytes);
    }

    public PublicKey deriveVaultPda(PublicKey escrowPda) {
        try {
            return findProgramDerivedAddress(
                    List.of("vault".getBytes(), escrowPda.bytes()),
                    solanaProperties.escrowProgramId());
        } catch (SolanaTransactionException e) {
            throw e;
        } catch (Exception e) {
            throw SolanaTransactionException.pdaDerivationFailed(
                    "vault:" + escrowPda.toBase58(), e);
        }
    }

    byte[] buildClaimData() {
        return anchorDiscriminator("global:claim");
    }

    byte[] buildRefundData() {
        return anchorDiscriminator("global:refund");
    }

    byte[] anchorDiscriminator(String instructionName) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(instructionName.getBytes());
            return Arrays.copyOfRange(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    static PublicKey findProgramDerivedAddress(List<byte[]> seeds, PublicKey programId) {
        for (var nonce = 255; nonce >= 0; nonce--) {
            try {
                var address = createProgramAddress(seeds, (byte) nonce, programId);
                return address;
            } catch (IllegalArgumentException e) {
                // nonce produces on-curve point, try next
            }
        }
        throw SolanaTransactionException.pdaDerivationFailed("exhausted all 256 nonces",
                new IllegalStateException("No valid off-curve PDA found for given seeds"));
    }

    private static PublicKey createProgramAddress(
            List<byte[]> seeds, byte nonce, PublicKey programId) {
        try {
            var totalSize = seeds.stream().mapToInt(s -> s.length).sum()
                    + 1 // nonce byte
                    + programId.bytes().length
                    + PDA_MARKER.length;

            var buffer = ByteBuffer.allocate(totalSize);
            seeds.forEach(buffer::put);
            buffer.put(nonce);
            buffer.put(programId.bytes());
            buffer.put(PDA_MARKER);

            var hash = MessageDigest.getInstance("SHA-256").digest(buffer.array());

            if (isOnCurve(hash)) {
                throw new IllegalArgumentException("Invalid seeds: address is on curve");
            }
            return new PublicKey(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static final BigInteger ED25519_P =
            BigInteger.TWO.pow(255).subtract(BigInteger.valueOf(19));
    private static final BigInteger ED25519_D = BigInteger.valueOf(-121665)
            .multiply(BigInteger.valueOf(121666).modInverse(ED25519_P))
            .mod(ED25519_P);

    private static boolean isOnCurve(byte[] point) {
        var yBytes = point.clone();
        yBytes[31] &= 0x7f;

        var reversed = new byte[32];
        for (var i = 0; i < 32; i++) {
            reversed[i] = yBytes[31 - i];
        }
        var y = new BigInteger(1, reversed);

        if (y.compareTo(ED25519_P) >= 0) {
            return false;
        }

        var y2 = y.modPow(BigInteger.TWO, ED25519_P);
        var numerator = y2.subtract(BigInteger.ONE).mod(ED25519_P);
        var denominator = ED25519_D.multiply(y2).add(BigInteger.ONE).mod(ED25519_P);
        var x2 = numerator.multiply(denominator.modInverse(ED25519_P)).mod(ED25519_P);

        var exp = ED25519_P.subtract(BigInteger.ONE).shiftRight(1);
        var legendreSymbol = x2.modPow(exp, ED25519_P);

        return legendreSymbol.equals(BigInteger.ONE) || x2.equals(BigInteger.ZERO);
    }

    static byte[] uuidToBytes(UUID uuid) {
        var buffer = ByteBuffer.allocate(16)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    static long usdcToLamports(BigDecimal amountUsdc) {
        return amountUsdc.movePointRight(USDC_DECIMALS).longValueExact();
    }
}
