package com.stablepay.infrastructure.stripe;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stablepay.domain.funding.exception.InvalidWebhookSignatureException;
import com.stablepay.domain.funding.model.PaymentWebhookEvent;
import com.stablepay.domain.funding.model.WebhookEventType;
import com.stablepay.domain.funding.port.PaymentWebhookVerifier;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripePaymentWebhookVerifier implements PaymentWebhookVerifier {

    static final String EVENT_PAYMENT_SUCCEEDED = "payment_intent.succeeded";
    static final String EVENT_PAYMENT_FAILED = "payment_intent.payment_failed";
    private static final long REPLAY_TOLERANCE_SECONDS = 300L;

    private final StripeProperties stripeProperties;

    @Override
    public PaymentWebhookEvent verify(String payload, String signature)
            throws InvalidWebhookSignatureException {
        Event event;
        try {
            event = Webhook.constructEvent(
                    payload, signature, stripeProperties.webhookSecret(), REPLAY_TOLERANCE_SECONDS);
        } catch (SignatureVerificationException e) {
            throw InvalidWebhookSignatureException.withReason(e.getMessage(), e);
        }

        var type = mapType(event.getType());
        if (type == WebhookEventType.UNKNOWN) {
            return PaymentWebhookEvent.builder()
                    .eventId(event.getId())
                    .type(WebhookEventType.UNKNOWN)
                    .fundingId(null)
                    .build();
        }

        var fundingId = extractFundingId(event);
        if (fundingId == null) {
            return PaymentWebhookEvent.builder()
                    .eventId(event.getId())
                    .type(WebhookEventType.UNKNOWN)
                    .fundingId(null)
                    .build();
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

    private UUID extractFundingId(Event event) {
        var maybeObject = event.getDataObjectDeserializer().getObject();
        if (maybeObject.isEmpty()) {
            log.warn("Stripe event eventId={} type={} has no deserializable data object",
                    event.getId(), event.getType());
            return null;
        }
        var stripeObject = maybeObject.get();
        if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
            log.warn("Stripe event eventId={} type={} data is not a PaymentIntent (actual={})",
                    event.getId(), event.getType(), stripeObject.getClass().getName());
            return null;
        }
        var metadata = paymentIntent.getMetadata();
        var raw = metadata == null ? null : metadata.get("funding_id");
        if (raw == null) {
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
}
