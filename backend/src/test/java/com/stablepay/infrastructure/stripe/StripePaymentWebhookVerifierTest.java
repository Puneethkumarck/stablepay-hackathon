package com.stablepay.infrastructure.stripe;

import static com.stablepay.testutil.FundingOrderFixtures.SOME_FUNDING_ID;
import static com.stablepay.testutil.FundingOrderFixtures.SOME_STRIPE_PAYMENT_INTENT_ID;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_API_KEY;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_CURRENCY;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_TEST_PAYMENT_METHOD;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_WEBHOOK_EVENT_ID;
import static com.stablepay.testutil.StripeFixtures.SOME_STRIPE_WEBHOOK_SECRET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.stablepay.domain.funding.exception.InvalidWebhookSignatureException;
import com.stablepay.domain.funding.model.PaymentWebhookEvent;
import com.stablepay.domain.funding.model.WebhookEventType;
import com.stripe.net.Webhook;

import lombok.SneakyThrows;

class StripePaymentWebhookVerifierTest {

    private static final String LEGACY_API_VERSION = "2020-08-27";

    private StripeProperties stripeProperties;
    private StripePaymentWebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        stripeProperties = StripeProperties.builder()
                .apiKey(SOME_STRIPE_API_KEY)
                .webhookSecret(SOME_STRIPE_WEBHOOK_SECRET)
                .testMode(true)
                .autoConfirm(true)
                .testPaymentMethod(SOME_STRIPE_TEST_PAYMENT_METHOD)
                .currency(SOME_STRIPE_CURRENCY)
                .build();
        verifier = new StripePaymentWebhookVerifier(stripeProperties);
    }

    @Test
    void shouldParsePaymentSucceededEvent() {
        // given
        var payload = buildEventPayload(
                "payment_intent.succeeded", com.stripe.Stripe.API_VERSION, SOME_FUNDING_ID.toString());
        var signature = signPayload(payload);

        // when
        var result = verifier.verify(payload, signature);

        // then
        var expected = PaymentWebhookEvent.builder()
                .eventId(SOME_STRIPE_WEBHOOK_EVENT_ID)
                .type(WebhookEventType.PAYMENT_SUCCEEDED)
                .fundingId(SOME_FUNDING_ID)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldParsePaymentFailedEvent() {
        // given
        var payload = buildEventPayload(
                "payment_intent.payment_failed",
                com.stripe.Stripe.API_VERSION,
                SOME_FUNDING_ID.toString());
        var signature = signPayload(payload);

        // when
        var result = verifier.verify(payload, signature);

        // then
        var expected = PaymentWebhookEvent.builder()
                .eventId(SOME_STRIPE_WEBHOOK_EVENT_ID)
                .type(WebhookEventType.PAYMENT_FAILED)
                .fundingId(SOME_FUNDING_ID)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldExtractFundingIdWhenApiVersionDiffersFromSdk() {
        // given
        var payload = buildEventPayload(
                "payment_intent.succeeded", LEGACY_API_VERSION, SOME_FUNDING_ID.toString());
        var signature = signPayload(payload);

        // when
        var result = verifier.verify(payload, signature);

        // then
        var expected = PaymentWebhookEvent.builder()
                .eventId(SOME_STRIPE_WEBHOOK_EVENT_ID)
                .type(WebhookEventType.PAYMENT_SUCCEEDED)
                .fundingId(SOME_FUNDING_ID)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnknownEventType() {
        // given
        var payload = buildOtherEventPayload("customer.created");
        var signature = signPayload(payload);

        // when
        var result = verifier.verify(payload, signature);

        // then
        var expected = PaymentWebhookEvent.builder()
                .eventId(SOME_STRIPE_WEBHOOK_EVENT_ID)
                .type(WebhookEventType.UNKNOWN)
                .fundingId(null)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowOnInvalidSignature() {
        // given
        var payload = buildEventPayload(
                "payment_intent.succeeded", com.stripe.Stripe.API_VERSION, SOME_FUNDING_ID.toString());
        var bogusSignature = "t=1700000000,v1=deadbeef";

        // when
        var thrown = assertThatThrownBy(() -> verifier.verify(payload, bogusSignature));

        // then
        thrown
                .isInstanceOf(InvalidWebhookSignatureException.class)
                .hasMessageContaining("SP-0026");
    }

    @Test
    void shouldReturnUnknownWhenFundingIdMalformed() {
        // given
        var payload = buildEventPayload(
                "payment_intent.succeeded", com.stripe.Stripe.API_VERSION, "not-a-uuid");
        var signature = signPayload(payload);

        // when
        var result = verifier.verify(payload, signature);

        // then
        var expected = PaymentWebhookEvent.builder()
                .eventId(SOME_STRIPE_WEBHOOK_EVENT_ID)
                .type(WebhookEventType.UNKNOWN)
                .fundingId(null)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturnUnknownWhenFundingIdMissingFromMetadata() {
        // given
        var payload = buildEventPayloadWithoutFundingId(
                "payment_intent.succeeded", com.stripe.Stripe.API_VERSION);
        var signature = signPayload(payload);

        // when
        var result = verifier.verify(payload, signature);

        // then
        var expected = PaymentWebhookEvent.builder()
                .eventId(SOME_STRIPE_WEBHOOK_EVENT_ID)
                .type(WebhookEventType.UNKNOWN)
                .fundingId(null)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @SneakyThrows
    private String signPayload(String payload) {
        var timestamp = Webhook.Util.getTimeNow();
        var signedPayload = timestamp + "." + payload;
        var v1 = Webhook.Util.computeHmacSha256(SOME_STRIPE_WEBHOOK_SECRET, signedPayload);
        return "t=" + timestamp + ",v1=" + v1;
    }

    private String buildEventPayload(String eventType, String apiVersion, String fundingIdMetadata) {
        return """
            {
              "id": "%s",
              "object": "event",
              "api_version": "%s",
              "type": "%s",
              "data": {
                "object": {
                  "id": "%s",
                  "object": "payment_intent",
                  "amount": 2500,
                  "currency": "usd",
                  "status": "succeeded",
                  "metadata": {
                    "funding_id": "%s"
                  }
                }
              }
            }
            """.formatted(
                    SOME_STRIPE_WEBHOOK_EVENT_ID,
                    apiVersion,
                    eventType,
                    SOME_STRIPE_PAYMENT_INTENT_ID,
                    fundingIdMetadata);
    }

    private String buildEventPayloadWithoutFundingId(String eventType, String apiVersion) {
        return """
            {
              "id": "%s",
              "object": "event",
              "api_version": "%s",
              "type": "%s",
              "data": {
                "object": {
                  "id": "%s",
                  "object": "payment_intent",
                  "amount": 2500,
                  "currency": "usd",
                  "status": "succeeded",
                  "metadata": {}
                }
              }
            }
            """.formatted(
                    SOME_STRIPE_WEBHOOK_EVENT_ID,
                    apiVersion,
                    eventType,
                    SOME_STRIPE_PAYMENT_INTENT_ID);
    }

    private String buildOtherEventPayload(String eventType) {
        return """
            {
              "id": "%s",
              "object": "event",
              "api_version": "%s",
              "type": "%s",
              "data": {
                "object": {
                  "id": "cus_123",
                  "object": "customer"
                }
              }
            }
            """.formatted(SOME_STRIPE_WEBHOOK_EVENT_ID, com.stripe.Stripe.API_VERSION, eventType);
    }
}
