package com.stablepay.infrastructure.razorpay;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder(toBuilder = true)
record RazorpayContactRequest(
        String name,
        String type,
        @JsonProperty("reference_id") String referenceId) {

    static RazorpayContactRequest forRemittance(String remittanceId) {
        return RazorpayContactRequest.builder()
                .name("StablePay Recipient")
                .type("customer")
                .referenceId(remittanceId)
                .build();
    }
}
