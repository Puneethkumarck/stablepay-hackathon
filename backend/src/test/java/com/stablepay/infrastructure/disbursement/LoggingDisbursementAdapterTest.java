package com.stablepay.infrastructure.disbursement;

import static com.stablepay.testutil.WorkflowFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class LoggingDisbursementAdapterTest {

    private static final BigDecimal SOME_AMOUNT_USDC = new BigDecimal("100.00");

    private final LoggingDisbursementAdapter adapter = new LoggingDisbursementAdapter();

    @Test
    void shouldDisburseWithoutError() {
        // given — logging adapter is a no-op

        // when / then
        assertThatCode(() -> adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_REMITTANCE_ID.toString()))
                .doesNotThrowAnyException();
    }
}
