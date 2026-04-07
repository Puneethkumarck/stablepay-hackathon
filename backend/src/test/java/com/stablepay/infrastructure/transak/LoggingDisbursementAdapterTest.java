package com.stablepay.infrastructure.transak;

import static com.stablepay.testutil.WorkflowFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class LoggingDisbursementAdapterTest {

    private static final String SOME_AMOUNT_INR = "8450.00";

    private final LoggingDisbursementAdapter adapter = new LoggingDisbursementAdapter();

    @Test
    void shouldDisburseWithoutError() {
        // given — logging adapter is a no-op

        // when / then
        assertThatCode(() -> adapter.disburse(SOME_UPI_ID, SOME_AMOUNT_INR, SOME_REMITTANCE_ID.toString()))
                .doesNotThrowAnyException();
    }
}
