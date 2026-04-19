package com.stablepay.infrastructure.disbursement;

import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.WorkflowFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.WorkflowFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.WorkflowFixtures.SOME_UPI_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.stablepay.domain.remittance.model.DisbursementResult;

class LoggingDisbursementAdapterTest {

    private final LoggingDisbursementAdapter adapter = new LoggingDisbursementAdapter();

    @Test
    void shouldReturnSyntheticDisbursementResultWithSimulatedStatus() {
        // given — logging adapter is a no-op simulator

        // when
        var result = adapter.disburse(
                SOME_UPI_ID, SOME_AMOUNT_USDC, SOME_AMOUNT_INR, SOME_REMITTANCE_ID.toString());

        // then
        var expected = DisbursementResult.builder()
                .providerId("ignored")
                .providerStatus("SIMULATED")
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("providerId")
                .isEqualTo(expected);
        assertThat(result.providerId()).matches("^log_[0-9a-f-]{36}$");
    }
}
