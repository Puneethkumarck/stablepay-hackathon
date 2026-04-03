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

import com.stablepay.domain.wallet.exception.WalletAlreadyExistsException;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class CreateWalletHandlerTest {

    private static final String SOME_USER_ID = "user-123";
    private static final String SOME_SOLANA_ADDRESS = "SoLaNaAdDrEsS123456789";

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MpcWalletClient mpcWalletClient;

    @InjectMocks
    private CreateWalletHandler createWalletHandler;

    @Test
    void shouldCreateWallet() {
        // given
        given(walletRepository.findByUserId(SOME_USER_ID)).willReturn(Optional.empty());
        given(mpcWalletClient.generateKey()).willReturn(SOME_SOLANA_ADDRESS);

        var walletToSave = Wallet.builder()
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        var savedWallet = walletToSave.toBuilder()
                .id(1L)
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T10:00:00Z"))
                .build();

        given(walletRepository.save(walletToSave)).willReturn(savedWallet);

        // when
        var result = createWalletHandler.handle(SOME_USER_ID);

        // then
        var expected = Wallet.builder()
                .id(1L)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .createdAt(Instant.parse("2026-04-03T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-03T10:00:00Z"))
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        then(mpcWalletClient).should().generateKey();
        then(walletRepository).should().save(walletToSave);
    }

    @Test
    void shouldThrowWhenWalletAlreadyExists() {
        // given
        var existingWallet = Wallet.builder()
                .id(1L)
                .userId(SOME_USER_ID)
                .solanaAddress(SOME_SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        given(walletRepository.findByUserId(SOME_USER_ID)).willReturn(Optional.of(existingWallet));

        // when / then
        assertThatThrownBy(() -> createWalletHandler.handle(SOME_USER_ID))
                .isInstanceOf(WalletAlreadyExistsException.class)
                .hasMessageContaining("SP-0008")
                .hasMessageContaining(SOME_USER_ID);
    }
}
