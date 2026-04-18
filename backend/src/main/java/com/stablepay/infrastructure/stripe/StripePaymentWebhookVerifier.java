package com.stablepay.infrastructure.stripe;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablepay.domain.funding.exception.InvalidWebhookSignatureException;
import com.stablepay.domain.funding.model.PaymentWebhookEvent;
import com.stablepay.domain.funding.model.WebhookEventType;
import com.stablepay.domain.funding.port.PaymentWebhookVerifier;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripePaymentWebhookVerifier implements PaymentWebhookVerifier {

    private static final String EVENT_PAYMENT_SUCCEEDED = "payment_intent.succeeded";
    private static final String EVENT_PAYMENT_FAILED = "payment_intent.payment_failed";
    private static final long REPLAY_TOLERANCE_SECONDS = 300L;
    private static final String FUNDING_ID_METADATA_KEY = "funding_id";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StripeProperties stripeProperties;

    @Override
    public PaymentWebhookEvent verify(String payload, String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(
                    payload, signature, stripeProperties.webhookSecret(), REPLAY_TOLERANCE_SECONDS);
        } catch (SignatureVerificationException e) {
            throw InvalidWebhookSignatureException.withReason(e.getMessage(), e);
        }

        var type = mapType(event.getType());
        if (type == WebhookEventType.UNKNOWN) {
            return unknownEvent(event.getId());
        }

        var fundingId = extractFundingIdFromPayload(payload, event);
        if (fundingId == null) {
            return unknownEvent(event.getId());
        }
        return PaymentWebhookEvent.builder()
                .eventId(event.getId())
                .type(type)
                .fundingId(fundingId)
                .build();
    }

    private WebhookEventType mapType(String stripeType) {
        if (stripeType == null) {
            return WebhookEventType.UNKNOWN;
        }
        return switch (stripeType) {
            case EVENT_PAYMENT_SUCCEEDED -> WebhookEventType.PAYMENT_SUCCEEDED;
            case EVENT_PAYMENT_FAILED -> WebhookEventType.PAYMENT_FAILED;
            default -> WebhookEventType.UNKNOWN;
        };
    }

    // Parses funding_id from the raw JSON payload instead of the Stripe SDK's typed
    // PaymentIntent. event.getDataObjectDeserializer().getObject() returns Optional.empty()
    // when the webhook's api_version differs from the SDK's pinned version — which would
    // silently drop legitimate events. The metadata JSON path is stable across Stripe API
    // versions, so parsing it directly is safe.
    private UUID extractFundingIdFromPayload(String payload, Event event) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(payload);
        } catch (JsonProcessingException e) {
            log.warn("Unable to parse Stripe webhook payload eventId={}: {}",
                    event.getId(), e.getMessage());
            return null;
        }
        var raw = root.path("data").path("object").path("metadata")
                .path(FUNDING_ID_METADATA_KEY).asText(null);
        if (raw == null || raw.isBlank()) {
            log.warn("Stripe event eventId={} type={} missing funding_id metadata",
                    event.getId(), event.getType());
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            log.warn("Stripe event eventId={} has malformed funding_id metadata: {}",
                    event.getId(), raw);
            return null;
        }
    }

    private PaymentWebhookEvent unknownEvent(String eventId) {
        return PaymentWebhookEvent.builder()
                .eventId(eventId)
                .type(WebhookEventType.UNKNOWN)
                .fundingId(null)
                .build();
    }
}
