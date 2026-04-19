package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.model.DisbursementResult;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.port.RemittanceRepository;

@ExtendWith(MockitoExtension.class)
class RemittancePayoutWriterTest {

    private static final String SOME_PAYOUT_ID = "pout_ABC123";
    private static final String SOME_PROVIDER_STATUS = "processing";

    @Mock
    private RemittanceRepository remittanceRepository;

    @InjectMocks
    private RemittancePayoutWriter remittancePayoutWriter;

    @Captor
    private ArgumentCaptor<Remittance> remittanceCaptor;

    @Test
    void shouldReturnEmptyWhenPayoutIdNotYetPersisted() {
        // given
        var remittance = remittanceBuilder().build();
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));

        // when
        var result = remittancePayoutWriter.findExistingPayout(SOME_REMITTANCE_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenRemittanceNotFound() {
        // given
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.empty());

        // when
        var result = remittancePayoutWriter.findExistingPayout(SOME_REMITTANCE_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnPersistedPayoutWhenPayoutIdAlreadySet() {
        // given
        var remittance = remittanceBuilder()
                .payoutId(SOME_PAYOUT_ID)
                .payoutProviderStatus(SOME_PROVIDER_STATUS)
                .build();
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));

        // when
        var result = remittancePayoutWriter.findExistingPayout(SOME_REMITTANCE_ID);

        // then
        var expected = DisbursementResult.builder()
                .providerId(SOME_PAYOUT_ID)
                .providerStatus(SOME_PROVIDER_STATUS)
                .build();
        assertThat(result).isPresent();
        assertThat(result.get()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldPersistPayoutIdAndProviderStatus() {
        // given
        var remittance = remittanceBuilder().build();
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));
        given(remittanceRepository.save(argThat(r -> r != null && SOME_PAYOUT_ID.equals(r.payoutId()))))
                .willAnswer(invocation -> invocation.<Remittance>getArgument(0));

        // when
        remittancePayoutWriter.writePayoutId(SOME_REMITTANCE_ID, SOME_PAYOUT_ID, SOME_PROVIDER_STATUS);

        // then
        then(remittanceRepository).should().save(remittanceCaptor.capture());
        var saved = remittanceCaptor.getValue();

        var expected = remittance.toBuilder()
                .payoutId(SOME_PAYOUT_ID)
                .payoutProviderStatus(SOME_PROVIDER_STATUS)
                .build();
        assertThat(saved).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenRemittanceMissingOnWritePayoutId() {
        // given
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> remittancePayoutWriter.writePayoutId(
                SOME_REMITTANCE_ID, SOME_PAYOUT_ID, SOME_PROVIDER_STATUS))
                .isInstanceOf(RemittanceNotFoundException.class)
                .hasMessageContaining("SP-0010")
                .hasMessageContaining(SOME_REMITTANCE_ID.toString());
    }

    @Test
    void shouldTranslateDuplicatePayoutIdToIllegalStateException() {
        // given
        var remittance = remittanceBuilder().build();
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));
        willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(remittanceRepository).save(argThat(r -> r != null
                        && SOME_PAYOUT_ID.equals(r.payoutId())));

        // when / then
        assertThatThrownBy(() -> remittancePayoutWriter.writePayoutId(
                SOME_REMITTANCE_ID, SOME_PAYOUT_ID, SOME_PROVIDER_STATUS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SP-0027")
                .hasMessageContaining(SOME_REMITTANCE_ID.toString());
    }

    @Test
    void shouldReturnEmptyWhenPayoutIdSetButProviderStatusMissing() {
        // given — defensive guard: a partial write (or out-of-band SQL) could leave
        // payout_id set while payout_provider_status is null. findExistingPayout
        // must not NPE on DisbursementResult construction.
        var remittance = remittanceBuilder()
                .payoutId(SOME_PAYOUT_ID)
                .payoutProviderStatus(null)
                .build();
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));

        // when
        var result = remittancePayoutWriter.findExistingPayout(SOME_REMITTANCE_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldPersistSanitizedFailureReasonMaskingEmbeddedUpiHandles() {
        // given
        var remittance = remittanceBuilder().build();
        var rawReason = "BAD_REQUEST: vpa alice@upi rejected by bank";
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));
        given(remittanceRepository.save(argThat(r -> r != null && r.payoutFailureReason() != null)))
                .willAnswer(invocation -> invocation.<Remittance>getArgument(0));

        // when
        remittancePayoutWriter.writeFailureReason(SOME_REMITTANCE_ID, rawReason);

        // then
        then(remittanceRepository).should().save(remittanceCaptor.capture());
        var saved = remittanceCaptor.getValue();

        var expected = remittance.toBuilder()
                .payoutFailureReason("BAD_REQUEST: vpa ali**** rejected by bank")
                .build();
        assertThat(saved).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldTruncateFailureReasonTo500Chars() {
        // given
        var remittance = remittanceBuilder().build();
        var rawReason = "x".repeat(600);
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));
        given(remittanceRepository.save(argThat(r -> r != null && r.payoutFailureReason() != null)))
                .willAnswer(invocation -> invocation.<Remittance>getArgument(0));

        // when
        remittancePayoutWriter.writeFailureReason(SOME_REMITTANCE_ID, rawReason);

        // then
        then(remittanceRepository).should().save(remittanceCaptor.capture());
        var saved = remittanceCaptor.getValue();
        assertThat(saved.payoutFailureReason()).hasSize(500);
        assertThat(saved.payoutFailureReason()).isEqualTo("x".repeat(500));
    }

    @Test
    void shouldPersistNullFailureReasonWhenRawReasonIsNull() {
        // given
        var remittance = remittanceBuilder().build();
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.of(remittance));
        given(remittanceRepository.save(argThat(r -> r != null && r.payoutFailureReason() == null)))
                .willAnswer(invocation -> invocation.<Remittance>getArgument(0));

        // when
        remittancePayoutWriter.writeFailureReason(SOME_REMITTANCE_ID, null);

        // then
        then(remittanceRepository).should().save(remittanceCaptor.capture());
        var saved = remittanceCaptor.getValue();
        assertThat(saved.payoutFailureReason()).isNull();
    }

    @Test
    void shouldThrowWhenRemittanceMissingOnWriteFailureReason() {
        // given
        given(remittanceRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> remittancePayoutWriter.writeFailureReason(
                SOME_REMITTANCE_ID, "some error"))
                .isInstanceOf(RemittanceNotFoundException.class)
                .hasMessageContaining("SP-0010")
                .hasMessageContaining(SOME_REMITTANCE_ID.toString());
    }
}
