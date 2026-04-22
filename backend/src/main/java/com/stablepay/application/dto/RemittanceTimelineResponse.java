package com.stablepay.application.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record RemittanceTimelineResponse(
    List<TimelineStep> steps,
    boolean failed
) {}
