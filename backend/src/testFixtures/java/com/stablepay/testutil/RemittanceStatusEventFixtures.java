package com.stablepay.testutil;

import java.time.Instant;

import com.stablepay.domain.remittance.model.RemittanceStatus;
import com.stablepay.domain.remittance.model.RemittanceStatusEvent;

public final class RemittanceStatusEventFixtures {

    private RemittanceStatusEventFixtures() {}

    public static final Long SOME_EVENT_ID = 1L;
    public static final Instant SOME_EVENT_CREATED_AT = Instant.parse("2026-04-03T10:00:00Z");

    public static RemittanceStatusEvent.RemittanceStatusEventBuilder eventBuilder() {
        return RemittanceStatusEvent.builder()
                .id(SOME_EVENT_ID)
                .remittanceId(RemittanceFixtures.SOME_REMITTANCE_ID)
                .status(RemittanceStatus.INITIATED)
                .message("Payment received")
                .createdAt(SOME_EVENT_CREATED_AT);
    }
}
