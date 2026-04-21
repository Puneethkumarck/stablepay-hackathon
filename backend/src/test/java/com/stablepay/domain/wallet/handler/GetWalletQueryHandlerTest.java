package com.stablepay.domain.wallet.handler;

import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.wallet.exception.WalletNotFoundException;
import com.stablepay.domain.wallet.port.WalletRepository;

@ExtendWith(MockitoExtension.class)
class GetWalletQueryHandlerTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private GetWalletQueryHandler getWalletQueryHandler;

    @Test
    void shouldReturnWalletForUser() {
        // given
        var wallet = walletBuilder().build();
        given(walletRepository.findByUserId(SOME_USER_ID)).willReturn(Optional.of(wallet));

        // when
        var result = getWalletQueryHandler.handle(SOME_USER_ID);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(wallet);
    }

    @Test
    void shouldThrowWalletNotFoundWhenNoWallet() {
        // given
        var unknownUserId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        given(walletRepository.findByUserId(unknownUserId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> getWalletQueryHandler.handle(unknownUserId))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006")
                .hasMessageContaining(unknownUserId.toString());
    }
}
