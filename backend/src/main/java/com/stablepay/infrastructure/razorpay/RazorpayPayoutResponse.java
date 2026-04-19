package com.stablepay.infrastructure.razorpay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record RazorpayPayoutResponse(String id, String status) {}
