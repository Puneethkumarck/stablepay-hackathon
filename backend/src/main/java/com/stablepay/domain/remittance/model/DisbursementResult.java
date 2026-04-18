package com.stablepay.domain.remittance.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record DisbursementResult(String providerId, String providerStatus) {}
