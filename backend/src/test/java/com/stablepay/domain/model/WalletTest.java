package com.stablepay.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class WalletTest {

    @Test
    void shouldReserveBalanceSuccessfully() {
        // given
        var wallet = Wallet.builder()
                .userId("user-1")
                .solanaAddress("SomeAddress123")
                .totalBalance(BigDecimal.valueOf(100))
                .availableBalance(BigDecimal.valueOf(100))
                .build();

        // when
        var reserved = wallet.reserveBalance(BigDecimal.valueOf(60));

        // then
        var expected = wallet.toBuilder()
                .availableBalance(BigDecimal.valueOf(40))
                .build();

        assertThat(reserved)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenReservingMoreThanAvailable() {
        // given
        var wallet = Wallet.builder()
                .userId("user-1")
                .solanaAddress("SomeAddress123")
                .totalBalance(BigDecimal.valueOf(100))
                .availableBalance(BigDecimal.valueOf(40))
                .build();

        // when / then
        assertThatThrownBy(() -> wallet.reserveBalance(BigDecimal.valueOf(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SP-0002");
    }

    @Test
    void shouldReleaseBalanceSuccessfully() {
        // given
        var wallet = Wallet.builder()
                .userId("user-1")
                .solanaAddress("SomeAddress123")
                .totalBalance(BigDecimal.valueOf(100))
                .availableBalance(BigDecimal.valueOf(40))
                .build();

        // when
        var released = wallet.releaseBalance(BigDecimal.valueOf(60));

        // then
        var expected = wallet.toBuilder()
                .availableBalance(BigDecimal.valueOf(100))
                .build();

        assertThat(released)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }
}
