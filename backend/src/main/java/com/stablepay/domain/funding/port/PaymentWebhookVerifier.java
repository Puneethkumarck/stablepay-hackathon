package com.stablepay.domain.funding.port;

import com.stablepay.domain.funding.exception.InvalidWebhookSignatureException;
import com.stablepay.domain.funding.model.PaymentWebhookEvent;

public interface PaymentWebhookVerifier {

    PaymentWebhookEvent verify(String payload, String signature) throws InvalidWebhookSignatureException;
}
