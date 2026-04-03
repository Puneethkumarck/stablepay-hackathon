package com.stablepay.domain.service;

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

import com.stablepay.domain.exception.TreasuryDepletedException;
import com.stablepay.domain.exception.WalletAlreadyExistsException;
import com.stablepay.domain.exception.WalletNotFoundException;
import com.stablepay.domain.model.Wallet;
import com.stablepay.domain.port.outbound.MpcWalletClient;
import com.stablepay.domain.port.outbound.TreasuryService;
import com.stablepay.domain.port.outbound.WalletRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    private static final String USER_ID = "user-42";
    private static final String SOLANA_ADDRESS = "SoLaNa1234567890AbCdEfGhIjKlMnOpQrStUvWx";
    private static final Long WALLET_ID = 1L;
    private static final BigDecimal FUND_AMOUNT = BigDecimal.valueOf(50);
    private static final BigDecimal TREASURY_BALANCE = BigDecimal.valueOf(1_000_000);

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MpcWalletClient mpcWalletClient;

    @Mock
    private TreasuryService treasuryService;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void shouldCreateWalletWithMpcGeneratedAddress() {
        // given
        var newWallet = Wallet.builder()
                .userId(USER_ID)
                .solanaAddress(SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();
        var savedWallet = newWallet.toBuilder()
                .id(WALLET_ID)
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T10:00:00Z"))
                .build();

        given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
        given(mpcWalletClient.generateKey()).willReturn(SOLANA_ADDRESS);
        given(walletRepository.save(newWallet)).willReturn(savedWallet);

        // when
        var result = walletService.create(USER_ID);

        // then
        var expected = Wallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .solanaAddress(SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T10:00:00Z"))
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(mpcWalletClient).should().generateKey();
        then(walletRepository).should().save(newWallet);
    }

    @Test
    void shouldFundWalletSuccessfully() {
        // given
        var existingWallet = Wallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .solanaAddress(SOLANA_ADDRESS)
                .availableBalance(BigDecimal.valueOf(100))
                .totalBalance(BigDecimal.valueOf(100))
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T10:00:00Z"))
                .build();
        var fundedWallet = existingWallet.toBuilder()
                .availableBalance(BigDecimal.valueOf(150))
                .totalBalance(BigDecimal.valueOf(150))
                .build();
        var savedWallet = fundedWallet.toBuilder()
                .updatedAt(Instant.parse("2026-04-03T11:00:00Z"))
                .build();

        given(walletRepository.findById(WALLET_ID)).willReturn(Optional.of(existingWallet));
        given(treasuryService.getBalance()).willReturn(TREASURY_BALANCE);
        given(walletRepository.save(fundedWallet)).willReturn(savedWallet);

        // when
        var result = walletService.fund(WALLET_ID, FUND_AMOUNT);

        // then
        var expected = Wallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .solanaAddress(SOLANA_ADDRESS)
                .availableBalance(BigDecimal.valueOf(150))
                .totalBalance(BigDecimal.valueOf(150))
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T11:00:00Z"))
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(treasuryService).should().transferFromTreasury(SOLANA_ADDRESS, FUND_AMOUNT);
        then(walletRepository).should().save(fundedWallet);
    }

    @Test
    void shouldThrowWhenWalletNotFoundOnFund() {
        // given
        given(walletRepository.findById(WALLET_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> walletService.fund(WALLET_ID, FUND_AMOUNT))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006");
    }

    @Test
    void shouldThrowTreasuryDepletedWhenTreasuryBalanceLow() {
        // given
        var lowTreasuryBalance = BigDecimal.valueOf(10);
        var requestedAmount = BigDecimal.valueOf(100);
        var existingWallet = Wallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .solanaAddress(SOLANA_ADDRESS)
                .availableBalance(BigDecimal.valueOf(200))
                .totalBalance(BigDecimal.valueOf(200))
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T10:00:00Z"))
                .build();

        given(walletRepository.findById(WALLET_ID)).willReturn(Optional.of(existingWallet));
        given(treasuryService.getBalance()).willReturn(lowTreasuryBalance);

        // when / then
        assertThatThrownBy(() -> walletService.fund(WALLET_ID, requestedAmount))
                .isInstanceOf(TreasuryDepletedException.class)
                .hasMessageContaining("SP-0007");
    }

    @Test
    void shouldThrowWhenWalletAlreadyExistsForUserId() {
        // given
        var existingWallet = Wallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .solanaAddress(SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T10:00:00Z"))
                .build();

        given(walletRepository.findByUserId(USER_ID)).willReturn(Optional.of(existingWallet));

        // when / then
        assertThatThrownBy(() -> walletService.create(USER_ID))
                .isInstanceOf(WalletAlreadyExistsException.class)
                .hasMessageContaining("SP-0008");
    }
}
