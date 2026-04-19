package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_INR;
import static com.stablepay.testutil.RemittanceFixtures.SOME_AMOUNT_USDC;
import static com.stablepay.testutil.RemittanceFixtures.SOME_FX_RATE;
import static com.stablepay.testutil.RemittanceFixtures.SOME_RECIPIENT_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.model.DisbursementResult;
import com.stablepay.domain.remittance.model.Remittance;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.port.RemittanceRepository;
import com.stablepay.test.PgTest;

@PgTest
class RemittancePayoutWriterIntegrationTest {

    @Autowired
    private RemittancePayoutWriter remittancePayoutWriter;

    @Autowired
    private RemittanceRepository remittanceRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID remittanceId;

    @BeforeEach
    void setUp() {
        remittanceId = UUID.randomUUID();
        transactionTemplate.execute(status -> remittanceRepository.save(Remittance.builder()
                .remittanceId(remittanceId)
                .senderId("writer-sender-" + System.nanoTime())
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.CLAIMED)
                .smsNotificationFailed(false)
                .build()));
    }

    @Test
    void shouldReturnEmptyWhenNoPayoutYetPersisted() {
        // when
        var result = remittancePayoutWriter.findExistingPayout(remittanceId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldCommitPayoutIdImmediatelySoOtherTransactionsCanReadIt() {
        // given — writePayoutId runs in REQUIRES_NEW; the row must be visible to a
        // brand-new read transaction started AFTER writePayoutId returns.
        var existing = transactionTemplate.execute(status ->
                remittanceRepository.findByRemittanceId(remittanceId).orElseThrow());

        // when
        remittancePayoutWriter.writePayoutId(remittanceId, "pout_ABC123", "processing");

        // then
        var reloaded = transactionTemplate.execute(status ->
                remittanceRepository.findByRemittanceId(remittanceId).orElseThrow());
        var expected = existing.toBuilder()
                .payoutId("pout_ABC123")
                .payoutProviderStatus("processing")
                .build();
        assertThat(reloaded).usingRecursiveComparison()
                .ignoringFields("updatedAt")
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnPersistedPayoutAfterWrite() {
        // given
        remittancePayoutWriter.writePayoutId(remittanceId, "pout_XYZ789", "processed");

        // when
        var result = remittancePayoutWriter.findExistingPayout(remittanceId);

        // then
        var expected = DisbursementResult.builder()
                .providerId("pout_XYZ789")
                .providerStatus("processed")
                .build();
        assertThat(result).isPresent();
        assertThat(result.get()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectDuplicatePayoutIdAcrossRemittancesViaPartialUniqueIndex() {
        // given — first remittance claims the payout_id
        remittancePayoutWriter.writePayoutId(remittanceId, "pout_DUP001", "processing");

        var secondRemittanceId = UUID.randomUUID();
        transactionTemplate.execute(status -> remittanceRepository.save(Remittance.builder()
                .remittanceId(secondRemittanceId)
                .senderId("writer-sender-second-" + System.nanoTime())
                .recipientPhone(SOME_RECIPIENT_PHONE)
                .amountUsdc(SOME_AMOUNT_USDC)
                .amountInr(SOME_AMOUNT_INR)
                .fxRate(SOME_FX_RATE)
                .status(RemittanceStatus.CLAIMED)
                .smsNotificationFailed(false)
                .build()));

        // when / then — partial unique index idx_remittances_payout_id rejects reuse.
        assertThatThrownBy(() -> remittancePayoutWriter.writePayoutId(
                secondRemittanceId, "pout_DUP001", "processing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SP-0027")
                .hasMessageContaining(secondRemittanceId.toString());

        var secondAfterRollback = transactionTemplate.execute(status ->
                remittanceRepository.findByRemittanceId(secondRemittanceId).orElseThrow());
        assertThat(secondAfterRollback.payoutId()).isNull();
        assertThat(secondAfterRollback.payoutProviderStatus()).isNull();
    }

    @Test
    void shouldPersistSanitizedFailureReasonRoundTrippedThroughDb() {
        // when
        remittancePayoutWriter.writeFailureReason(
                remittanceId, "VPA_INVALID: vpa alice@upi rejected by bank");

        // then
        var reloaded = transactionTemplate.execute(status ->
                remittanceRepository.findByRemittanceId(remittanceId).orElseThrow());
        assertThat(reloaded.payoutFailureReason())
                .isEqualTo("VPA_INVALID: vpa ali**** rejected by bank");
    }

    @Test
    void shouldTruncateFailureReasonToColumnLimit() {
        // when
        remittancePayoutWriter.writeFailureReason(remittanceId, "x".repeat(750));

        // then
        var reloaded = transactionTemplate.execute(status ->
                remittanceRepository.findByRemittanceId(remittanceId).orElseThrow());
        assertThat(reloaded.payoutFailureReason()).hasSize(500);
    }

    @Test
    void shouldThrowWhenWritingPayoutIdForUnknownRemittance() {
        // given
        var unknown = UUID.randomUUID();

        // when / then
        assertThatThrownBy(() -> remittancePayoutWriter.writePayoutId(
                unknown, "pout_NOPE", "processing"))
                .isInstanceOf(RemittanceNotFoundException.class)
                .hasMessageContaining("SP-0010");
    }
}
