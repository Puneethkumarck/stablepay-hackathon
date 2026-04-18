package com.stablepay.application.controller.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stablepay.domain.funding.exception.InvalidWebhookSignatureException;
import com.stablepay.domain.funding.handler.CompleteFundingHandler;
import com.stablepay.domain.funding.handler.FailFundingHandler;
import com.stablepay.domain.funding.port.PaymentWebhookVerifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Spring Security is NOT wired in the project. When it is, this endpoint must be
// permit-all and CSRF-exempt (/webhooks/**): the Stripe signature header plus the
// 300s replay tolerance is the authentication mechanism for this endpoint.
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Inbound webhook endpoints (PSP-signed)")
public class StripeWebhookController {

    private final PaymentWebhookVerifier paymentWebhookVerifier;
    private final CompleteFundingHandler completeFundingHandler;
    private final FailFundingHandler failFundingHandler;

    @PostMapping("/stripe")
    @Operation(
            summary = "Receive Stripe webhook",
            description = "Verifies the Stripe-Signature header, then dispatches "
                    + "payment_intent.succeeded and payment_intent.payment_failed events "
                    + "to the appropriate funding handler. Always returns 200 once the "
                    + "signature is valid, to prevent Stripe retries on downstream errors.")
    public ResponseEntity<Void> receive(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        try {
            var event = paymentWebhookVerifier.verify(payload, signature);
            dispatch(event);
        } catch (InvalidWebhookSignatureException e) {
            log.warn("Rejecting Stripe webhook with invalid signature: {}", e.getMessage());
            throw e;
        }
        return ResponseEntity.ok().build();
    }

    private void dispatch(com.stablepay.domain.funding.model.PaymentWebhookEvent event) {
        try {
            switch (event.type()) {
                case PAYMENT_SUCCEEDED -> completeFundingHandler.handle(event.fundingId());
                case PAYMENT_FAILED -> failFundingHandler.handle(event.fundingId());
                case UNKNOWN -> log.debug(
                        "Ignoring unsupported Stripe event eventId={}", event.eventId());
            }
        } catch (Exception e) {
            // Always ACK 200 once signature is valid; Stripe would otherwise retry.
            log.error("Error dispatching Stripe webhook eventId={} type={}: {}",
                    event.eventId(), event.type(), e.getMessage(), e);
        }
    }
}
