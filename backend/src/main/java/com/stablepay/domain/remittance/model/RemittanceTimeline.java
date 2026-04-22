package com.stablepay.domain.remittance.model;

import java.util.List;

import lombok.Builder;

@Builder(toBuilder = true)
public record RemittanceTimeline(
    List<RemittanceTimelineStep> steps,
    boolean failed
) {}
