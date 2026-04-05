package com.stablepay.domain.claim.model;

import static java.util.Objects.requireNonNull;

import com.stablepay.domain.remittance.model.Remittance;

import lombok.Builder;

@Builder(toBuilder = true)
public record ClaimDetails(
    ClaimToken claimToken,
    Remittance remittance
) {
    public ClaimDetails {
        requireNonNull(claimToken, "claimToken cannot be null");
        requireNonNull(remittance, "remittance cannot be null");
    }
}
