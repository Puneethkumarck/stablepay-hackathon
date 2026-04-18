package com.stablepay.testutil;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_WEBHOOK_EVENT_ID;

import com.stablepay.domain.funding.model.PaymentWebhookEvent;
import com.stablepay.domain.funding.model.WebhookEventType;

public final class WebhookEventFixtures {

    private WebhookEventFixtures() {}

    public static final PaymentWebhookEvent SOME_PAYMENT_SUCCEEDED_EVENT =
            paymentWebhookEventBuilder()
                    .type(WebhookEventType.PAYMENT_SUCCEEDED)
                    .fundingId(SOME_FUNDING_ID)
                    .build();

    public static final PaymentWebhookEvent SOME_PAYMENT_FAILED_EVENT =
            paymentWebhookEventBuilder()
                    .type(WebhookEventType.PAYMENT_FAILED)
                    .fundingId(SOME_FUNDING_ID)
                    .build();

    public static final PaymentWebhookEvent SOME_UNKNOWN_EVENT =
            paymentWebhookEventBuilder()
                    .type(WebhookEventType.UNKNOWN)
                    .fundingId(null)
                    .build();

    public static PaymentWebhookEvent.PaymentWebhookEventBuilder paymentWebhookEventBuilder() {
        return PaymentWebhookEvent.builder()
                .eventId(SOME_STRIPE_WEBHOOK_EVENT_ID)
                .type(WebhookEventType.PAYMENT_SUCCEEDED)
                .fundingId(SOME_FUNDING_ID);
    }
}
