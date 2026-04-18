package com.stablepay.infrastructure.solana;

import static com.stablepay.testutil.SolanaFixtures.SOME_BLOCKHASH;
import static com.stablepay.testutil.SolanaFixtures.SOME_DESTINATION_WALLET;
import static com.stablepay.testutil.SolanaFixtures.SOME_PROGRAM_ID;
import static com.stablepay.testutil.SolanaFixtures.SOME_TRANSACTION_SIGNATURE;
import static com.stablepay.testutil.SolanaFixtures.SOME_TREASURY_ADDRESS;
import static com.stablepay.testutil.SolanaFixtures.SOME_TREASURY_PRIVATE_KEY;
import static com.stablepay.testutil.SolanaFixtures.SOME_USDC_MINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sol4k.Base58;
import org.sol4k.Connection;
import org.sol4k.Keypair;
import org.sol4k.PublicKey;
import org.sol4k.api.AccountInfo;
import org.sol4k.api.TokenAccountBalance;

import com.stablepay.domain.remittance.exception.SolanaTransactionException;

@ExtendWith(MockitoExtension.class)
class TreasuryServiceAdapterTest {

    private static final byte[] SYSTEM_PROGRAM_ID =
            new PublicKey("11111111111111111111111111111111").bytes();
    private static final byte[] SPL_TOKEN_PROGRAM_ID =
            new PublicKey("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA").bytes();
    private static final byte[] ASSOCIATED_TOKEN_PROGRAM_ID =
            new PublicKey("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL").bytes();

    @Mock
    private Connection solanaConnection;

    @Captor
    private ArgumentCaptor<byte[]> txBytesCaptor;

    private SolanaProperties solanaProperties;
    private TreasuryProperties configuredTreasury;
    private TreasuryProperties unconfiguredTreasury;
    private PublicKey treasuryPubkey;
    private PublicKey usdcMint;

    private static boolean containsBytes(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || needle.length > haystack.length) {
            return false;
        }
        outer:
        for (var i = 0; i <= haystack.length - needle.length; i++) {
            for (var j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    @BeforeEach
    void setUp() {
        solanaProperties = new SolanaProperties(
                new PublicKey(SOME_PROGRAM_ID),
                new PublicKey(SOME_USDC_MINT),
                "",
                "http://localhost:8899");
        configuredTreasury = new TreasuryProperties(SOME_TREASURY_PRIVATE_KEY);
        unconfiguredTreasury = new TreasuryProperties("");
        treasuryPubkey = Keypair.fromSecretKey(Base58.decode(SOME_TREASURY_PRIVATE_KEY)).getPublicKey();
        usdcMint = new PublicKey(SOME_USDC_MINT);
    }

    @Nested
    class TransferSol {

        @Test
        void shouldSignAndSubmitSolTransfer() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, configuredTreasury);
            given(solanaConnection.getLatestBlockhash()).willReturn(SOME_BLOCKHASH);
            given(solanaConnection.sendTransaction(txBytesCaptor.capture()))
                    .willReturn(SOME_TRANSACTION_SIGNATURE);

            // when
            adapter.transferSol(SOME_DESTINATION_WALLET, 500_000L);

            // then
            var txBytes = txBytesCaptor.getValue();
            assertThat(txBytes[0]).isEqualTo((byte) 1);
            assertThat(txBytes.length).isGreaterThan(65);
            assertThat(containsBytes(txBytes, SYSTEM_PROGRAM_ID))
                    .as("SOL transfer must reference the System Program")
                    .isTrue();
        }

        @Test
        void shouldThrowWhenTreasuryNotConfigured() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, unconfiguredTreasury);

