package com.stablepay.domain.wallet.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.stablepay.domain.wallet.exception.InsufficientBalanceException;

class WalletTest {

    @Nested
    class ReserveBalance {

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
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("SP-0002");
        }

        @Test
        void shouldThrowWhenReservingNegativeAmount() {
            // given
            var wallet = Wallet.builder()
                    .userId("user-1")
                    .solanaAddress("SomeAddress123")
                    .totalBalance(BigDecimal.valueOf(100))
                    .availableBalance(BigDecimal.valueOf(100))
                    .build();

            // when / then
            assertThatThrownBy(() -> wallet.reserveBalance(BigDecimal.valueOf(-10)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SP-0004");
        }

        @Test
        void shouldThrowWhenReservingZeroAmount() {
            // given
            var wallet = Wallet.builder()
                    .userId("user-1")
                    .solanaAddress("SomeAddress123")
                    .totalBalance(BigDecimal.valueOf(100))
                    .availableBalance(BigDecimal.valueOf(100))
                    .build();

            // when / then
            assertThatThrownBy(() -> wallet.reserveBalance(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SP-0004");
        }
    }

    @Nested
    class ReleaseBalance {

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

        @Test
        void shouldThrowWhenReleasingExceedsTotalBalance() {
            // given
            var wallet = Wallet.builder()
                    .userId("user-1")
                    .solanaAddress("SomeAddress123")
                    .totalBalance(BigDecimal.valueOf(100))
                    .availableBalance(BigDecimal.valueOf(40))
                    .build();

            // when / then
            assertThatThrownBy(() -> wallet.releaseBalance(BigDecimal.valueOf(100)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SP-0005");
        }

        @Test
        void shouldThrowWhenReleasingNegativeAmount() {
            // given
            var wallet = Wallet.builder()
                    .userId("user-1")
                    .solanaAddress("SomeAddress123")
                    .totalBalance(BigDecimal.valueOf(100))
                    .availableBalance(BigDecimal.valueOf(40))
                    .build();

            // when / then
            assertThatThrownBy(() -> wallet.releaseBalance(BigDecimal.valueOf(-10)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SP-0004");
        }
    }

    @Nested
    class CompactConstructorValidation {

        @Test
        void shouldThrowWhenUserIdIsNull() {
            // when / then
            assertThatThrownBy(() -> Wallet.builder()
                    .solanaAddress("SomeAddress123")
                    .availableBalance(BigDecimal.ZERO)
                    .totalBalance(BigDecimal.ZERO)
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        void shouldThrowWhenAvailableBalanceIsNull() {
            // when / then
            assertThatThrownBy(() -> Wallet.builder()
                    .userId("user-1")
                    .solanaAddress("SomeAddress123")
                    .totalBalance(BigDecimal.ZERO)
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("availableBalance");
        }
    }
}
