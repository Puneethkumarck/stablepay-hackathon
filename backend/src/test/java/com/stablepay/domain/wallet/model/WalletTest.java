package com.stablepay.domain.wallet.model;

import static com.stablepay.testutil.WalletFixtures.SOME_SOLANA_ADDRESS;
import static com.stablepay.testutil.WalletFixtures.SOME_USER_ID;
import static com.stablepay.testutil.WalletFixtures.walletBuilder;
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
            var wallet = walletBuilder().build();

            // when
            var reserved = wallet.reserveBalance(BigDecimal.valueOf(60));

            // then
            var expected = wallet.toBuilder()
                    .availableBalance(new BigDecimal("40.50"))
                    .build();

            assertThat(reserved)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldThrowWhenReservingMoreThanAvailable() {
            // given
            var wallet = walletBuilder()
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
            var wallet = walletBuilder().build();

            // when / then
            assertThatThrownBy(() -> wallet.reserveBalance(BigDecimal.valueOf(-10)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SP-0004");
        }

        @Test
        void shouldThrowWhenReservingZeroAmount() {
            // given
            var wallet = walletBuilder().build();

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
            var wallet = walletBuilder()
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
            var wallet = walletBuilder()
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
            var wallet = walletBuilder()
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
                    .solanaAddress(SOME_SOLANA_ADDRESS)
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
                    .userId(SOME_USER_ID)
                    .solanaAddress(SOME_SOLANA_ADDRESS)
                    .totalBalance(BigDecimal.ZERO)
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("availableBalance");
        }
    }
}
