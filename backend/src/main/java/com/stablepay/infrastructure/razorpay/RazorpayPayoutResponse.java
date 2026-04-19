package com.stablepay.infrastructure.razorpay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;

@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
record RazorpayPayoutResponse(String id, String status) {}
