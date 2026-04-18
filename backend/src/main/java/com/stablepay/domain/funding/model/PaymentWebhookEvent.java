package com.stablepay.domain.funding.model;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import lombok.Builder;

@Builder(toBuilder = true)
public record PaymentWebhookEvent(
    String eventId,
    WebhookEventType type,
    UUID fundingId
) {
    public PaymentWebhookEvent {
        requireNonNull(eventId, "eventId cannot be null");
        requireNonNull(type, "type cannot be null");
    }
}
