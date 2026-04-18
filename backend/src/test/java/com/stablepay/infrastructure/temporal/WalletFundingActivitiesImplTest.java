package com.stablepay.infrastructure.temporal;

import static com.stablepay.testutil.WalletFundingFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_LOW_SOL;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_SENDER_ADDRESS;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_SUFFICIENT_SOL;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_TREASURY_BALANCE;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_TX_SIGNATURE;
import static com.stablepay.testutil.WalletFundingFixtures.SOME_WALLET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.funding.handler.FinalizeFundingHandler;
import com.stablepay.domain.remittance.exception.SolanaTransactionException;
import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.domain.wallet.port.TreasuryService;

@ExtendWith(MockitoExtension.class)
class WalletFundingActivitiesImplTest {

    private static final long SOL_TOP_UP_LAMPORTS = 10_000_000L;

    @Mock
    private TreasuryService treasuryService;

    @Mock
    private FinalizeFundingHandler finalizeFundingHandler;

    @InjectMocks
    private WalletFundingActivitiesImpl activities;

    @Test
    void shouldPassTreasuryCheckWhenBalanceIsSufficient() {
        // given
        given(treasuryService.getTreasuryUsdcBalance()).willReturn(SOME_TREASURY_BALANCE);

        // when
        activities.checkTreasuryBalance(SOME_AMOUNT_USDC);

        // then
        then(treasuryService).should().getTreasuryUsdcBalance();
        then(treasuryService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldThrowTreasuryDepletedWhenBalanceIsInsufficient() {
        // given
        var available = new BigDecimal("1.00");
        given(treasuryService.getTreasuryUsdcBalance()).willReturn(available);

        // when / then
        assertThatThrownBy(() -> activities.checkTreasuryBalance(SOME_AMOUNT_USDC))
                .isInstanceOf(TreasuryDepletedException.class)
                .hasMessageContaining("SP-0007");
    }

    @Test
    void shouldRejectNonPositiveAmountOnCheckTreasuryBalance() {
        // given
        var zero = BigDecimal.ZERO;

        // when / then
        assertThatThrownBy(() -> activities.checkTreasuryBalance(zero))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amountUsdc must be positive");
    }

    @Test
    void shouldRejectNegativeAmountOnTransferUsdc() {
        // given
        var negative = new BigDecimal("-1.00");

        // when / then
        assertThatThrownBy(() -> activities.transferUsdc(SOME_SENDER_ADDRESS, negative))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amountUsdc must be positive");
    }

    @Test
    void shouldRejectZeroAmountOnFinalizeFunding() {
        // given
        var zero = BigDecimal.ZERO;

        // when / then
        assertThatThrownBy(() -> activities.finalizeFunding(SOME_FUNDING_ID, SOME_WALLET_ID, zero))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amountUsdc must be positive");
    }

    @Test
    void shouldPropagateSolanaExceptionWhenBalanceQueryFails() {
        // given
        var rpcFailure = SolanaTransactionException.submissionFailed(
                "treasury-usdc-balance", new RuntimeException("RPC 503"));
        BDDMockito.willThrow(rpcFailure)
                .given(treasuryService).getTreasuryUsdcBalance();

        // when / then
        assertThatThrownBy(() -> activities.checkTreasuryBalance(SOME_AMOUNT_USDC))
                .isInstanceOf(SolanaTransactionException.class)
                .hasMessageContaining("SP-0010");
    }

    @Test
    void shouldSkipSolTopUpWhenSenderBalanceIsSufficient() {
        // given
        given(treasuryService.getSolBalance(SOME_SENDER_ADDRESS)).willReturn(SOME_SUFFICIENT_SOL);

        // when
        activities.ensureSolBalance(SOME_SENDER_ADDRESS);

        // then
        then(treasuryService).should().getSolBalance(SOME_SENDER_ADDRESS);
        then(treasuryService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldTopUpSolWhenSenderBalanceIsBelowThreshold() {
        // given
        given(treasuryService.getSolBalance(SOME_SENDER_ADDRESS)).willReturn(SOME_LOW_SOL);
        given(treasuryService.transferSol(SOME_SENDER_ADDRESS, SOL_TOP_UP_LAMPORTS))
                .willReturn(SOME_TX_SIGNATURE);

        // when
        activities.ensureSolBalance(SOME_SENDER_ADDRESS);

        // then
        then(treasuryService).should().getSolBalance(SOME_SENDER_ADDRESS);
        then(treasuryService).should().transferSol(SOME_SENDER_ADDRESS, SOL_TOP_UP_LAMPORTS);
    }

    @Test
    void shouldDelegateCreateAtaToTreasury() {
        // given

        // when
        activities.createAtaIfNeeded(SOME_SENDER_ADDRESS);

        // then
        then(treasuryService).should().createAtaIfNeeded(SOME_SENDER_ADDRESS);
    }

    @Test
    void shouldReturnSignatureFromUsdcTransfer() {
        // given
        given(treasuryService.transferUsdc(SOME_SENDER_ADDRESS, SOME_AMOUNT_USDC))
                .willReturn(SOME_TX_SIGNATURE);

        // when
        var signature = activities.transferUsdc(SOME_SENDER_ADDRESS, SOME_AMOUNT_USDC);

        // then
        assertThat(signature).isEqualTo(SOME_TX_SIGNATURE);
    }

    @Test
    void shouldDelegateFinalizeToDomainHandler() {
        // given

        // when
        activities.finalizeFunding(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC);

        // then
        then(finalizeFundingHandler).should()
                .handle(SOME_FUNDING_ID, SOME_WALLET_ID, SOME_AMOUNT_USDC);
    }
}
