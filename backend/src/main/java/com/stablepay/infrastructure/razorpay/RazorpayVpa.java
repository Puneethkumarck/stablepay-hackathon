package com.stablepay.infrastructure.razorpay;

import lombok.Builder;

@Builder(toBuilder = true)
record RazorpayVpa(String address) {}
