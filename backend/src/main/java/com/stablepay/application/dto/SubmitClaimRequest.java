package com.stablepay.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Builder;

@Builder(toBuilder = true)
public record SubmitClaimRequest(
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = ".*@.*", message = "must be a valid UPI ID (e.g., name@bank)")
    String upiId
) {}
