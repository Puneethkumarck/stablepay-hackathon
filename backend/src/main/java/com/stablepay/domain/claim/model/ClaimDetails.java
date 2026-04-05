package com.stablepay.domain.claim.model;

import com.stablepay.domain.remittance.model.Remittance;

import lombok.Builder;

@Builder(toBuilder = true)
public record ClaimDetails(
    ClaimToken claimToken,
    Remittance remittance
) {}
