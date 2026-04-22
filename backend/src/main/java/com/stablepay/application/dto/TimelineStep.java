package com.stablepay.application.dto;

import java.time.Instant;

import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.TimelineStepStatus;

import lombok.Builder;

@Builder
public record TimelineStep(
    RemittanceStatus step,
    TimelineStepStatus status,
    String message,
    Instant completedAt
) {}
