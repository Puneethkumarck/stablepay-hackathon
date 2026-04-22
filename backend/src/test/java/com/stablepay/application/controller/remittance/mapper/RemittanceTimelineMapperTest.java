package com.stablepay.application.controller.remittance.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.stablepay.application.dto.RemittanceTimelineResponse;
import com.stablepay.application.dto.TimelineStep;
import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.RemittanceTimeline;
import com.stablepay.domain.remittance.model.RemittanceTimelineStep;
import com.stablepay.domain.remittance.model.TimelineStepStatus;

class RemittanceTimelineMapperTest {

    private final RemittanceTimelineMapper mapper = new RemittanceTimelineMapperImpl();

    @Test
    void shouldMapTimelineToResponse() {
        // given
        var timeline = RemittanceTimeline.builder()
                .steps(List.of(
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(Instant.parse("2026-04-03T10:00:00Z"))
                                .build(),
                        RemittanceTimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.CURRENT)
                                .message("Securing funds on-chain...")
                                .build()
                ))
                .failed(false)
                .build();

        // when
        var response = mapper.toResponse(timeline);

        // then
        var expected = RemittanceTimelineResponse.builder()
                .steps(List.of(
                        TimelineStep.builder()
                                .step(RemittanceStatus.INITIATED)
                                .status(TimelineStepStatus.COMPLETED)
                                .message("Payment received")
                                .completedAt(Instant.parse("2026-04-03T10:00:00Z"))
                                .build(),
                        TimelineStep.builder()
                                .step(RemittanceStatus.ESCROWED)
                                .status(TimelineStepStatus.CURRENT)
                                .message("Securing funds on-chain...")
                                .build()
                ))
                .failed(false)
                .build();

        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }
}
