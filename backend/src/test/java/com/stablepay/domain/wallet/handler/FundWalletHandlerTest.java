package com.stablepay.domain.wallet.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.port.TreasuryService;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.testutil.WalletFixtures;

@ExtendWith(MockitoExtension.class)
class FundWalletHandlerTest {

    private static final BigDecimal SOME_FUND_AMOUNT = BigDecimal.valueOf(500);
    private static final BigDecimal SOME_TREASURY_BALANCE = BigDecimal.valueOf(1_000_000);

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TreasuryService treasuryService;

    @InjectMocks
    private FundWalletHandler fundWalletHandler;

    @Test
    void shouldFundWallet() {
        // given
        var existingWallet = WalletFixtures.walletBuilder().build();

        given(walletRepository.findById(WalletFixtures.SOME_WALLET_ID))
                .willReturn(Optional.of(existingWallet));
        given(treasuryService.getBalance()).willReturn(SOME_TREASURY_BALANCE);

        var fundedWallet = existingWallet.toBuilder()
                .availableBalance(BigDecimal.valueOf(600))
                .totalBalance(BigDecimal.valueOf(600))
                .build();

        given(walletRepository.save(fundedWallet)).willReturn(fundedWallet);

        // when
        var result = fundWalletHandler.handle(WalletFixtures.SOME_WALLET_ID, SOME_FUND_AMOUNT);

        // then
        var expected = existingWallet.toBuilder()
                .availableBalance(BigDecimal.valueOf(600))
                .totalBalance(BigDecimal.valueOf(600))
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(treasuryService).should()
                .transferFromTreasury(WalletFixtures.SOME_SOLANA_ADDRESS, SOME_FUND_AMOUNT);
        then(walletRepository).should().save(fundedWallet);
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        // given
        given(walletRepository.findById(WalletFixtures.SOME_WALLET_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> fundWalletHandler.handle(WalletFixtures.SOME_WALLET_ID, SOME_FUND_AMOUNT))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006")
                .hasMessageContaining(WalletFixtures.SOME_WALLET_ID.toString());
    }

    @Test
    void shouldThrowWhenTreasuryDepleted() {
        // given
        var existingWallet = WalletFixtures.walletBuilder()
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        var lowTreasuryBalance = BigDecimal.valueOf(100);
        var requestedAmount = BigDecimal.valueOf(500);

        given(walletRepository.findById(WalletFixtures.SOME_WALLET_ID))
                .willReturn(Optional.of(existingWallet));
        given(treasuryService.getBalance()).willReturn(lowTreasuryBalance);

        // when / then
        assertThatThrownBy(() -> fundWalletHandler.handle(WalletFixtures.SOME_WALLET_ID, requestedAmount))
                .isInstanceOf(TreasuryDepletedException.class)
                .hasMessageContaining("SP-0007");
    }
}
