package com.stablepay.application.controller.wallet.mapper;

import static com.stablepay.testutil.WalletFixtures.walletBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.application.dto.WalletResponse;

class WalletApiMapperTest {

    private final WalletApiMapper mapper = new WalletApiMapperImpl();

    @Test
    void shouldMapWalletToResponse() {
        // given
        var wallet = walletBuilder().build();

        // when
        var response = mapper.toResponse(wallet);

        // then
        var expected = WalletResponse.builder()
                .id(wallet.id())
                .solanaAddress(wallet.solanaAddress())
                .availableBalance(wallet.availableBalance())
                .totalBalance(wallet.totalBalance())
                .createdAt(wallet.createdAt())
                .updatedAt(wallet.updatedAt())
                .build();

        assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
