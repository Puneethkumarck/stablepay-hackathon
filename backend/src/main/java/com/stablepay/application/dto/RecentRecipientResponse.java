package com.stablepay.application.dto;

import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record RecentRecipientResponse(
    String name,
    String phone,
    Instant lastSentAt
) {}