            // when / then
            assertThatThrownBy(() -> adapter.transferSol(SOME_DESTINATION_WALLET, 500_000L))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0015");
        }
    }

    @Nested
    class TransferUsdc {

        @Test
        void shouldSignAndSubmitUsdcTransferUsingSplInstruction() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, configuredTreasury);
            given(solanaConnection.getLatestBlockhash()).willReturn(SOME_BLOCKHASH);
            given(solanaConnection.sendTransaction(txBytesCaptor.capture()))
                    .willReturn(SOME_TRANSACTION_SIGNATURE);

            // when
            adapter.transferUsdc(SOME_DESTINATION_WALLET, new BigDecimal("12.500000"));

            // then
            var txBytes = txBytesCaptor.getValue();
            assertThat(txBytes[0]).isEqualTo((byte) 1);
            assertThat(txBytes.length).isGreaterThan(65);
            assertThat(containsBytes(txBytes, SPL_TOKEN_PROGRAM_ID))
                    .as("USDC transfer must reference the SPL Token Program")
                    .isTrue();
        }

        @Test
        void shouldThrowWhenTreasuryNotConfigured() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, unconfiguredTreasury);

            // when / then
            assertThatThrownBy(() -> adapter.transferUsdc(
                    SOME_DESTINATION_WALLET, new BigDecimal("10")))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0015");
        }
    }

    @Nested
    class GetSolBalance {

        @Test
        void shouldReturnSolBalanceConvertedFromLamports() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, configuredTreasury);
            given(solanaConnection.getBalance(new PublicKey(SOME_TREASURY_ADDRESS)))
                    .willReturn(BigInteger.valueOf(2_500_000_000L));

            // when
            var result = adapter.getSolBalance(SOME_TREASURY_ADDRESS);

            // then
            assertThat(result).isEqualByComparingTo(new BigDecimal("2.500000000"));
        }

        @Test
        void shouldThrowWhenBalanceQueryFails() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, configuredTreasury);
            given(solanaConnection.getBalance(new PublicKey(SOME_TREASURY_ADDRESS)))
                    .willThrow(new RuntimeException("rpc down"));

            // when / then
            assertThatThrownBy(() -> adapter.getSolBalance(SOME_TREASURY_ADDRESS))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0010");
        }
    }

    @Nested
    class GetUsdcBalance {

        @Test
        void shouldReturnUsdcBalanceScaledFromBaseUnits() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, configuredTreasury);
            var ata = PublicKey.findProgramDerivedAddress(
                    new PublicKey(SOME_TREASURY_ADDRESS), usdcMint).getPublicKey();
            given(solanaConnection.getTokenAccountBalance(ata))
                    .willReturn(new TokenAccountBalance(
                            BigInteger.valueOf(123_456_789L), 6, "123.456789"));

            // when
            var result = adapter.getUsdcBalance(SOME_TREASURY_ADDRESS);

            // then
            assertThat(result).isEqualByComparingTo(new BigDecimal("123.456789"));
        }

        @Test
        void shouldReturnZeroWhenAtaDoesNotExist() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, configuredTreasury);
            var ata = PublicKey.findProgramDerivedAddress(
                    new PublicKey(SOME_DESTINATION_WALLET), usdcMint).getPublicKey();
            given(solanaConnection.getTokenAccountBalance(ata))
                    .willThrow(new RuntimeException("ata not found"));

            // when
            var result = adapter.getUsdcBalance(SOME_DESTINATION_WALLET);

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class CreateAtaIfNeeded {

        @Test
        void shouldSubmitCreateAtaTransactionWhenAccountMissing() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, configuredTreasury);
            var ata = PublicKey.findProgramDerivedAddress(
                    new PublicKey(SOME_DESTINATION_WALLET), usdcMint).getPublicKey();
            given(solanaConnection.getAccountInfo(ata)).willReturn(null);
            given(solanaConnection.getLatestBlockhash()).willReturn(SOME_BLOCKHASH);
            given(solanaConnection.sendTransaction(txBytesCaptor.capture()))
                    .willReturn(SOME_TRANSACTION_SIGNATURE);

            // when
            adapter.createAtaIfNeeded(SOME_DESTINATION_WALLET);

            // then
            var txBytes = txBytesCaptor.getValue();
            assertThat(txBytes[0]).isEqualTo((byte) 1);
            assertThat(txBytes.length).isGreaterThan(65);
            assertThat(containsBytes(txBytes, ASSOCIATED_TOKEN_PROGRAM_ID))
                    .as("ATA creation must reference the Associated Token Program")
                    .isTrue();
        }

        @Test
        void shouldBeNoOpWhenAtaAlreadyExists() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, configuredTreasury);
            var ata = PublicKey.findProgramDerivedAddress(
                    new PublicKey(SOME_DESTINATION_WALLET), usdcMint).getPublicKey();
            var existingAccount = new AccountInfo(
                    new byte[0], false, BigInteger.valueOf(2_039_280L),
                    treasuryPubkey, BigInteger.ZERO, 165);
            given(solanaConnection.getAccountInfo(ata)).willReturn(existingAccount);

            // when
            adapter.createAtaIfNeeded(SOME_DESTINATION_WALLET);

            // then
            then(solanaConnection).should().getAccountInfo(ata);
            then(solanaConnection).shouldHaveNoMoreInteractions();
        }

        @Test
        void shouldThrowWhenTreasuryNotConfigured() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, unconfiguredTreasury);

            // when / then
            assertThatThrownBy(() -> adapter.createAtaIfNeeded(SOME_DESTINATION_WALLET))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0015");
        }

        @Test
        void shouldPropagateAccountInfoFailureInsteadOfAttemptingDuplicateCreate() {
            // given
            var adapter = new TreasuryServiceAdapter(
                    solanaConnection, solanaProperties, configuredTreasury);
            var ata = PublicKey.findProgramDerivedAddress(
                    new PublicKey(SOME_DESTINATION_WALLET), usdcMint).getPublicKey();
            given(solanaConnection.getAccountInfo(ata))
                    .willThrow(new RuntimeException("rpc rate limited"));

            // when / then
            assertThatThrownBy(() -> adapter.createAtaIfNeeded(SOME_DESTINATION_WALLET))
                    .isInstanceOf(SolanaTransactionException.class)
                    .hasMessageContaining("SP-0010");
            then(solanaConnection).should().getAccountInfo(ata);
            then(solanaConnection).shouldHaveNoMoreInteractions();
        }
    }
}
