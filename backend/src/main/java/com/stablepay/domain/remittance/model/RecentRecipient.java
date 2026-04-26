package com.stablepay.domain.remittance.model;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

import lombok.Builder;

@Builder(toBuilder = true)
public record RecentRecipient(
    String name,
    String phone,
    Instant lastSentAt
) {
    public RecentRecipient {
        requireNonNull(name, "name cannot be null");
        requireNonNull(phone, "phone cannot be null");
        requireNonNull(lastSentAt, "lastSentAt cannot be null");
    }
}
