package com.stablepay.domain.remittance.model;

import static java.util.Objects.requireNonNull;

import lombok.Builder;

@Builder(toBuilder = true)
public record DisbursementResult(String providerId, String providerStatus) {
    public DisbursementResult {
        requireNonNull(providerId, "providerId cannot be null");
        requireNonNull(providerStatus, "providerStatus cannot be null");
    }
}
