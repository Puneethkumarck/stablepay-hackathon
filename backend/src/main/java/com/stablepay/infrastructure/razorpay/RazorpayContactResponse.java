package com.stablepay.infrastructure.razorpay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record RazorpayContactResponse(String id) {}
