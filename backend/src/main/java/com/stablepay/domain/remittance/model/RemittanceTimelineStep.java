package com.stablepay.domain.remittance.model;

import java.time.Instant;

import lombok.Builder;

@Builder
public record RemittanceTimelineStep(
    RemittanceStatus step,
    TimelineStepStatus status,
    String message,
    Instant completedAt
) {}
