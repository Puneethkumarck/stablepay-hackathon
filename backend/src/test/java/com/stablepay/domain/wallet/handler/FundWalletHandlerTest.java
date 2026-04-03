package com.stablepay.domain.wallet.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.wallet.exception.TreasuryDepletedException;
import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.TreasuryService;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class FundWalletHandlerTest {

    private static final Long SOME_WALLET_ID = 1L;
    private static final String SOME_USER_ID = "user-123";
    private static final String SOME_SOLANA_ADDRESS = "SoLaNaAdDrEsS123456789";
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
        var existingWallet = Wallet.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(BigDecimal.valueOf(100))
                .totalBalance(BigDecimal.valueOf(100))
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T10:00:00Z"))
                .build();

        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(existingWallet));
        given(treasuryService.getBalance()).willReturn(SOME_TREASURY_BALANCE);

        var fundedWallet = existingWallet.toBuilder()
                .availableBalance(BigDecimal.valueOf(600))
                .totalBalance(BigDecimal.valueOf(600))
                .build();

        given(walletRepository.save(fundedWallet)).willReturn(fundedWallet);

        // when
        var result = fundWalletHandler.handle(SOME_WALLET_ID, SOME_FUND_AMOUNT);

        // then
        var expected = existingWallet.toBuilder()
                .availableBalance(BigDecimal.valueOf(600))
                .totalBalance(BigDecimal.valueOf(600))
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(treasuryService).should().transferFromTreasury(SOME_SOLANA_ADDRESS, SOME_FUND_AMOUNT);
        then(walletRepository).should().save(fundedWallet);
    }

    @Test
    void shouldThrowWhenWalletNotFound() {
        // given
        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> fundWalletHandler.handle(SOME_WALLET_ID, SOME_FUND_AMOUNT))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006")
                .hasMessageContaining(SOME_WALLET_ID.toString());
    }

    @Test
    void shouldThrowWhenTreasuryDepleted() {
        // given
        var existingWallet = Wallet.builder()
                .id(SOME_WALLET_ID)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T10:00:00Z"))
                .build();

        var lowTreasuryBalance = BigDecimal.valueOf(100);
        var requestedAmount = BigDecimal.valueOf(500);

        given(walletRepository.findById(SOME_WALLET_ID)).willReturn(Optional.of(existingWallet));
        given(treasuryService.getBalance()).willReturn(lowTreasuryBalance);

        // when / then
        assertThatThrownBy(() -> fundWalletHandler.handle(SOME_WALLET_ID, requestedAmount))
                .isInstanceOf(TreasuryDepletedException.class)
                .hasMessageContaining("SP-0007");
    }
}
