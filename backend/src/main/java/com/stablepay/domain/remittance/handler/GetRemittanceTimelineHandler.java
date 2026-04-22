package com.stablepay.domain.remittance.handler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.RemittanceStatusEvent;
import com.stablepay.domain.remittance.model.RemittanceTimeline;
import com.stablepay.domain.remittance.model.RemittanceTimelineStep;
import com.stablepay.domain.remittance.model.TimelineStepStatus;
import com.stablepay.domain.remittance.port.RemittanceStatusEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRemittanceTimelineHandler {

    private static final List<RemittanceStatus> HAPPY_PATH_STEPS = List.of(
            RemittanceStatus.INITIATED,
            RemittanceStatus.ESCROWED,
            RemittanceStatus.CLAIMED,
            RemittanceStatus.DELIVERED
    );

    private static final Set<RemittanceStatus> FAILURE_STATES = Set.of(
            RemittanceStatus.DEPOSIT_FAILED,
            RemittanceStatus.CLAIM_FAILED,
            RemittanceStatus.DISBURSEMENT_FAILED
    );

    private static final Set<RemittanceStatus> TERMINAL_NON_DELIVERY = Set.of(
            RemittanceStatus.REFUNDED,
            RemittanceStatus.CANCELLED,
            RemittanceStatus.REFUND_FAILED
    );

    private static final Map<RemittanceStatus, RemittanceStatus> FAILURE_TO_STEP = Map.of(
            RemittanceStatus.DEPOSIT_FAILED, RemittanceStatus.ESCROWED,
            RemittanceStatus.CLAIM_FAILED, RemittanceStatus.CLAIMED,
            RemittanceStatus.DISBURSEMENT_FAILED, RemittanceStatus.DELIVERED
    );

    private static final Map<RemittanceStatus, String> COMPLETED_MESSAGES = Map.of(
            RemittanceStatus.INITIATED, "Payment received",
            RemittanceStatus.ESCROWED, "Funds secured on-chain",
            RemittanceStatus.CLAIMED, "Recipient claimed",
            RemittanceStatus.DELIVERED, "INR deposited to recipient's bank"
    );

    private static final Map<RemittanceStatus, String> CURRENT_MESSAGES = Map.of(
            RemittanceStatus.INITIATED, "Processing payment...",
            RemittanceStatus.ESCROWED, "Securing funds on-chain...",
            RemittanceStatus.DELIVERED, "Depositing INR to recipient's bank..."
    );

    private static final Map<RemittanceStatus, String> PENDING_MESSAGES = Map.of(
            RemittanceStatus.INITIATED, "Payment received",
            RemittanceStatus.ESCROWED, "Funds secured on-chain",
            RemittanceStatus.CLAIMED, "Recipient claimed",
            RemittanceStatus.DELIVERED, "INR deposited to recipient's bank"
    );

    private final GetRemittanceQueryHandler getRemittanceQueryHandler;
    private final RemittanceStatusEventRepository remittanceStatusEventRepository;

    public RemittanceTimeline handle(UUID remittanceId, UUID principalId) {
        var remittance = getRemittanceQueryHandler.handle(remittanceId, principalId);

        var events = remittanceStatusEventRepository.findByRemittanceId(remittanceId);

        var eventMap = events.stream()
                .filter(e -> HAPPY_PATH_STEPS.contains(e.status()))
                .collect(Collectors.toMap(
                        RemittanceStatusEvent::status,
                        e -> e,
                        (first, second) -> first
                ));

        var status = remittance.status();
        var isFailed = FAILURE_STATES.contains(status) || TERMINAL_NON_DELIVERY.contains(status);
        var failedStep = FAILURE_TO_STEP.get(status);

        var foundCurrent = new boolean[]{false};

        var steps = HAPPY_PATH_STEPS.stream()
                .map(step -> {
                    var event = eventMap.get(step);

                    if (event != null) {
                        return RemittanceTimelineStep.builder()
                                .step(step)
                                .status(TimelineStepStatus.COMPLETED)
                                .message(COMPLETED_MESSAGES.get(step))
                                .completedAt(event.createdAt())
                                .build();
                    }

                    if (!isFailed && !foundCurrent[0]) {
                        foundCurrent[0] = true;
                        var message = step == RemittanceStatus.CLAIMED
                                ? claimedCurrentMessage(remittance.smsNotificationFailed())
                                : CURRENT_MESSAGES.get(step);
                        return RemittanceTimelineStep.builder()
                                .step(step)
                                .status(TimelineStepStatus.CURRENT)
                                .message(message)
                                .build();
                    }

                    if (step.equals(failedStep)) {
                        return RemittanceTimelineStep.builder()
                                .step(step)
                                .status(TimelineStepStatus.FAILED)
                                .message(PENDING_MESSAGES.get(step))
                                .build();
                    }

                    return RemittanceTimelineStep.builder()
                            .step(step)
                            .status(TimelineStepStatus.PENDING)
                            .message(PENDING_MESSAGES.get(step))
                            .build();
                })
                .toList();

        return RemittanceTimeline.builder()
                .steps(steps)
                .failed(isFailed)
                .build();
    }

    private String claimedCurrentMessage(boolean smsNotificationFailed) {
        return smsNotificationFailed
                ? "Claim link available, waiting for recipient"
                : "SMS sent, waiting for recipient";
    }
}
