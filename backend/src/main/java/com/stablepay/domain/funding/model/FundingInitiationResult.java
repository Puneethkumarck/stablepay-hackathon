package com.stablepay.domain.funding.model;

import static java.util.Objects.requireNonNull;

import lombok.Builder;

@Builder(toBuilder = true)
public record FundingInitiationResult(
    FundingOrder order,
    String clientSecret
) {
    public FundingInitiationResult {
        requireNonNull(order, "order cannot be null");
    }
}
