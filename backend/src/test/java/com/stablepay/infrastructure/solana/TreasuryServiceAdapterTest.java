package com.stablepay.infrastructure.solana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class TreasuryServiceAdapterTest {

    private final TreasuryServiceAdapter adapter = new TreasuryServiceAdapter();

    @Test
    void shouldReturnStubBalanceOfOneMillion() {
        // when
        var balance = adapter.getBalance();

        // then
        assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
    }

    @Test
    void shouldTransferFromTreasuryWithoutError() {
        // given — stub implementation is a no-op

        // when / then
        assertThatCode(() -> adapter.transferFromTreasury("SoLaNa1234567890AbCdEf", BigDecimal.valueOf(500)))
                .doesNotThrowAnyException();
    }
}
