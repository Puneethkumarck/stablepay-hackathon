package com.stablepay.domain.remittance.handler;

import static com.stablepay.testutil.RemittanceFixtures.SOME_REMITTANCE_ID;
import static com.stablepay.testutil.RemittanceFixtures.SOME_SENDER_ID;
import static com.stablepay.testutil.RemittanceFixtures.remittanceBuilder;
import static com.stablepay.testutil.RemittanceStatusEventFixtures.eventBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stablepay.domain.remittance.exception.RemittanceNotFoundException;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.RemittanceTimeline;
import com.stablepay.domain.remittance.model.RemittanceTimelineStep;
import com.stablepay.domain.remittance.model.TimelineStepStatus;
import com.stablepay.domain.remittance.port.RemittanceStatusEventRepository;

@ExtendWith(MockitoExtension.class)
class GetRemittanceTimelineHandlerTest {

    private static final Instant T1 = Instant.parse("2026-04-03T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-04-03T10:01:00Z");
    private static final Instant T3 = Instant.parse("2026-04-03T10:02:00Z");
    private static final Instant T4 = Instant.parse("2026-04-03T10:03:00Z");

    @Mock
    private GetRemittanceQueryHandler getRemittanceQueryHandler;

    @Mock
    private RemittanceStatusEventRepository remittanceStatusEventRepository;

    @InjectMocks
    private GetRemittanceTimelineHandler getRemittanceTimelineHandler;

