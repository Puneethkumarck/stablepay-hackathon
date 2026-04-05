package com.stablepay.infrastructure.temporal;

import lombok.Builder;

@Builder(toBuilder = true)
public record ClaimSignal(
    String claimToken,
    String upiId,
    String destinationAddress
) {}
