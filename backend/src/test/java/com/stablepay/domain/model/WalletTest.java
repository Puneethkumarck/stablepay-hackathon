package com.stablepay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class WalletTest {

    @Test
    void shouldCalculateAvailableBalanceAfterReservation() {
        // given
        var wallet = Wallet.builder()
                .userId("user-1")
                .solanaAddress("SomeAddress123")
                .totalBalance(BigDecimal.valueOf(100))
                .availableBalance(BigDecimal.valueOf(100))
                .build();

        // when — simulate reserving 60 USDC for a remittance
        var reserved = wallet.toBuilder()
                .availableBalance(wallet.availableBalance().subtract(BigDecimal.valueOf(60)))
                .build();

        // then
        var expected = wallet.toBuilder()
                .availableBalance(BigDecimal.valueOf(40))
                .build();

        assertThat(reserved)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
