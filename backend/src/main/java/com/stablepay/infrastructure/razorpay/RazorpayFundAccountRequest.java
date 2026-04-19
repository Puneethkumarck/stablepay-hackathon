package com.stablepay.infrastructure.razorpay;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder(toBuilder = true)
record RazorpayFundAccountRequest(
        @JsonProperty("contact_id") String contactId,
        @JsonProperty("account_type") String accountType,
        RazorpayVpa vpa) {

    static RazorpayFundAccountRequest forVpa(String contactId, String upiId) {
        return RazorpayFundAccountRequest.builder()
                .contactId(contactId)
                .accountType("vpa")
                .vpa(RazorpayVpa.builder().address(upiId).build())
                .build();
    }
}
