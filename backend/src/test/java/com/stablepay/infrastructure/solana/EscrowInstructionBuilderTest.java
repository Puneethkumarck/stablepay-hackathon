package com.stablepay.infrastructure.solana;

import static com.stablepay.testutil.SolanaFixtures.SOME_AMOUNT_LAMPORTS;
import static com.stablepay.testutil.SolanaFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.SolanaFixtures.SOME_CLAIM_AUTHORITY;
import static com.stablepay.testutil.SolanaFixtures.SOME_EXPIRY_TIMESTAMP;
import static com.stablepay.testutil.SolanaFixtures.SOME_PROGRAM_ID;
import static com.stablepay.testutil.SolanaFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.SolanaFixtures.SOME_SENDER_WALLET;
import static com.stablepay.testutil.SolanaFixtures.SOME_USDC_MINT;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sol4k.AccountMeta;
import org.sol4k.PublicKey;

class EscrowInstructionBuilderTest {

    private EscrowInstructionBuilder builder;

    @BeforeEach
    void setUp() {
        var properties = new SolanaProperties(
                new PublicKey(SOME_PROGRAM_ID),
                new PublicKey(SOME_USDC_MINT),
                "");
        builder = new EscrowInstructionBuilder(properties);
    }

    @Nested
    class DepositInstruction {

        @Test
        void shouldBuildDepositInstructionWithCorrectProgramId() {
            // given
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            var claimAuthority = new PublicKey(SOME_CLAIM_AUTHORITY);

            // when
            var instruction = builder.buildDepositInstruction(
                    SOME_REMITTANCE_ID, senderWallet, claimAuthority,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP);

            // then
            assertThat(instruction.getProgramId()).isEqualTo(new PublicKey(SOME_PROGRAM_ID));
        }

        @Test
        void shouldBuildDepositInstructionWithCorrectAccountCount() {
            // given
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            var claimAuthority = new PublicKey(SOME_CLAIM_AUTHORITY);

            // when
            var instruction = builder.buildDepositInstruction(
                    SOME_REMITTANCE_ID, senderWallet, claimAuthority,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP);

            // then
            assertThat(instruction.getKeys()).hasSize(10);
        }

        @Test
        void shouldBuildDepositInstructionWithSenderAsSignerAndWritable() {
            // given
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);
            var claimAuthority = new PublicKey(SOME_CLAIM_AUTHORITY);

            // when
            var instruction = builder.buildDepositInstruction(
                    SOME_REMITTANCE_ID, senderWallet, claimAuthority,
                    SOME_AMOUNT_USDC, SOME_EXPIRY_TIMESTAMP);

            // then
            var expected = AccountMeta.signerAndWritable(senderWallet);
            assertThat(instruction.getKeys().getFirst())
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldBuildDepositDataWithCorrectDiscriminator() throws Exception {
            // given
            var remittanceIdBytes = EscrowInstructionBuilder.uuidToBytes(SOME_REMITTANCE_ID);

            // when
            var data = builder.buildDepositData(
                    remittanceIdBytes, SOME_AMOUNT_LAMPORTS, SOME_EXPIRY_TIMESTAMP);

            // then
            var expectedDiscriminator = Arrays.copyOfRange(
                    MessageDigest.getInstance("SHA-256").digest("global:deposit".getBytes()), 0, 8);
            var actualDiscriminator = Arrays.copyOfRange(data, 0, 8);
            assertThat(actualDiscriminator).isEqualTo(expectedDiscriminator);
        }

        @Test
        void shouldBuildDepositDataWithCorrectLayout() {
            // given
            var remittanceIdBytes = EscrowInstructionBuilder.uuidToBytes(SOME_REMITTANCE_ID);

            // when
            var data = builder.buildDepositData(
                    remittanceIdBytes, SOME_AMOUNT_LAMPORTS, SOME_EXPIRY_TIMESTAMP);

            // then
            assertThat(data).hasSize(8 + 16 + 8 + 8);

            var buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            buffer.position(8);
            var actualRemittanceId = new byte[16];
            buffer.get(actualRemittanceId);
            assertThat(actualRemittanceId).isEqualTo(remittanceIdBytes);

            var actualAmount = buffer.getLong();
            assertThat(actualAmount).isEqualTo(SOME_AMOUNT_LAMPORTS);

            var actualExpiry = buffer.getLong();
            assertThat(actualExpiry).isEqualTo(SOME_EXPIRY_TIMESTAMP);
        }
    }

    @Nested
    class ClaimInstruction {

        @Test
        void shouldBuildClaimInstructionWithCorrectProgramId() {
            // given
            var claimAuthority = new PublicKey(SOME_CLAIM_AUTHORITY);
            var destination = new PublicKey("DestToken111111111111111111111111111111111");

            // when
            var instruction = builder.buildClaimInstruction(
                    SOME_REMITTANCE_ID, claimAuthority, destination);

            // then
            assertThat(instruction.getProgramId()).isEqualTo(new PublicKey(SOME_PROGRAM_ID));
        }

