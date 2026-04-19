package com.stablepay.infrastructure.razorpay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;

@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
record RazorpayErrorResponse(RazorpayError error) {}