    @Test
    void shouldBuildTimelineForInitiatedStatus() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.INITIATED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.CURRENT)
                                .message("Securing funds on-chain...")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.PENDING)
                                .message("Recipient claimed")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.PENDING)
                                .message("INR deposited to recipient's bank")
                                .build()
                ))
                .failed(false)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildTimelineForEscrowedStatus() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.ESCROWED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build(),
                eventBuilder().status(RemittanceStatus.ESCROWED).createdAt(T2).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Funds secured on-chain")
                                .completedAt(T2)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.CURRENT)
                                .message("SMS sent, waiting for recipient")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.PENDING)
                                .message("INR deposited to recipient's bank")
                                .build()
                ))
                .failed(false)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildTimelineForClaimedStatus() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.CLAIMED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build(),
                eventBuilder().status(RemittanceStatus.ESCROWED).createdAt(T2).build(),
                eventBuilder().status(RemittanceStatus.CLAIMED).createdAt(T3).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Funds secured on-chain")
                                .completedAt(T2)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Recipient claimed")
                                .completedAt(T3)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.CURRENT)
                                .message("Depositing INR to recipient's bank...")
                                .build()
                ))
                .failed(false)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildTimelineForDeliveredStatus() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.DELIVERED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build(),
                eventBuilder().status(RemittanceStatus.ESCROWED).createdAt(T2).build(),
                eventBuilder().status(RemittanceStatus.CLAIMED).createdAt(T3).build(),
                eventBuilder().status(RemittanceStatus.DELIVERED).createdAt(T4).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Funds secured on-chain")
                                .completedAt(T2)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Recipient claimed")
                                .completedAt(T3)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("INR deposited to recipient's bank")
                                .completedAt(T4)
                                .build()
                ))
                .failed(false)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildTimelineForDepositFailed() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.DEPOSIT_FAILED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.FAILED)
                                .message("Failed to secure funds on-chain")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.PENDING)
                                .message("Recipient claimed")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.PENDING)
                                .message("INR deposited to recipient's bank")
                                .build()
                ))
                .failed(true)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildTimelineForClaimFailed() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.CLAIM_FAILED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build(),
                eventBuilder().status(RemittanceStatus.ESCROWED).createdAt(T2).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Funds secured on-chain")
                                .completedAt(T2)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.FAILED)
                                .message("Claim failed")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.PENDING)
                                .message("INR deposited to recipient's bank")
                                .build()
                ))
                .failed(true)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildTimelineForDisbursementFailed() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.DISBURSEMENT_FAILED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build(),
                eventBuilder().status(RemittanceStatus.ESCROWED).createdAt(T2).build(),
                eventBuilder().status(RemittanceStatus.CLAIMED).createdAt(T3).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Funds secured on-chain")
                                .completedAt(T2)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Recipient claimed")
                                .completedAt(T3)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.FAILED)
                                .message("Failed to deposit INR")
                                .build()
                ))
                .failed(true)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildTimelineForRefundedWithNoFailedStep() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.REFUNDED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build(),
                eventBuilder().status(RemittanceStatus.ESCROWED).createdAt(T2).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Funds secured on-chain")
                                .completedAt(T2)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.PENDING)
                                .message("Recipient claimed")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.PENDING)
                                .message("INR deposited to recipient's bank")
                                .build()
                ))
                .failed(true)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildTimelineForCancelledFromInitiated() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.CANCELLED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.PENDING)
                                .message("Funds secured on-chain")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.PENDING)
                                .message("Recipient claimed")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.PENDING)
                                .message("INR deposited to recipient's bank")
                                .build()
                ))
                .failed(true)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldBuildTimelineForRefundFailedWithEscrowedStayingCompleted() {
        // given
        var remittance = remittanceBuilder().status(RemittanceStatus.REFUND_FAILED).build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build(),
                eventBuilder().status(RemittanceStatus.ESCROWED).createdAt(T2).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Funds secured on-chain")
                                .completedAt(T2)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.PENDING)
                                .message("Recipient claimed")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.PENDING)
                                .message("INR deposited to recipient's bank")
                                .build()
                ))
                .failed(true)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldShowSmsMessageForClaimedCurrentWhenSmsSucceeded() {
        // given
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.ESCROWED)
                .smsNotificationFailed(false)
                .build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build(),
                eventBuilder().status(RemittanceStatus.ESCROWED).createdAt(T2).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Funds secured on-chain")
                                .completedAt(T2)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.CURRENT)
                                .message("SMS sent, waiting for recipient")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.PENDING)
                                .message("INR deposited to recipient's bank")
                                .build()
                ))
                .failed(false)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldShowFallbackMessageForClaimedCurrentWhenSmsFailed() {
        // given
        var remittance = remittanceBuilder()
                .status(RemittanceStatus.ESCROWED)
                .smsNotificationFailed(true)
                .build();
        given(getRemittanceQueryHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID))
                .willReturn(remittance);

        var events = List.of(
                eventBuilder().status(RemittanceStatus.INITIATED).createdAt(T1).build(),
                eventBuilder().status(RemittanceStatus.ESCROWED).createdAt(T2).build()
        );
        given(remittanceStatusEventRepository.findByRemittanceId(SOME_REMITTANCE_ID))
                .willReturn(events);

        // when
        var result = getRemittanceTimelineHandler.handle(SOME_REMITTANCE_ID, SOME_SENDER_ID);

        // then
        var expected = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(T1)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Funds secured on-chain")
                                .completedAt(T2)
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.CLAIMED)
                                .status(TimelineStepStatus.CURRENT)
                                .message("Claim link available, waiting for recipient")
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.DELIVERED)
                                .status(TimelineStepStatus.PENDING)
                                .message("INR deposited to recipient's bank")
                                .build()
                ))
                .failed(false)
                .build();

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowWhenRemittanceNotFound() {
        // given
        var unknownId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        given(getRemittanceQueryHandler.handle(unknownId, SOME_SENDER_ID))
                .willThrow(RemittanceNotFoundException.byId(unknownId));

        // when / then
        assertThatThrownBy(() -> getRemittanceTimelineHandler.handle(unknownId, SOME_SENDER_ID))
                .isInstanceOf(RemittanceNotFoundException.class)
                .hasMessageContaining("SP-0010");
    }
}
