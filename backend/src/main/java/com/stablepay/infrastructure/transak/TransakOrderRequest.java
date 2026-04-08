package com.stablepay.infrastructure.transak;

import lombok.Builder;

@Builder(toBuilder = true)
record TransakOrderRequest(String quoteId, String paymentDetails, String partnerOrderId) {}
