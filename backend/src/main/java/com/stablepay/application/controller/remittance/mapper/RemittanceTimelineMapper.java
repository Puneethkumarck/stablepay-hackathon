package com.stablepay.application.controller.remittance.mapper;

import org.mapstruct.Mapper;

import com.stablepay.application.dto.RemittanceTimelineResponse;
import com.stablepay.application.dto.TimelineStep;
import com.stablepay.domain.remittance.model.RemittanceTimeline;
import com.stablepay.domain.remittance.model.RemittanceTimelineStep;

@Mapper
public interface RemittanceTimelineMapper {
    RemittanceTimelineResponse toResponse(RemittanceTimeline timeline);
    TimelineStep toStep(RemittanceTimelineStep step);
}
