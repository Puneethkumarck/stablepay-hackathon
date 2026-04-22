package com.stablepay.domain.remittance.model;

import java.util.List;

import lombok.Builder;

@Builder
public record RemittanceTimeline(
    List<RemittanceTimelineStep> steps,
    boolean failed
) {}
