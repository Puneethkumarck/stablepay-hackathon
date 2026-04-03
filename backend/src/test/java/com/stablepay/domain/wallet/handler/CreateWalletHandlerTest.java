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

import com.stablepay.domain.wallet.exception.WalletAlreadyExistsException;
import com.stablepay.domain.wallet.model.Wallet;
import com.stablepay.domain.wallet.port.MpcWalletClient;
import com.stablepay.domain.wallet.port.WalletRepository;
import com.stablepay.testutil.WalletFixtures;

@ExtendWith(MockitoExtension.class)
class CreateWalletHandlerTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private MpcWalletClient mpcWalletClient;

    @InjectMocks
    private CreateWalletHandler createWalletHandler;

    @Test
    void shouldCreateWallet() {
        // given
        given(walletRepository.findByUserId(WalletFixtures.SOME_USER_ID)).willReturn(Optional.empty());
        given(mpcWalletClient.generateKey()).willReturn(WalletFixtures.SOME_SOLANA_ADDRESS);

        var walletToSave = Wallet.builder()
                .userId(WalletFixtures.SOME_USER_ID)
                .solanaAddress(WalletFixtures.SOME_SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        var savedWallet = walletToSave.toBuilder()
                .id(WalletFixtures.SOME_WALLET_ID)
                .createdAt(WalletFixtures.SOME_CREATED_AT)
                .updatedAt(WalletFixtures.SOME_UPDATED_AT)
                .build();

        given(walletRepository.save(walletToSave)).willReturn(savedWallet);

        // when
        var result = createWalletHandler.handle(WalletFixtures.SOME_USER_ID);

        // then
        var expected = Wallet.builder()
                .id(WalletFixtures.SOME_WALLET_ID)
                .userId(WalletFixtures.SOME_USER_ID)
                .solanaAddress(WalletFixtures.SOME_SOLANA_ADDRESS)
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .createdAt(WalletFixtures.SOME_CREATED_AT)
                .updatedAt(WalletFixtures.SOME_UPDATED_AT)
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
        var existingWallet = WalletFixtures.walletBuilder()
                .availableBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build();

        given(walletRepository.findByUserId(WalletFixtures.SOME_USER_ID))
                .willReturn(Optional.of(existingWallet));

        // when / then
        assertThatThrownBy(() -> createWalletHandler.handle(WalletFixtures.SOME_USER_ID))
                .isInstanceOf(WalletAlreadyExistsException.class)
                .hasMessageContaining("SP-0008")
                .hasMessageContaining(WalletFixtures.SOME_USER_ID);
    }
}