        @Test
        void shouldBuildClaimInstructionWithCorrectAccountCount() {
            // given
            var claimAuthority = new PublicKey(SOME_CLAIM_AUTHORITY);
            var destination = new PublicKey("DestToken111111111111111111111111111111111");

            // when
            var instruction = builder.buildClaimInstruction(
                    SOME_REMITTANCE_ID, claimAuthority, destination);

            // then
            assertThat(instruction.getKeys()).hasSize(6);
        }

        @Test
        void shouldBuildClaimDataWithOnlyDiscriminator() {
            // when
            var data = builder.buildClaimData();

            // then
            assertThat(data).hasSize(8);
            var expectedDiscriminator = builder.anchorDiscriminator("global:claim");
            assertThat(data).isEqualTo(expectedDiscriminator);
        }
    }

    @Nested
    class RefundInstruction {

        @Test
        void shouldBuildRefundInstructionWithCorrectProgramId() {
            // given
            var claimAuthority = new PublicKey(SOME_CLAIM_AUTHORITY);
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);

            // when
            var instruction = builder.buildRefundInstruction(
                    SOME_REMITTANCE_ID, claimAuthority, senderWallet);

            // then
            assertThat(instruction.getProgramId()).isEqualTo(new PublicKey(SOME_PROGRAM_ID));
        }

        @Test
        void shouldBuildRefundInstructionWithCorrectAccountCount() {
            // given
            var claimAuthority = new PublicKey(SOME_CLAIM_AUTHORITY);
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);

            // when
            var instruction = builder.buildRefundInstruction(
                    SOME_REMITTANCE_ID, claimAuthority, senderWallet);

            // then
            assertThat(instruction.getKeys()).hasSize(7);
        }

        @Test
        void shouldBuildRefundInstructionWithClaimAuthorityAsSigner() {
            // given
            var claimAuthority = new PublicKey(SOME_CLAIM_AUTHORITY);
            var senderWallet = new PublicKey(SOME_SENDER_WALLET);

            // when
            var instruction = builder.buildRefundInstruction(
                    SOME_REMITTANCE_ID, claimAuthority, senderWallet);

            // then
            var expected = AccountMeta.signerAndWritable(claimAuthority);
            assertThat(instruction.getKeys().getFirst())
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldBuildRefundDataWithOnlyDiscriminator() {
            // when
            var data = builder.buildRefundData();

            // then
            assertThat(data).hasSize(8);
            var expectedDiscriminator = builder.anchorDiscriminator("global:refund");
            assertThat(data).isEqualTo(expectedDiscriminator);
        }
    }

    @Nested
    class Utility {

        @Test
        void shouldConvertUsdcToLamportsCorrectly() {
            // given
            var amount = new BigDecimal("100.50");

            // when
            var lamports = EscrowInstructionBuilder.usdcToLamports(amount);

            // then
            assertThat(lamports).isEqualTo(100_500_000L);
        }

        @Test
        void shouldConvertUuidToBytesCorrectly() {
            // given
            var uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            // when
            var bytes = EscrowInstructionBuilder.uuidToBytes(uuid);

            // then
            assertThat(bytes).hasSize(16);
            var reconstructed = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            var mostSig = reconstructed.getLong();
            var leastSig = reconstructed.getLong();
            assertThat(new UUID(mostSig, leastSig)).isEqualTo(uuid);
        }

        @Test
        void shouldProduceDeterministicAnchorDiscriminator() {
            // when
            var discriminator1 = builder.anchorDiscriminator("global:deposit");
            var discriminator2 = builder.anchorDiscriminator("global:deposit");

            // then
            assertThat(discriminator1).isEqualTo(discriminator2);
            assertThat(discriminator1).hasSize(8);
        }

        @Test
        void shouldProduceDifferentDiscriminatorsForDifferentInstructions() {
            // when
            var depositDisc = builder.anchorDiscriminator("global:deposit");
            var claimDisc = builder.anchorDiscriminator("global:claim");
            var refundDisc = builder.anchorDiscriminator("global:refund");

            // then
            assertThat(depositDisc).isNotEqualTo(claimDisc);
            assertThat(depositDisc).isNotEqualTo(refundDisc);
            assertThat(claimDisc).isNotEqualTo(refundDisc);
        }

        @Test
        void shouldDeriveConsistentEscrowPda() {
            // given
            var remittanceIdBytes = EscrowInstructionBuilder.uuidToBytes(SOME_REMITTANCE_ID);

            // when
            var pda1 = builder.deriveEscrowPda(remittanceIdBytes);
            var pda2 = builder.deriveEscrowPda(remittanceIdBytes);

            // then
            assertThat(pda1).isEqualTo(pda2);
        }

        @Test
        void shouldDeriveDifferentPdasForDifferentRemittances() {
            // given
            var id1Bytes = EscrowInstructionBuilder.uuidToBytes(SOME_REMITTANCE_ID);
            var id2Bytes = EscrowInstructionBuilder.uuidToBytes(
                    UUID.fromString("660e8400-e29b-41d4-a716-446655440001"));

            // when
            var pda1 = builder.deriveEscrowPda(id1Bytes);
            var pda2 = builder.deriveEscrowPda(id2Bytes);

            // then
            assertThat(pda1).isNotEqualTo(pda2);
        }
    }
}
