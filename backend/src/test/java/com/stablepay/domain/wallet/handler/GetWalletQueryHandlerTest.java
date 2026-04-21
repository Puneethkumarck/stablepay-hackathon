package com.stablepay.domain.wallet.handler;

import static com.stablepay.testutil.AuthFixtures.SOME_OTHER_USER_ID;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

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
        given(walletRepository.findByUserId(SOME_OTHER_USER_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> getWalletQueryHandler.handle(SOME_OTHER_USER_ID))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("SP-0006")
                .hasMessageContaining(SOME_OTHER_USER_ID.toString());
    }
}
