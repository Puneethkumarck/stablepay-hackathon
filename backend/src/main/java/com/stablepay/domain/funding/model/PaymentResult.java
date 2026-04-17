package com.stablepay.domain.funding.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record PaymentResult(
    String pspReference,
    String clientSecret,
    String status
) {}
