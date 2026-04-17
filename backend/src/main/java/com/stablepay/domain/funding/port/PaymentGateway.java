package com.stablepay.domain.funding.port;

import java.math.BigDecimal;

import com.stablepay.domain.funding.model.PaymentRequest;
import com.stablepay.domain.funding.model.PaymentResult;

public interface PaymentGateway {
    PaymentResult initiatePayment(PaymentRequest request);
    void refund(String paymentIntentId, BigDecimal amount);
}
